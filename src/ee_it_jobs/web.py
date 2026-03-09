from __future__ import annotations

import asyncio
import json
from pathlib import Path

from fastapi import FastAPI, Request, Query, UploadFile, File
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel as PydanticBaseModel

from ee_it_jobs.models import UserProfile

app = FastAPI(title="Eesti IT Tööpakkumised")

_scrape_lock = asyncio.Lock()
_scrape_status: dict = {"running": False, "last_error": None}

STATIC_DIR = Path(__file__).parent / "static"
TEMPLATE_PATH = STATIC_DIR / "index.html"
OUTPUT_DIR = Path("output")
PROFILE_PATH = OUTPUT_DIR / "profile.json"
CV_PATH = OUTPUT_DIR / "cv.pdf"
APPLICATIONS_PATH = OUTPUT_DIR / "applications.json"

# Lazy-init auto applier
_applier = None


def _get_applier():
    global _applier
    if _applier is None:
        from ee_it_jobs.applier import AutoApplier
        _applier = AutoApplier(output_dir=OUTPUT_DIR)
    return _applier


@app.on_event("shutdown")
async def _shutdown_applier():
    if _applier is not None:
        await _applier.close()


# ── Existing routes ──────────────────────────────────────────────────────────


@app.get("/", response_class=HTMLResponse)
async def index():
    return TEMPLATE_PATH.read_text(encoding="utf-8")


@app.get("/api/jobs")
async def get_jobs(
    search: str = Query("", description="Search query"),
    company: str = Query("", description="Filter by company"),
    source: str = Query("", description="Filter by source"),
    workplace: str = Query("", description="Filter by workplace type"),
):
    """Return all scraped jobs, optionally filtered."""
    data = _load_latest_jobs()
    if not data:
        return {"jobs": [], "total": 0, "sources": {}, "scraped_at": None}

    jobs = data.get("jobs", [])

    # Apply filters
    if search:
        q = search.lower()
        jobs = [
            j for j in jobs
            if q in j.get("title", "").lower()
            or q in j.get("company", "").lower()
            or q in j.get("location", "").lower()
            or q in (j.get("department") or "").lower()
        ]
    if company:
        jobs = [j for j in jobs if j.get("company", "").lower() == company.lower()]
    if source:
        jobs = [j for j in jobs if j.get("source", "").lower() == source.lower()]
    if workplace and workplace != "all":
        jobs = [j for j in jobs if j.get("workplace_type", "") == workplace]

    return {
        "jobs": jobs,
        "total": len(jobs),
        "sources": data.get("sources", {}),
        "scraped_at": data.get("scraped_at"),
    }


@app.get("/api/filters")
async def get_filters():
    """Return unique values for filter dropdowns."""
    data = _load_latest_jobs()
    if not data:
        return {"companies": [], "sources": [], "workplace_types": []}

    jobs = data.get("jobs", [])
    companies = sorted({j.get("company", "") for j in jobs if j.get("company")})
    sources = sorted({j.get("source", "") for j in jobs if j.get("source")})
    workplace_types = sorted({j.get("workplace_type", "") for j in jobs if j.get("workplace_type")})

    return {
        "companies": companies,
        "sources": sources,
        "workplace_types": workplace_types,
    }


@app.post("/api/scrape")
async def trigger_scrape():
    """Trigger a new scrape run from the web UI."""
    if _scrape_status["running"]:
        return {"status": "already_running"}

    async def _do_scrape():
        _scrape_status["running"] = True
        _scrape_status["last_error"] = None
        try:
            from ee_it_jobs.config import load_config
            from ee_it_jobs.runner import run_all
            config = load_config()
            await run_all(config)
        except Exception as e:
            _scrape_status["last_error"] = str(e)
        finally:
            _scrape_status["running"] = False

    asyncio.create_task(_do_scrape())
    return {"status": "started"}


@app.get("/api/scrape/status")
async def scrape_status():
    """Check if a scrape is currently running."""
    return _scrape_status


@app.post("/api/match")
async def match_cv(file: UploadFile = File(...)):
    """Upload a CV (PDF) and match against scraped jobs."""
    from ee_it_jobs.matcher import parse_pdf, extract_profile, match_jobs

    if not file.filename or not file.filename.lower().endswith(".pdf"):
        return {"error": "Palun lae ules PDF fail"}

    content = await file.read()
    if len(content) > 10 * 1024 * 1024:  # 10MB limit
        return {"error": "Fail on liiga suur (max 10MB)"}

    try:
        text = parse_pdf(content)
    except Exception:
        return {"error": "PDF lugemine ebaonnestus"}

    if not text.strip():
        return {"error": "PDF-st ei leitud teksti"}

    profile = extract_profile(text)

    data = _load_latest_jobs()
    if not data or not data.get("jobs"):
        return {"error": "Toopakkumisi pole veel scrapitud"}

    matches = match_jobs(profile, data["jobs"], top_n=20)

    return {
        "skills_found": sorted(profile.skills),
        "years_experience": profile.years_experience,
        "role_level": profile.role_level,
        "matches": matches,
        "total_jobs": len(data["jobs"]),
    }


