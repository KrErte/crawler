from __future__ import annotations

import re
from typing import AsyncIterator

from loguru import logger
from selectolax.parser import HTMLParser

from ee_it_jobs.models import JobListing
from ee_it_jobs.scrapers.base import BaseScraper

_WS = re.compile(r"\s+")


class CvKeskusScraper(BaseScraper):
    name = "cvkeskus"
    base_url = "https://www.cvkeskus.ee/toopakkumised-infotehnoloogia-valdkonnas"
    requires_browser = False

    PAGE_SIZE = 25

    async def scrape(self) -> AsyncIterator[JobListing]:
        offset = 0
        while True:
            url = f"{self.base_url}?start={offset}"
            try:
                html = await self.fetch_page(url)
            except Exception:
                break

            tree = HTMLParser(html)
            cards = tree.css("a.jobad-url")

            if not cards:
                logger.info(f"[{self.name}] No cards at offset {offset}")
                break

            for card in cards:
                job = self._parse_card(card)
                if job is not None:
                    yield job

            # Stop if fewer than a full page or we've gone far enough
            if len(cards) < self.PAGE_SIZE:
                break

            offset += self.PAGE_SIZE

            # Safety cap – cvkeskus IT category has ~350 jobs
            if offset >= 400:
                logger.info(f"[{self.name}] Reached offset cap ({offset})")
                break

    def _parse_card(self, card) -> JobListing | None:
        # Title
        h2 = card.css_first("h2")
        if not h2:
            return None
        title = h2.text(strip=True)
        if not title:
            return None
        # Normalise ALL-CAPS titles to title-case
        if title == title.upper():
            title = title.title()

        # Company
        company_el = card.css_first(".job-company")
        company = company_el.text(strip=True) if company_el else "Unknown"

        # Location — span.location sits inside a parent span with optional "Kaugtöö"
        location = "Estonia"
        loc_el = card.css_first("span.location")
        if loc_el and loc_el.parent:
            raw = loc_el.parent.text(strip=True)
            location = _WS.sub(" ", raw).strip() or "Estonia"

        # Salary — use the desktop salary-block (hidden lg:inline-block)
        salary_text = None
        salary_el = card.css_first("div.salary-block")
        if salary_el:
            raw = salary_el.text(strip=True)
            if raw:
                salary_text = _WS.sub(" ", raw)

        # URL
        href = card.attributes.get("href", "")
        job_url = f"https://www.cvkeskus.ee{href}" if href.startswith("/") else href

        return JobListing(
            title=title,
            company=company,
            location=location,
            url=job_url,
            source=self.name,
            salary_text=salary_text,
        )
