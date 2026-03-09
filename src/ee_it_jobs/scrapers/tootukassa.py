from __future__ import annotations

from typing import AsyncIterator

from loguru import logger
from selectolax.parser import HTMLParser

from ee_it_jobs.models import JobListing
from ee_it_jobs.scrapers.base import BaseScraper


class TootukassaScraper(BaseScraper):
    name = "tootukassa"
    base_url = "https://www.tootukassa.ee/toopakkumised"
    requires_browser = True

    IT_KEYWORDS = (
        "developer", "engineer", "programmer", "architect", "devops",
        "software", "frontend", "backend", "fullstack", "full-stack",
        "data", "cloud", "sre", "qa", "test", "security", "cyber",
        "it ", "admin", "system", "network", "database", "analyst",
        "arendaja", "programmeerija", "tarkvara", "andme",
    )

    async def scrape(self) -> AsyncIterator[JobListing]:
        try:
            html = await self.fetch_with_browser(
                self.base_url,
                wait_selector="a[href*='toopakkumine']",
            )
        except Exception as e:
            logger.warning(f"[{self.name}] Browser fetch failed: {e}")
            return

        tree = HTMLParser(html)

        for link in tree.css("a[href*='toopakkumine'], a[href*='job-offer']"):
            title = link.text(strip=True)
            href = link.attributes.get("href", "")
            if not title or len(title) < 3:
                continue

            # Filter for IT-related jobs
            title_lower = title.lower()
            if not any(kw in title_lower for kw in self.IT_KEYWORDS):
                continue

            # Try to get company from nearby elements
            company = "Unknown"
            parent = link.parent
            if parent:
                company_el = parent.css_first("[class*='company'], [class*='employer'], span")
                if company_el and company_el != link:
                    company = company_el.text(strip=True) or "Unknown"

            url = href if href.startswith("http") else f"https://www.tootukassa.ee{href}"

            yield JobListing(
                title=title,
                company=company,
                location="Estonia",
                url=url,
                source=self.name,
            )