# ── Profile API ──────────────────────────────────────────────────────────────


@app.get("/api/profile")
async def get_profile():
    """Read saved user profile."""
    if PROFILE_PATH.exists():
        try:
            data = json.loads(PROFILE_PATH.read_text(encoding="utf-8"))
            data["has_cv"] = CV_PATH.exists()
            return data
        except (json.JSONDecodeError, OSError):
            pass
    return {"first_name": "", "last_name": "", "email": "", "phone": "",
            "linkedin_url": "", "cover_letter": "", "has_cv": False}


@app.post("/api/profile")
async def save_profile(profile: UserProfile):
    """Save user profile to JSON."""
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    PROFILE_PATH.write_text(
        profile.model_dump_json(indent=2), encoding="utf-8"
    )
    return {"ok": True}


@app.post("/api/profile/cv")
async def upload_cv(file: UploadFile = File(...)):
    """Upload CV PDF file."""
    if not file.filename or not file.filename.lower().endswith(".pdf"):
        return {"error": "Palun lae üles PDF fail"}

    content = await file.read()
    if len(content) > 10 * 1024 * 1024:
        return {"error": "Fail on liiga suur (max 10MB)"}

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    CV_PATH.write_bytes(content)
    return {"ok": True, "filename": file.filename}


# ── Apply API ────────────────────────────────────────────────────────────────


class ApplyStartRequest(PydanticBaseModel):
    job_url: str
    job_title: str = ""
    job_company: str = ""
    job_source: str = ""


@app.post("/api/apply/start")
async def start_apply(req: ApplyStartRequest):
    """Start a Playwright apply session: navigate, fill form, return screenshot."""
    # Load profile
    if not PROFILE_PATH.exists():
        return {"error": "Profiil pole salvestatud. Palun salvesta esmalt oma profiil."}

    try:
        profile = UserProfile.model_validate_json(
            PROFILE_PATH.read_text(encoding="utf-8")
        )
    except Exception:
        return {"error": "Profiili lugemine ebaõnnestus"}

    if not profile.email:
        return {"error": "Profiilis puudub e-mail"}

    cv_path = CV_PATH if CV_PATH.exists() else Path("/dev/null")

    job = {
        "url": req.job_url,
        "title": req.job_title,
        "company": req.job_company,
        "source": req.job_source,
    }

    applier = _get_applier()

    # Cleanup stale sessions first
    await applier.cleanup_stale()

    try:
        session = await applier.start_session(job, profile, cv_path)
        import base64
        return {
            "session_id": session.session_id,
            "screenshot": base64.b64encode(session.screenshot).decode()
            if session.screenshot else "",
        }
    except Exception as e:
        return {"error": f"Vormi täitmine ebaõnnestus: {e}"}


@app.post("/api/apply/{session_id}/confirm")
async def confirm_apply(session_id: str):
    """Submit the filled form."""
    applier = _get_applier()
    try:
        result = await applier.confirm(session_id)
        return result
    except ValueError as e:
        return {"error": str(e)}
    except Exception as e:
        return {"error": f"Saatmine ebaõnnestus: {e}"}


@app.post("/api/apply/{session_id}/cancel")
async def cancel_apply(session_id: str):
    """Cancel and close the apply session."""
    applier = _get_applier()
    await applier.cancel(session_id)
    return {"ok": True}


@app.get("/api/applications")
async def get_applications():
    """Return application history."""
    if APPLICATIONS_PATH.exists():
        try:
            data = json.loads(APPLICATIONS_PATH.read_text(encoding="utf-8"))
            return {"applications": data}
        except (json.JSONDecodeError, OSError):
            pass
    return {"applications": []}


# ── Helpers ──────────────────────────────────────────────────────────────────


def _load_latest_jobs() -> dict | None:
    """Load the most recent jobs JSON file from the output directory."""
    if not OUTPUT_DIR.exists():
        return None

    json_files = sorted(OUTPUT_DIR.glob("jobs_*.json"), reverse=True)
    if not json_files:
        return None

    try:
        return json.loads(json_files[0].read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return None


# Mount static files after routes
app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")
