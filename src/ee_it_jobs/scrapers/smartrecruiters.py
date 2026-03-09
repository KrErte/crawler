from __future__ import annotations

from typing import AsyncIterator

from ee_it_jobs.models import JobListing
from ee_it_jobs.scrapers.base import BaseScraper


class SmartRecruitersScraper(BaseScraper):
    """Generic scraper for SmartRecruiters-hosted career pages."""

    requires_browser = False

    def __init__(self, company_id: str, company_name: str, location_filter: str = "Estonia", **kwargs):
        super().__init__(**kwargs)
        self.company_id = company_id
        self.company_name = company_name
        self.location_filter = location_filter.lower()
        self.name = f"smartrecruiters/{company_id.lower()}"

    async def scrape(self) -> AsyncIterator[JobListing]:
        offset = 0
        limit = 100
        while True:
            url = (
                f"https://api.smartrecruiters.com/v1/companies/{self.company_id}"
                f"/postings?offset={offset}&limit={limit}"
            )
            data = await self.fetch_json(url)
            if not isinstance(data, dict):
                break

            content = data.get("content", [])
            if not content:
                break

            for post in content:
                loc = post.get("location", {})
                city = loc.get("city", "")
                country = loc.get("country", "")
                loc_str = f"{city}, {country}".strip(", ")

                if self.location_filter and self.location_filter not in loc_str.lower():
                    if not any(
                        c in loc_str.lower()
                        for c in ("tallinn", "tartu", "estoni")
                    ):
                        continue

                dept = post.get("department", {})
                dept_name = dept.get("label") if isinstance(dept, dict) else None

                yield JobListing(
                    title=post.get("name", ""),
                    company=self.company_name,
                    location=loc_str or "Estonia",
                    url=post.get("ref", f"https://jobs.smartrecruiters.com/{self.company_id}/{post.get('id', '')}"),
                    source=self.name,
                    department=dept_name,
                )

            if len(content) < limit:
                break
            offset += limit
