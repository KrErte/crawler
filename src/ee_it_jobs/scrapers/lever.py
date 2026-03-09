from __future__ import annotations

from typing import AsyncIterator

from ee_it_jobs.models import JobListing, WorkplaceType
from ee_it_jobs.scrapers.base import BaseScraper


class LeverScraper(BaseScraper):
    """Generic scraper for companies using Lever ATS (public JSON API)."""

    requires_browser = False

    def __init__(self, company_slug: str, company_name: str, location_filter: str = "Estonia", api_host: str = "api.lever.co", **kwargs):
        super().__init__(**kwargs)
        self.company_slug = company_slug
        self.company_name = company_name
        self.location_filter = location_filter.lower()
        self.api_host = api_host
        self.name = f"lever/{company_slug}"

    async def scrape(self) -> AsyncIterator[JobListing]:
        url = f"https://{self.api_host}/v0/postings/{self.company_slug}?mode=json"
        postings = await self.fetch_json(url)
        if not isinstance(postings, list):
            return

        for post in postings:
            location = post.get("categories", {}).get("location", "")
            if self.location_filter and self.location_filter not in location.lower():
                continue

            workplace = WorkplaceType.UNKNOWN
            wp_raw = post.get("workplaceType", "")
            if "remote" in wp_raw.lower():
                workplace = WorkplaceType.REMOTE
            elif "hybrid" in wp_raw.lower():
                workplace = WorkplaceType.HYBRID
            elif wp_raw:
                workplace = WorkplaceType.ONSITE

            yield JobListing(
                title=post.get("text", ""),
                company=self.company_name,
                location=location or "Estonia",
                url=post.get("hostedUrl", ""),
                source=self.name,
                department=post.get("categories", {}).get("department"),
                workplace_type=workplace,
                description_snippet=(post.get("descriptionPlain", "") or "")[:200],
            )
