from __future__ import annotations

import csv
import json
import re
from datetime import datetime
from difflib import SequenceMatcher
from pathlib import Path

from ee_it_jobs.models import JobListing, ScrapeResult

_COMPANY_SUFFIXES = re.compile(
    r"\b(oü|as|ou|ltd|inc|gmbh|se|ag|corp|llc|oy|ab|sia)\b\.?",
    re.IGNORECASE,
)


def normalize_company(name: str) -> str:
    """Lowercase, strip business suffixes (OÜ, AS, Ltd, …) and extra whitespace."""
    name = _COMPANY_SUFFIXES.sub("", name.lower())
    return " ".join(name.split())


def companies_match(a: str, b: str) -> bool:
    """True if two company names refer to the same company.

    Matches on exact normalised form, or when one is a word-boundary
    prefix of the other (e.g. "bolt" vs "bolt technology").
    """
    na, nb = normalize_company(a), normalize_company(b)
    if na == nb:
        return True
    short, long = sorted([na, nb], key=len)
    return long.startswith(short) and (
        len(long) == len(short) or long[len(short)] == " "
    )


def titles_similar(a: str, b: str, threshold: float = 0.78) -> bool:
    """True if two job titles are fuzzy-similar (SequenceMatcher ratio >= threshold)."""
    return SequenceMatcher(None, a.lower(), b.lower()).ratio() >= threshold


def deduplicate(jobs: list[JobListing]) -> list[JobListing]:
    # Phase 1 — exact match (fast, set-based)
    seen: set[str] = set()
    phase1: list[JobListing] = []
    for job in jobs:
        key = job.dedup_key
        if key not in seen:
            seen.add(key)
            phase1.append(job)

    # Phase 2 — fuzzy match (company prefix + title similarity)
    unique: list[JobListing] = []
    for job in phase1:
        is_dup = False
        for kept in unique:
            if companies_match(job.company, kept.company) and titles_similar(
                job.title, kept.title
            ):
                is_dup = True
                break
        if not is_dup:
            unique.append(job)
    return unique


def export_json(
    jobs: list[JobListing],
    results: list[ScrapeResult],
    path: Path,
) -> None:
    sources = {}
    for r in results:
        sources[r.source] = {
            "count": len(r.jobs),
            "errors": len(r.errors),
            "duration_seconds": r.duration_seconds,
        }

    payload = {
        "scraped_at": datetime.now().isoformat(),
        "total_jobs": len(jobs),
        "sources": sources,
        "jobs": [j.model_dump(mode="json") for j in jobs],
    }
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")


def export_csv(jobs: list[JobListing], path: Path) -> None:
    fields = [
        "title", "company", "location", "url", "source",
        "date_posted", "date_scraped", "job_type", "workplace_type",
        "department", "salary_text",
    ]
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        for job in jobs:
            row = job.model_dump(mode="json")
            writer.writerow({k: row.get(k, "") for k in fields})
