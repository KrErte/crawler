from __future__ import annotations

import asyncio
import time

import httpx
from loguru import logger
from rich.console import Console
from rich.progress import Progress, SpinnerColumn, TextColumn, BarColumn, TaskProgressColumn
from rich.table import Table

from ee_it_jobs.browser import BrowserPool
from ee_it_jobs.config import Config
from ee_it_jobs.models import ScrapeResult
from ee_it_jobs.output import deduplicate, export_json, export_csv
from ee_it_jobs.rate_limiter import RateLimiter
from ee_it_jobs.scrapers import SCRAPER_CONFIGS


async def run_all(config: Config) -> list:
    """Run all scrapers and export results. Returns deduplicated job list."""
    console = Console()
    results: list[ScrapeResult] = []

    # Filter sources if specified
    configs = SCRAPER_CONFIGS
    if config.sources:
        source_set = set(config.sources)
        filtered = []
        for cls, kwargs in configs:
            slug = kwargs.get("company_slug", "")
            board = kwargs.get("board_token", "")
            cid = kwargs.get("company_id", "")
            scraper_name = cls.name if hasattr(cls, "name") and cls.name != "base" else ""
            names = {
                scraper_name,
                f"lever/{slug}" if slug else "",
                f"greenhouse/{board}" if board else "",
                f"smartrecruiters/{cid.lower()}" if cid else "",
            }
            if names & source_set:
                filtered.append((cls, kwargs))
        configs = filtered

    if not configs:
        console.print("[red]No matching sources found.[/red]")
        return []

    # Check if any scraper needs a browser
    needs_browser = any(cls.requires_browser for cls, _ in configs)
    browser_pool = None
    browser_ctx = None

    if needs_browser:
        try:
            browser_pool = BrowserPool()
        except Exception:
            console.print("[yellow]Playwright not available, skipping browser-based scrapers.[/yellow]")
            configs = [(cls, kw) for cls, kw in configs if not cls.requires_browser]

    async with httpx.AsyncClient(
        headers={"User-Agent": config.user_agent},
        timeout=httpx.Timeout(config.request_timeout),
        follow_redirects=True,
    ) as client:
        if browser_pool:
            console.print("[dim]Starting browser...[/dim]")
            try:
                await browser_pool.start()
                browser_ctx = await browser_pool.new_context()
            except Exception as e:
                console.print(f"[yellow]Browser start failed: {e}. Skipping browser scrapers.[/yellow]")
                browser_pool = None
                configs = [(cls, kw) for cls, kw in configs if not cls.requires_browser]

        rate_limiter = RateLimiter(requests_per_second=config.rate_limit)

        scrapers = []
        for cls, kwargs in configs:
            scraper = cls(
                http_client=client,
                rate_limiter=rate_limiter,
                browser=browser_ctx,
                **kwargs,
            )
            scrapers.append(scraper)

        sem = asyncio.Semaphore(config.max_concurrency)

        with Progress(
            SpinnerColumn(),
            TextColumn("[bold blue]{task.description}"),
            BarColumn(),
            TaskProgressColumn(),
            console=console,
        ) as progress:
            overall = progress.add_task("Scraping jobs...", total=len(scrapers))

            async def run_one(scraper):
                async with sem:
                    task_id = progress.add_task(f"  {scraper.name}", total=1)
                    result = await scraper.run()
                    progress.update(task_id, completed=1)
                    progress.update(overall, advance=1)
                    return result

            results = await asyncio.gather(*[run_one(s) for s in scrapers])

        if browser_pool:
            await browser_pool.close()

    # Merge and deduplicate
    all_jobs = []
    for r in results:
        all_jobs.extend(r.jobs)
    unique_jobs = deduplicate(all_jobs)

    # Summary table
    table = Table(title="Scrape Results", show_lines=True)
    table.add_column("Source", style="cyan")
    table.add_column("Jobs", justify="right", style="green")
    table.add_column("Errors", justify="right", style="red")
    table.add_column("Time (s)", justify="right")

    for r in sorted(results, key=lambda r: len(r.jobs), reverse=True):
        table.add_row(
            r.source,
            str(len(r.jobs)),
            str(len(r.errors)),
            str(r.duration_seconds),
        )

    table.add_section()
    table.add_row("[bold]TOTAL (deduped)[/bold]", f"[bold]{len(unique_jobs)}[/bold]", "", "")
    console.print(table)

    # Export
    json_path = config.output_dir / f"jobs_{config.run_date}.json"
    csv_path = config.output_dir / f"jobs_{config.run_date}.csv"
    export_json(unique_jobs, results, json_path)
    export_csv(unique_jobs, csv_path)
    console.print(f"\n[green]Exported {len(unique_jobs)} jobs:[/green]")
    console.print(f"  JSON: {json_path}")
    console.print(f"  CSV:  {csv_path}")

    return unique_jobs
