from __future__ import annotations

import abc
import time
from typing import AsyncIterator

import httpx
from loguru import logger

from ee_it_jobs.models import JobListing, ScrapeResult
from ee_it_jobs.rate_limiter import RateLimiter


class BaseScraper(abc.ABC):
    """Abstract base for all job scrapers."""

    name: str = "base"
    base_url: str = ""
    requires_browser: bool = False

    def __init__(
        self,
        http_client: httpx.AsyncClient,
        rate_limiter: RateLimiter,
        browser=None,
    ):
        self.client = http_client
        self.rate_limiter = rate_limiter
        self.browser = browser
        self._errors: list[str] = []
        self._pages_scraped: int = 0

    async def run(self) -> ScrapeResult:
        start = time.monotonic()
        jobs: list[JobListing] = []
        try:
            async for job in self.scrape():
                jobs.append(job)
        except Exception as e:
            logger.error(f"[{self.name}] Fatal error: {e}")
            self._errors.append(str(e))

        return ScrapeResult(
            source=self.name,
            jobs=jobs,
            errors=self._errors,
            duration_seconds=round(time.monotonic() - start, 2),
            pages_scraped=self._pages_scraped,
        )

    @abc.abstractmethod
    async def scrape(self) -> AsyncIterator[JobListing]:
        ...
        yield  # type: ignore[misc]

    async def fetch_page(self, url: str, **kwargs) -> str:
        await self.rate_limiter.acquire()
        try:
            resp = await self.client.get(url, **kwargs)
            resp.raise_for_status()
            self._pages_scraped += 1
            return resp.text
        except httpx.HTTPStatusError as e:
            msg = f"HTTP {e.response.status_code}: {url}"
            logger.warning(f"[{self.name}] {msg}")
            self._errors.append(msg)
            raise
        except httpx.RequestError as e:
            msg = f"Request error: {url} — {e}"
            logger.warning(f"[{self.name}] {msg}")
            self._errors.append(msg)
            raise

    async def fetch_json(self, url: str, **kwargs) -> dict | list:
        await self.rate_limiter.acquire()
        try:
            resp = await self.client.get(url, **kwargs)
            resp.raise_for_status()
            self._pages_scraped += 1
            return resp.json()
        except httpx.HTTPStatusError as e:
            msg = f"HTTP {e.response.status_code}: {url}"
            logger.warning(f"[{self.name}] {msg}")
            self._errors.append(msg)
            raise
        except httpx.RequestError as e:
            msg = f"Request error: {url} — {e}"
            logger.warning(f"[{self.name}] {msg}")
            self._errors.append(msg)
            raise

    async def fetch_with_browser(self, url: str, wait_selector: str | None = None) -> str:
        if not self.browser:
            raise RuntimeError(f"[{self.name}] Browser required but not provided")
        page = await self.browser.new_page()
        try:
            await page.goto(url, wait_until="networkidle", timeout=30_000)
            if wait_selector:
                await page.wait_for_selector(wait_selector, timeout=15_000)
            self._pages_scraped += 1
            return await page.content()
        finally:
            await page.close()
