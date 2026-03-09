from __future__ import annotations

import re
from typing import AsyncIterator

from loguru import logger
from selectolax.parser import HTMLParser

from ee_it_jobs.models import JobListing
from ee_it_jobs.scrapers.base import BaseScraper

LOCATION_KEYWORDS = ("estonia", "tallinn", "tartu", "pärnu", "narva", "eesti")


class TeamtailorScraper(BaseScraper):
    """Scraper for companies using Teamtailor career pages (HTML parsing)."""

    requires_browser = False

    def __init__(self, base_url: str, company_name: str, **kwargs):
        super().__init__(**kwargs)
        self.base_url = base_url.rstrip("/")
        self.company_name = company_name
        slug = re.sub(r'https?://', '', base_url).split('.')[0].split('/')[-1]
        self.name = f"teamtailor/{slug}"

    async def scrape(self) -> AsyncIterator[JobListing]:
        page = 1
        while True:
            if page == 1:
                url = f"{self.base_url}/jobs"
            else:
                url = f"{self.base_url}/jobs/show_more?page={page}"

            try:
                html = await self.fetch_page(url)
            except Exception:
                break

            tree = HTMLParser(html)
            job_links = tree.css("li a[href*='/jobs/'], a.focus-visible-company[href*='/jobs/']")
            if not job_links:
                job_links = tree.css("a[href*='/jobs/']")
                job_links = [
                    a for a in job_links
                    if a.text(strip=True)
                    and len(a.text(strip=True)) > 5
                    and a.text(strip=True).lower() not in ("all jobs", "view all jobs", "see all jobs", "jobs")
                ]

            if not job_links:
                break

            for link in job_links:
                job = self._parse_link(link)
                if job:
                    yield job

            has_more = tree.css_first(f"a[href*='show_more?page={page + 1}']")
            if not has_more:
                break
            page += 1

    def _parse_link(self, link) -> JobListing | None:
        href = link.attributes.get("href", "")

        title_el = link.css_first("span, h3, h4, div[class*='title']")
        title = title_el.text(strip=True) if title_el else link.text(strip=True)

        if not title or len(title) < 3:
            return None

        # Extract location from remainder text after title
        full_text = link.text(strip=True)
        location = ""
        if title in full_text:
            remainder = full_text.replace(title, "", 1).strip(" ·–-|,")
            if remainder:
                location = remainder

        loc_el = link.css_first("[class*='location'], [class*='meta']")
        if loc_el:
            location = loc_el.text(strip=True)

        if location and not any(kw in location.lower() for kw in LOCATION_KEYWORDS):
            return None

        # If no location info, skip to avoid including non-Estonian jobs
        if not location:
            return None

        url = href if href.startswith("http") else f"{self.base_url}{href}"

        return JobListing(
            title=title,
            company=self.company_name,
            location=location,
            url=url,
            source=self.name,
        )
