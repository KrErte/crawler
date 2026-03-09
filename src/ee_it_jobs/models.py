from __future__ import annotations

from datetime import date
from enum import StrEnum
from typing import Optional

from pydantic import BaseModel, Field, HttpUrl


class JobType(StrEnum):
    FULL_TIME = "full-time"
    PART_TIME = "part-time"
    CONTRACT = "contract"
    INTERNSHIP = "internship"
    UNKNOWN = "unknown"


class WorkplaceType(StrEnum):
    ONSITE = "onsite"
    REMOTE = "remote"
    HYBRID = "hybrid"
    UNKNOWN = "unknown"


class JobListing(BaseModel):
    title: str
    company: str
    location: str
    url: str
    source: str

    date_posted: Optional[date] = None
    date_scraped: date = Field(default_factory=date.today)
    job_type: JobType = JobType.UNKNOWN
    workplace_type: WorkplaceType = WorkplaceType.UNKNOWN
    department: Optional[str] = None
    salary_text: Optional[str] = None
    description_snippet: Optional[str] = None

    @property
    def dedup_key(self) -> str:
        return f"{self.company.lower().strip()}|{self.title.lower().strip()}|{self.location.lower().strip()}"


class ScrapeResult(BaseModel):
    source: str
    jobs: list[JobListing] = []
    errors: list[str] = []
    duration_seconds: float = 0.0
    pages_scraped: int = 0


class UserProfile(BaseModel):
    first_name: str = ""
    last_name: str = ""
    email: str = ""
    phone: str = ""
    linkedin_url: str = ""
    cover_letter: str = ""
