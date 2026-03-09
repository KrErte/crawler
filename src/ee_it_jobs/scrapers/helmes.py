from __future__ import annotations

from typing import AsyncIterator

from selectolax.parser import HTMLParser

from ee_it_jobs.models import JobListing
from ee_it_jobs.scrapers.base import BaseScraper


class HelmesScraper(BaseScraper):
    name = "helmes"
    base_url = "https://www.helmes.com/careers/"
    requires_browser = False

    async def scrape(self) -> AsyncIterator[JobListing]:
        html = await self.fetch_page(self.base_url)
        tree = HTMLParser(html)

        for link in tree.css("a[href*='career'], a[href*='position'], a[href*='job']"):
            title = link.text(strip=True)
            href = link.attributes.get("href", "")
            if not title or len(title) < 5:
                continue
            # Skip navigation links
            if title.lower() in ("careers", "jobs", "open positions", "career"):
                continue

            url = href if href.startswith("http") else f"https://www.helmes.com{href}"
            yield JobListing(
                title=title,
                company="Helmes",
                location="Tallinn, Estonia",
                url=url,
                source=self.name,
            )
