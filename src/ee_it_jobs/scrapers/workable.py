from __future__ import annotations

from datetime import date
from typing import AsyncIterator

from ee_it_jobs.models import JobListing
from ee_it_jobs.scrapers.base import BaseScraper

LOCATION_KEYWORDS = ("estonia", "tallinn", "tartu", "pärnu", "narva")


class WorkableScraper(BaseScraper):
    """Scraper for companies using Workable ATS (Widget API)."""

    requires_browser = False

    def __init__(self, account_slug: str, company_name: str, **kwargs):
        super().__init__(**kwargs)
        self.account_slug = account_slug
        self.company_name = company_name
        self.name = f"workable/{account_slug}"

    async def scrape(self) -> AsyncIterator[JobListing]:
        url = f"https://apply.workable.com/api/v1/widget/accounts/{self.account_slug}"
        try:
            data = await self.fetch_json(url)
        except Exception:
            return

        if not isinstance(data, dict):
            return

        for job in data.get("jobs", []):
            if not self._is_estonia(job):
                continue

            city = job.get("city", "")
            country = job.get("country", "")
            location = f"{city}, {country}".strip(", ") or "Estonia"

            date_posted = None
            pub_raw = job.get("published_on", "")
            if pub_raw:
                try:
                    date_posted = date.fromisoformat(pub_raw[:10])
                except (ValueError, TypeError):
                    pass

            yield JobListing(
                title=job.get("title", ""),
                company=self.company_name,
                location=location,
                url=job.get("url", ""),
                source=self.name,
                department=job.get("department"),
                date_posted=date_posted,
            )

    @staticmethod
    def _is_estonia(job: dict) -> bool:
        country = (job.get("country") or "").lower()
        city = (job.get("city") or "").lower()
        if any(kw in country for kw in LOCATION_KEYWORDS):
            return True
        if any(kw in city for kw in LOCATION_KEYWORDS):
            return True
        for loc in job.get("locations", []):
            loc_str = str(loc).lower() if not isinstance(loc, dict) else (loc.get("country", "") + " " + loc.get("city", "")).lower()
            if any(kw in loc_str for kw in LOCATION_KEYWORDS):
                return True
        return False
