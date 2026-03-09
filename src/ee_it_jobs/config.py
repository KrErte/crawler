from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date
from pathlib import Path

try:
    import tomllib
except ModuleNotFoundError:
    import tomli as tomllib  # type: ignore[no-redef]


@dataclass
class Config:
    rate_limit: float = 2.0
    max_concurrency: int = 4
    request_timeout: float = 30.0
    output_dir: Path = Path("output")
    user_agent: str = "EE-IT-Jobs-Crawler/0.1"
    sources: tuple[str, ...] = ()
    run_date: str = field(default_factory=lambda: date.today().isoformat())
    server_host: str = "127.0.0.1"
    server_port: int = 8000


def load_config(
    *,
    output_dir: str = "output",
    sources: tuple[str, ...] = (),
    concurrency: int = 4,
) -> Config:
    config_path = Path("config.toml")
    cfg = Config(output_dir=Path(output_dir), sources=sources, max_concurrency=concurrency)

    if config_path.exists():
        with open(config_path, "rb") as f:
            data = tomllib.load(f)
        crawler = data.get("crawler", {})
        cfg.rate_limit = crawler.get("rate_limit", cfg.rate_limit)
        cfg.max_concurrency = concurrency or crawler.get("max_concurrency", cfg.max_concurrency)
        cfg.request_timeout = crawler.get("request_timeout", cfg.request_timeout)
        cfg.user_agent = crawler.get("user_agent", cfg.user_agent)
        cfg.output_dir = Path(crawler.get("output_dir", str(cfg.output_dir)))

        server = data.get("server", {})
        cfg.server_host = server.get("host", cfg.server_host)
        cfg.server_port = server.get("port", cfg.server_port)

    cfg.output_dir.mkdir(parents=True, exist_ok=True)
    return cfg
