from __future__ import annotations

import asyncio

import click
from rich.console import Console

from ee_it_jobs.config import load_config


@click.group()
def main():
    """Estonian IT Job Crawler"""
    pass


@main.command()
@click.option("--output", "-o", default="output", help="Output directory")
@click.option("--sources", "-s", multiple=True, help="Only run specific sources (e.g. cv.ee, lever/pipedrive)")
@click.option("--concurrency", "-c", default=4, help="Max concurrent scrapers")
def scrape(output, sources, concurrency):
    """Scrape all configured job sources."""
    from ee_it_jobs.runner import run_all
    config = load_config(output_dir=output, sources=sources, concurrency=concurrency)
    asyncio.run(run_all(config))


@main.command("list-sources")
def list_sources():
    """List all configured scraper sources."""
    from ee_it_jobs.scrapers import SCRAPER_CONFIGS
    console = Console()
    console.print("\n[bold]Available sources:[/bold]\n")
    for cls, kwargs in SCRAPER_CONFIGS:
        slug = kwargs.get("company_slug", "")
        board = kwargs.get("board_token", "")
        cid = kwargs.get("company_id", "")
        if slug:
            name = f"lever/{slug}"
        elif board:
            name = f"greenhouse/{board}"
        elif cid:
            name = f"smartrecruiters/{cid.lower()}"
        else:
            name = cls.name
        browser = "browser" if cls.requires_browser else "http"
        console.print(f"  {name:<30} [{browser}]")
    console.print()


@main.command()
@click.option("--host", "-h", default=None, help="Server host")
@click.option("--port", "-p", default=None, type=int, help="Server port")
def web(host, port):
    """Start the web UI to browse scraped jobs."""
    import uvicorn
    from ee_it_jobs.config import load_config as _lc
    config = _lc()
    uvicorn.run(
        "ee_it_jobs.web:app",
        host=host or config.server_host,
        port=port or config.server_port,
        reload=False,
    )


if __name__ == "__main__":
    main()
