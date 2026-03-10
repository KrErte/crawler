"""Scraper registry — all available scrapers and their configurations."""

from ee_it_jobs.scrapers.cvee import CvEeScraper
from ee_it_jobs.scrapers.cvkeskus import CvKeskusScraper
from ee_it_jobs.scrapers.tootukassa import TootukassaScraper
from ee_it_jobs.scrapers.lever import LeverScraper
from ee_it_jobs.scrapers.greenhouse import GreenhouseScraper
from ee_it_jobs.scrapers.smartrecruiters import SmartRecruitersScraper
from ee_it_jobs.scrapers.nortal import NortalScraper
from ee_it_jobs.scrapers.helmes import HelmesScraper
from ee_it_jobs.scrapers.workable import WorkableScraper
from ee_it_jobs.scrapers.teamtailor import TeamtailorScraper

# (scraper_class, kwargs for __init__)
SCRAPER_CONFIGS: list[tuple[type, dict]] = [
    # Job portals
    (CvEeScraper, {}),
    (CvKeskusScraper, {}),
    (TootukassaScraper, {}),

    # Lever ATS
    (LeverScraper, {"company_slug": "pipedrive", "company_name": "Pipedrive"}),
    (LeverScraper, {"company_slug": "seb", "company_name": "SEB", "api_host": "api.eu.lever.co"}),

    # Greenhouse ATS
    (GreenhouseScraper, {"board_token": "veriff", "company_name": "Veriff"}),
    (GreenhouseScraper, {"board_token": "bondora", "company_name": "Bondora"}),
    (GreenhouseScraper, {"board_token": "gelato", "company_name": "Gelato"}),
    (GreenhouseScraper, {"board_token": "boltv2", "company_name": "Bolt"}),
    (GreenhouseScraper, {"board_token": "testlio", "company_name": "Testlio"}),

    # SmartRecruiters ATS
    (SmartRecruitersScraper, {"company_id": "Playtech", "company_name": "Playtech"}),
    (SmartRecruitersScraper, {"company_id": "Wise", "company_name": "Wise"}),
    (SmartRecruitersScraper, {"company_id": "Proekspert", "company_name": "Proekspert"}),

    # Workable ATS
    (WorkableScraper, {"account_slug": "skeletontech", "company_name": "Skeleton Technologies"}),

    # Teamtailor ATS
    (TeamtailorScraper, {"base_url": "https://starship.teamtailor.com", "company_name": "Starship Technologies"}),
    (TeamtailorScraper, {"base_url": "https://luminorbank.teamtailor.com", "company_name": "Luminor"}),
    (TeamtailorScraper, {"base_url": "https://jobs.swedbank.com", "company_name": "Swedbank"}),

    # Custom
    (NortalScraper, {}),
    (HelmesScraper, {}),
]
