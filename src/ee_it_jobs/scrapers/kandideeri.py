from __future__ import annotations

import re
from typing import AsyncIterator

from loguru import logger
from selectolax.parser import HTMLParser

from ee_it_jobs.models import JobListing
from ee_it_jobs.scrapers.base import BaseScraper

_WS = re.compile(r"\s+")


class KandideeriScraper(BaseScraper):
    name = "kandideeri"
    base_url = "https://www.kandideeri.ee/categories/IT-toopakkumised/tarkvara-arendus/"
    requires_browser = False

    async def scrape(self) -> AsyncIterator[JobListing]:
        page = 1
        while True:
            url = self.base_url if page == 1 else f"{self.base_url}?page={page}"
            try:
                html = await self.fetch_page(url)
            except Exception:
                break

            tree = HTMLParser(html)
            cards = tree.css("article.listing-item")

            if not cards:
                logger.info(f"[{self.name}] No cards on page {page}")
                break

            for card in cards:
                job = self._parse_card(card)
                if job is not None:
                    yield job

            if len(cards) < 20:
                break

            page += 1
            if page > 10:
                logger.info(f"[{self.name}] Reached page cap ({page})")
                break

    def _parse_card(self, card) -> JobListing | None:
        title_el = card.css_first(".listing-item__title a.link")
        if not title_el:
            return None
        title = title_el.text(strip=True)
        if not title:
            return None

        href = title_el.attributes.get("href", "")
        if not href:
            return None
        if href.startswith("/"):
            href = f"https://www.kandideeri.ee{href}"

        company_el = card.css_first(".listing-item__additional--company")
        company = company_el.text(strip=True) if company_el else "Unknown"

        location_el = card.css_first(".listing-item__additional--location")
        location = location_el.text(strip=True) if location_el else "Estonia"
        location = _WS.sub(" ", location).strip() or "Estonia"

        return JobListing(
            title=title,
            company=company,
            location=location,
            url=href,
            source=self.name,
        )
