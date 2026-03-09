from __future__ import annotations

from typing import AsyncIterator

from loguru import logger

from ee_it_jobs.models import JobListing
from ee_it_jobs.scrapers.base import BaseScraper

LOCATION_KEYWORDS = ("estonia", "tallinn", "tartu")


class NortalScraper(BaseScraper):
    name = "nortal"
    base_url = "https://careers.nortal.com"
    requires_browser = False

    ICIMS_URL = "https://careers.nortal.com/hs/serverless/career"
    GREENHOUSE_URL = "https://careers.nortal.com/_hcms/api/greenhouse"

    async def scrape(self) -> AsyncIterator[JobListing]:
        seen_titles: set[str] = set()

        # Source 1: iCIMS jobs
        async for job in self._scrape_icims():
            key = job.title.lower().strip()
            if key not in seen_titles:
                seen_titles.add(key)
                yield job

        # Source 2: Greenhouse jobs
        async for job in self._scrape_greenhouse():
            key = job.title.lower().strip()
            if key not in seen_titles:
                seen_titles.add(key)
                yield job

    async def _scrape_icims(self) -> AsyncIterator[JobListing]:
        try:
            data = await self.fetch_json(self.ICIMS_URL)
        except Exception as e:
            logger.warning(f"[{self.name}] iCIMS API failed: {e}")
            return

        jobs = data if isinstance(data, list) else data.get("jobs", []) if isinstance(data, dict) else []
        for item in jobs:
            location = self._extract_location(item)
            if not self._is_estonia(location):
                continue

            title = item.get("title") or item.get("name", "")
            url = item.get("url") or item.get("link", "")
            if not url.startswith("http"):
                url = f"{self.base_url}{url}"

            yield JobListing(
                title=title,
                company="Nortal",
                location=location or "Estonia",
                url=url,
                source=self.name,
            )

    async def _scrape_greenhouse(self) -> AsyncIterator[JobListing]:
        try:
            data = await self.fetch_json(self.GREENHOUSE_URL)
        except Exception as e:
            logger.warning(f"[{self.name}] Greenhouse API failed: {e}")
            return

        jobs = data if isinstance(data, list) else data.get("jobs", []) if isinstance(data, dict) else []
        for item in jobs:
            location = self._extract_location(item)
            if not self._is_estonia(location):
                continue

            title = item.get("title") or item.get("name", "")
            url = item.get("absolute_url") or item.get("url") or item.get("link", "")
            if not url.startswith("http"):
                url = f"{self.base_url}{url}"

            yield JobListing(
                title=title,
                company="Nortal",
                location=location or "Estonia",
                url=url,
                source=self.name,
            )

    @staticmethod
    def _extract_location(item: dict) -> str:
        loc = item.get("location", "")
        if isinstance(loc, dict):
            return loc.get("name", "")
        return str(loc)

    @staticmethod
    def _is_estonia(location: str) -> bool:
        return any(kw in location.lower() for kw in LOCATION_KEYWORDS)
