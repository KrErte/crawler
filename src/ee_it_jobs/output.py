from __future__ import annotations

import csv
import json
from datetime import datetime
from pathlib import Path

from ee_it_jobs.models import JobListing, ScrapeResult


def deduplicate(jobs: list[JobListing]) -> list[JobListing]:
    seen: set[str] = set()
    unique: list[JobListing] = []
    for job in jobs:
        key = job.dedup_key
        if key not in seen:
            seen.add(key)
            unique.append(job)
    return unique


def export_json(
    jobs: list[JobListing],
    results: list[ScrapeResult],
    path: Path,
) -> None:
    sources = {}
    for r in results:
        sources[r.source] = {
            "count": len(r.jobs),
            "errors": len(r.errors),
            "duration_seconds": r.duration_seconds,
        }

    payload = {
        "scraped_at": datetime.now().isoformat(),
        "total_jobs": len(jobs),
        "sources": sources,
        "jobs": [j.model_dump(mode="json") for j in jobs],
    }
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")


def export_csv(jobs: list[JobListing], path: Path) -> None:
    fields = [
        "title", "company", "location", "url", "source",
        "date_posted", "date_scraped", "job_type", "workplace_type",
        "department", "salary_text",
    ]
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        for job in jobs:
            row = job.model_dump(mode="json")
            writer.writerow({k: row.get(k, "") for k in fields})
