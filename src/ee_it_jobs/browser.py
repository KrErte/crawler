from __future__ import annotations

from playwright.async_api import async_playwright, Browser, BrowserContext


class BrowserPool:
    """Manages a shared Playwright Chromium instance."""

    def __init__(self):
        self._pw = None
        self._browser: Browser | None = None

    async def start(self):
        self._pw = await async_playwright().start()
        self._browser = await self._pw.chromium.launch(headless=True)

    async def new_context(self) -> BrowserContext:
        if not self._browser:
            await self.start()
        return await self._browser.new_context(
            user_agent=(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/122.0.0.0 Safari/537.36"
            ),
            viewport={"width": 1280, "height": 720},
        )

    async def close(self):
        if self._browser:
            await self._browser.close()
        if self._pw:
            await self._pw.stop()
