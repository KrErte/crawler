from __future__ import annotations

import re
from pathlib import Path

from loguru import logger
from playwright.async_api import Page

from ee_it_jobs.models import UserProfile


class BaseFormFiller:
    """Base class for ATS-specific form fillers."""

    async def fill(self, page: Page, profile: UserProfile, cv_path: Path) -> bool:
        """Fill the application form. Returns True if successful."""
        raise NotImplementedError

    async def _fill_input(self, page: Page, selector: str, value: str) -> bool:
        """Fill a single input field if it exists and value is non-empty."""
        if not value:
            return False
        try:
            el = page.locator(selector).first
            if await el.is_visible(timeout=2000):
                await el.click()
                await el.fill(value)
                return True
        except Exception:
            pass
        return False

    async def _upload_file(self, page: Page, selector: str, file_path: Path) -> bool:
        """Upload a file to a file input."""
        try:
            el = page.locator(selector).first
            await el.set_input_files(str(file_path))
            return True
        except Exception:
            logger.debug(f"File upload failed with selector {selector}")
            return False

    async def _fill_by_label(
        self, page: Page, label_pattern: str, value: str
    ) -> bool:
        """Fill an input field found by its associated label text."""
        if not value:
            return False
        try:
            el = page.get_by_label(re.compile(label_pattern, re.IGNORECASE)).first
            if await el.is_visible(timeout=2000):
                await el.click()
                await el.fill(value)
                return True
        except Exception:
            pass
        return False


class GreenhouseFormFiller(BaseFormFiller):
    """Greenhouse ATS form filler.

    URL pattern: boards.greenhouse.io/*/jobs/*
    Selectors: #first_name, #last_name, #email, #phone, input[type=file]
    """

    async def fill(self, page: Page, profile: UserProfile, cv_path: Path) -> bool:
        logger.info("Filling Greenhouse form")
        try:
            await page.wait_for_selector("#first_name", timeout=10000)
        except Exception:
            logger.warning("Greenhouse form not found, trying generic")
            return False

        await self._fill_input(page, "#first_name", profile.first_name)
        await self._fill_input(page, "#last_name", profile.last_name)
        await self._fill_input(page, "#email", profile.email)
        await self._fill_input(page, "#phone", profile.phone)

        if profile.linkedin_url:
            # Greenhouse often has a LinkedIn field with various selectors
            filled = await self._fill_input(
                page, "input[name*='linkedin' i]", profile.linkedin_url
            )
            if not filled:
                await self._fill_input(
                    page, "input[autocomplete='url']", profile.linkedin_url
                )

        if cv_path.exists():
            # Greenhouse uses a visible file input or a drop zone
            await self._upload_file(page, "input[type='file']", cv_path)

        logger.info("Greenhouse form filled successfully")
        return True


class LeverFormFiller(BaseFormFiller):
    """Lever ATS form filler.

    URL pattern: jobs.lever.co/*/apply
    Selectors: input[name="name"], input[name="email"], etc.
    """

    async def fill(self, page: Page, profile: UserProfile, cv_path: Path) -> bool:
        logger.info("Filling Lever form")
        try:
            await page.wait_for_selector(
                "input[name='name']", timeout=10000
            )
        except Exception:
            logger.warning("Lever form not found")
            return False

        full_name = f"{profile.first_name} {profile.last_name}".strip()
        await self._fill_input(page, "input[name='name']", full_name)
        await self._fill_input(page, "input[name='email']", profile.email)
        await self._fill_input(page, "input[name='phone']", profile.phone)
        await self._fill_input(
            page, "input[name='urls[LinkedIn]']", profile.linkedin_url
        )
        await self._fill_input(
            page, "input[name='urls[Other]']", profile.linkedin_url
        )

        if cv_path.exists():
            await self._upload_file(page, "input[type='file']", cv_path)

        if profile.cover_letter:
            await self._fill_input(
                page, "textarea[name='comments']", profile.cover_letter
            )

        logger.info("Lever form filled successfully")
        return True


class WorkableFormFiller(BaseFormFiller):
    """Workable ATS form filler.

    URL pattern: apply.workable.com/*/j/*/apply
    Selectors: input[name="firstname"], input[name="lastname"], etc.
    """

    async def fill(self, page: Page, profile: UserProfile, cv_path: Path) -> bool:
        logger.info("Filling Workable form")
        try:
            await page.wait_for_selector(
                "input[name='firstname'], input[data-ui='firstname']",
                timeout=10000,
            )
        except Exception:
            logger.warning("Workable form not found")
            return False

        await self._fill_input(page, "input[name='firstname']", profile.first_name)
        await self._fill_input(page, "input[name='lastname']", profile.last_name)
        await self._fill_input(page, "input[name='email']", profile.email)
        await self._fill_input(page, "input[name='phone']", profile.phone)

        if cv_path.exists():
            await self._upload_file(page, "input[type='file']", cv_path)

        logger.info("Workable form filled successfully")
        return True


