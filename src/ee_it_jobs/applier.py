from __future__ import annotations

import asyncio
import base64
import json
import time
import uuid
from dataclasses import dataclass, field
from pathlib import Path

from loguru import logger
from playwright.async_api import Page, async_playwright, Playwright, Browser

from ee_it_jobs.form_fillers import get_form_filler
from ee_it_jobs.models import UserProfile

STALE_SESSION_SECONDS = 300  # 5 minutes


@dataclass
class ApplySession:
    session_id: str
    job: dict
    page: Page
    screenshot: bytes = b""
    created_at: float = field(default_factory=time.time)
    status: str = "filling"  # filling | filled | submitted | cancelled


class AutoApplier:
    """Manages Playwright-based apply sessions."""

    def __init__(self, output_dir: Path = Path("output")):
        self._sessions: dict[str, ApplySession] = {}
        self._pw: Playwright | None = None
        self._browser: Browser | None = None
        self._lock = asyncio.Lock()
        self._output_dir = output_dir
        self._applications_path = output_dir / "applications.json"

    async def _ensure_browser(self) -> Browser:
        if self._browser and self._browser.is_connected():
            return self._browser
        self._pw = await async_playwright().start()
        self._browser = await self._pw.chromium.launch(headless=True)
        return self._browser

    async def start_session(
        self, job: dict, profile: UserProfile, cv_path: Path
    ) -> ApplySession:
        """Open the apply page, fill the form, take a screenshot."""
        browser = await self._ensure_browser()
        context = await browser.new_context(
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/122.0.0.0 Safari/537.36"
            ),
            viewport={"width": 1280, "height": 900},
        )
        page = await context.new_page()

        session_id = uuid.uuid4().hex[:12]
        session = ApplySession(
            session_id=session_id,
            job=job,
            page=page,
        )
        self._sessions[session_id] = session

        try:
            apply_url = self._get_apply_url(job)
            logger.info(f"Navigating to apply page: {apply_url}")
            await page.goto(apply_url, wait_until="domcontentloaded", timeout=20000)

            # Wait a moment for JS to render
            await page.wait_for_timeout(2000)

            # Select and run the appropriate form filler
            filler = get_form_filler(job.get("source", ""), apply_url)
            filled = await filler.fill(page, profile, cv_path)

            if not filled:
                logger.warning("Form filler returned False, trying generic fallback")
                from ee_it_jobs.form_fillers import GenericFormFiller
                await GenericFormFiller().fill(page, profile, cv_path)

            # Wait for fills to settle
            await page.wait_for_timeout(1000)

            # Take screenshot
            session.screenshot = await page.screenshot(full_page=True)
            session.status = "filled"

            logger.info(f"Apply session {session_id} ready for confirmation")
            return session

        except Exception as e:
            logger.error(f"Error filling form: {e}")
            await self._close_session(session)
            raise

    async def confirm(self, session_id: str) -> dict:
        """Click submit and return the result."""
        session = self._sessions.get(session_id)
        if not session:
            raise ValueError(f"Session {session_id} not found")
        if session.status != "filled":
            raise ValueError(f"Session {session_id} is in status {session.status}")

        page = session.page
        try:
            # Look for submit button
            submit_selectors = [
                "button[type='submit']",
                "input[type='submit']",
                "button:has-text('Submit')",
                "button:has-text('Apply')",
                "button:has-text('Kandideeri')",
                "button:has-text('Saada')",
                "button:has-text('Send')",
            ]

            clicked = False
            for sel in submit_selectors:
                try:
                    btn = page.locator(sel).first
                    if await btn.is_visible(timeout=1000):
                        await btn.click()
                        clicked = True
                        break
                except Exception:
                    continue

            if not clicked:
                logger.warning("No submit button found")
                return {
                    "success": False,
                    "error": "Submit nuppu ei leitud",
                    "screenshot": self._screenshot_b64(session),
                }

            # Wait for navigation or response
            await page.wait_for_timeout(3000)

            # Take post-submit screenshot
            session.screenshot = await page.screenshot(full_page=True)
            session.status = "submitted"

            # Record application
            self._record_application(session)

            logger.info(f"Application submitted for session {session_id}")
            return {
                "success": True,
                "screenshot": self._screenshot_b64(session),
            }

        except Exception as e:
            logger.error(f"Error submitting form: {e}")
            return {
                "success": False,
                "error": str(e),
                "screenshot": self._screenshot_b64(session),
            }
        finally:
            await self._close_session(session)

    async def cancel(self, session_id: str) -> None:
        """Cancel and close a session."""
        session = self._sessions.get(session_id)
        if session:
            session.status = "cancelled"
            await self._close_session(session)
            logger.info(f"Session {session_id} cancelled")

    async def cleanup_stale(self) -> None:
        """Close sessions older than STALE_SESSION_SECONDS."""
        now = time.time()
        stale = [
            sid
            for sid, s in self._sessions.items()
            if now - s.created_at > STALE_SESSION_SECONDS
            and s.status not in ("submitted", "cancelled")
        ]
        for sid in stale:
            logger.info(f"Cleaning up stale session {sid}")
            await self.cancel(sid)

    async def close(self) -> None:
        """Shut down all sessions and the browser."""
        for session in list(self._sessions.values()):
            await self._close_session(session)
        if self._browser:
            await self._browser.close()
        if self._pw:
            await self._pw.stop()

    async def _close_session(self, session: ApplySession) -> None:
        """Close a session's page and context."""
        try:
            context = session.page.context
            await session.page.close()
            await context.close()
        except Exception:
            pass
        self._sessions.pop(session.session_id, None)

    def _screenshot_b64(self, session: ApplySession) -> str:
        if session.screenshot:
            return base64.b64encode(session.screenshot).decode()
        return ""

    def _get_apply_url(self, job: dict) -> str:
        """Determine the apply URL from job data."""
        url = job.get("url", "")
        source = job.get("source", "").lower()

        # Greenhouse: board page → append #app to scroll to application
        if "greenhouse" in source or "boards.greenhouse.io" in url:
            if "/jobs/" in url and "#app" not in url:
                return url + "#app"

        # Lever: job page → append /apply
        if "lever" in source or "jobs.lever.co" in url:
            clean = url.rstrip("/")
            if not clean.endswith("/apply"):
                return clean + "/apply"

        return url

    def _record_application(self, session: ApplySession) -> None:
        """Save application to history JSON."""
        self._output_dir.mkdir(parents=True, exist_ok=True)

        applications = []
        if self._applications_path.exists():
            try:
                applications = json.loads(
                    self._applications_path.read_text(encoding="utf-8")
                )
            except (json.JSONDecodeError, OSError):
                applications = []

        from datetime import datetime

        now = datetime.now().isoformat()
        applications.append(
            {
                "id": uuid.uuid4().hex[:8],
                "job_title": session.job.get("title", ""),
                "company": session.job.get("company", ""),
                "url": session.job.get("url", ""),
                "source": session.job.get("source", ""),
                "applied_at": now,
                "status": "submitted",
                "notes": "",
                "updated_at": now,
            }
        )

        self._applications_path.write_text(
            json.dumps(applications, ensure_ascii=False, indent=2), encoding="utf-8"
        )
