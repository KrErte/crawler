import asyncio
import time


class RateLimiter:
    """Simple token-bucket rate limiter for async code."""

    def __init__(self, requests_per_second: float = 2.0):
        self._min_interval = 1.0 / requests_per_second
        self._last_request = 0.0
        self._lock = asyncio.Lock()

    async def acquire(self):
        async with self._lock:
            now = time.monotonic()
            elapsed = now - self._last_request
            if elapsed < self._min_interval:
                await asyncio.sleep(self._min_interval - elapsed)
            self._last_request = time.monotonic()
