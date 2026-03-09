from __future__ import annotations

import json
from datetime import date
from typing import AsyncIterator

from loguru import logger
from selectolax.parser import HTMLParser

from ee_it_jobs.models import JobListing, WorkplaceType
from ee_it_jobs.scrapers.base import BaseScraper

# cv.ee uses numeric townId values for locations
TOWN_ID_MAP: dict[int, str] = {
    312: "Tallinn",
    314: "Tartu",
    315: "Pärnu",
    316: "Narva",
    317: "Viljandi",
    318: "Rakvere",
    319: "Kuressaare",
    320: "Haapsalu",
    321: "Jõhvi",
    322: "Võru",
    323: "Valga",
    324: "Paide",
    325: "Türi",
    326: "Põlva",
    327: "Rapla",
    328: "Kärdla",
    329: "Elva",
    330: "Keila",
    331: "Maardu",
    332: "Kohtla-Järve",
    333: "Sillamäe",
}

REMOTE_WORK_MAP: dict[str, WorkplaceType] = {
    "ON_SITE": WorkplaceType.ONSITE,
    "HYBRID": WorkplaceType.HYBRID,
    "FULLY_REMOTE": WorkplaceType.REMOTE,
}


class CvEeScraper(BaseScraper):
    name = "cv.ee"
    base_url = "https://www.cv.ee/et/search"
    requires_browser = False

    PAGE_SIZE = 25

    async def scrape(self) -> AsyncIterator[JobListing]:
        offset = 0
        while True:
            url = (
                f"{self.base_url}"
                f"?categories%5B0%5D=INFORMATION_TECHNOLOGY"
                f"&limit={self.PAGE_SIZE}&offset={offset}"
            )
            try:
                html = await self.fetch_page(url)
            except Exception:
                break

            tree = HTMLParser(html)

            script = tree.css_first("script#__NEXT_DATA__")
            if not script or not script.text():
                logger.warning(f"[{self.name}] No __NEXT_DATA__ found at offset {offset}")
                break

            try:
                data = json.loads(script.text())
            except json.JSONDecodeError as e:
                logger.warning(f"[{self.name}] JSON parse error: {e}")
                break

            props = data.get("props", {}).get("pageProps", {})
            search_results = props.get("searchResults", {})
            total = search_results.get("total", 0)
            vacancies = search_results.get("vacancies", [])

            if not isinstance(vacancies, list) or not vacancies:
                logger.info(f"[{self.name}] No vacancies at offset {offset}")
                break

            for item in vacancies:
                yield self._parse_vacancy(item)

            offset += self.PAGE_SIZE
            if offset >= total:
                break

    def _parse_vacancy(self, item: dict) -> JobListing:
        # Workplace type from remoteWorkType field
        remote_raw = item.get("remoteWorkType", "")
        workplace = REMOTE_WORK_MAP.get(remote_raw, WorkplaceType.UNKNOWN)

        # Location from townId
        town_id = item.get("townId")
        if isinstance(town_id, int):
            location = TOWN_ID_MAP.get(town_id, "Estonia")
        else:
            location = "Estonia"

        title = item.get("positionTitle", "")
        company = item.get("employerName", "Unknown")
        job_id = item.get("id", "")
        salary = item.get("salaryText")

        # Date posted from publishDate (format: "YYYY-MM-DD" or ISO)
        date_posted = None
        publish_raw = item.get("publishDate", "")
        if publish_raw:
            try:
                date_posted = date.fromisoformat(publish_raw[:10])
            except (ValueError, TypeError):
                pass

        # Description snippet from positionContent
        content = item.get("positionContent", "")
        snippet = content[:200] if content else None

        return JobListing(
            title=title,
            company=company,
            location=location,
            url=f"https://www.cv.ee/et/vacancy/{job_id}",
            source=self.name,
            workplace_type=workplace,
            salary_text=str(salary) if salary else None,
            date_posted=date_posted,
            description_snippet=snippet,
        )