class SmartRecruitersFormFiller(BaseFormFiller):
    """SmartRecruiters ATS form filler.

    URL pattern: jobs.smartrecruiters.com/*/apply
    Selectors: input[name="firstName"], input[name="lastName"], etc.
    """

    async def fill(self, page: Page, profile: UserProfile, cv_path: Path) -> bool:
        logger.info("Filling SmartRecruiters form")
        try:
            await page.wait_for_selector(
                "input[name='firstName'], input[aria-label*='First']",
                timeout=10000,
            )
        except Exception:
            logger.warning("SmartRecruiters form not found")
            return False

        await self._fill_input(page, "input[name='firstName']", profile.first_name)
        await self._fill_input(page, "input[name='lastName']", profile.last_name)
        await self._fill_input(page, "input[name='email']", profile.email)
        await self._fill_input(
            page, "input[name='phoneNumber']", profile.phone
        )

        if cv_path.exists():
            await self._upload_file(page, "input[type='file']", cv_path)

        logger.info("SmartRecruiters form filled successfully")
        return True


class GenericFormFiller(BaseFormFiller):
    """Generic form filler for unknown ATS platforms.

    Uses label/placeholder text matching to find fields.
    Fallback for cv.ee, Teamtailor, and other platforms.
    """

    FIRST_NAME_PATTERNS = r"first.?name|eesnimi|given.?name|vorname"
    LAST_NAME_PATTERNS = r"last.?name|perenimi|surname|family.?name|nachname"
    EMAIL_PATTERNS = r"e?.?mail|meil"
    PHONE_PATTERNS = r"phone|telefon|tel\b|mobile|mobil"
    LINKEDIN_PATTERNS = r"linkedin"
    NAME_PATTERNS = r"^name$|full.?name|nimi"

    async def fill(self, page: Page, profile: UserProfile, cv_path: Path) -> bool:
        logger.info("Filling form with generic filler")

        # Wait for any form to appear
        try:
            await page.wait_for_selector(
                "form, input[type='text'], input[type='email']", timeout=10000
            )
        except Exception:
            logger.warning("No form found on page")
            return False

        filled_any = False

        # Try first/last name separately
        first = await self._fill_by_label(
            page, self.FIRST_NAME_PATTERNS, profile.first_name
        )
        last = await self._fill_by_label(
            page, self.LAST_NAME_PATTERNS, profile.last_name
        )

        # If no separate fields, try combined name
        if not first and not last:
            full_name = f"{profile.first_name} {profile.last_name}".strip()
            await self._fill_by_label(page, self.NAME_PATTERNS, full_name)

        filled_any = first or last or filled_any

        # Try by label first, then by placeholder
        for pattern, value in [
            (self.EMAIL_PATTERNS, profile.email),
            (self.PHONE_PATTERNS, profile.phone),
            (self.LINKEDIN_PATTERNS, profile.linkedin_url),
        ]:
            if value:
                result = await self._fill_by_label(page, pattern, value)
                if not result:
                    result = await self._fill_by_placeholder(page, pattern, value)
                filled_any = filled_any or result

        # Try to fill email by type attribute
        if profile.email:
            await self._fill_input(page, "input[type='email']", profile.email)
            filled_any = True

        # Upload CV
        if cv_path.exists():
            uploaded = await self._upload_file(page, "input[type='file']", cv_path)
            filled_any = filled_any or uploaded

        # Cover letter
        if profile.cover_letter:
            await self._fill_by_label(
                page, r"cover.?letter|kaaskiri|message|sõnum", profile.cover_letter
            )

        logger.info(f"Generic form fill {'succeeded' if filled_any else 'failed'}")
        return filled_any

    async def _fill_by_placeholder(
        self, page: Page, pattern: str, value: str
    ) -> bool:
        """Fill an input found by placeholder text."""
        if not value:
            return False
        try:
            el = page.get_by_placeholder(re.compile(pattern, re.IGNORECASE)).first
            if await el.is_visible(timeout=2000):
                await el.click()
                await el.fill(value)
                return True
        except Exception:
            pass
        return False


def get_form_filler(source: str, url: str) -> BaseFormFiller:
    """Select the appropriate form filler based on job source or URL."""
    source_lower = source.lower()
    url_lower = url.lower()

    if "greenhouse" in source_lower or "boards.greenhouse.io" in url_lower:
        return GreenhouseFormFiller()
    if "lever" in source_lower or "jobs.lever.co" in url_lower:
        return LeverFormFiller()
    if "workable" in source_lower or "apply.workable.com" in url_lower:
        return WorkableFormFiller()
    if "smartrecruiters" in source_lower or "jobs.smartrecruiters.com" in url_lower:
        return SmartRecruitersFormFiller()

    return GenericFormFiller()
