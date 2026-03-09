from __future__ import annotations

from typing import AsyncIterator

from ee_it_jobs.models import JobListing
from ee_it_jobs.scrapers.base import BaseScraper


class GreenhouseScraper(BaseScraper):
    """Generic scraper for companies using Greenhouse ATS (public JSON API)."""

    requires_browser = False

    def __init__(self, board_token: str, company_name: str, location_filter: str = "Estonia", **kwargs):
        super().__init__(**kwargs)
        self.board_token = board_token
        self.company_name = company_name
        self.location_filter = location_filter.lower()
        self.name = f"greenhouse/{board_token}"

    async def scrape(self) -> AsyncIterator[JobListing]:
        url = f"https://boards-api.greenhouse.io/v1/boards/{self.board_token}/jobs"
        data = await self.fetch_json(url)
        if not isinstance(data, dict):
            return

        jobs = data.get("jobs", [])
        for job in jobs:
            location = job.get("location", {}).get("name", "")
            if self.location_filter and self.location_filter not in location.lower():
                # Also check for Tallinn, Tartu etc.
                if not any(
                    city in location.lower()
                    for city in ("tallinn", "tartu", "estoni")
                ):
                    continue

            departments = job.get("departments", [])
            dept = departments[0].get("name") if departments else None

            yield JobListing(
                title=job.get("title", ""),
                company=self.company_name,
                location=location or "Estonia",
                url=job.get("absolute_url", ""),
                source=self.name,
                department=dept,
            )
