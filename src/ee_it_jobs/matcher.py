"""CV parser and job matcher — extracts skills from PDF and scores against job listings."""
from __future__ import annotations

import re
from dataclasses import dataclass, field

import pdfplumber


# Common IT skills/keywords grouped by category
SKILL_ALIASES: dict[str, list[str]] = {
    "python": ["python"],
    "javascript": ["javascript", "js", "ecmascript"],
    "typescript": ["typescript", "ts"],
    "java": ["java"],
    "c#": ["c#", "csharp", "c sharp", ".net", "dotnet"],
    "c++": ["c++", "cpp"],
    "go": ["golang", "go language", "go developer", "go programming"],
    "rust": ["rust"],
    "ruby": ["ruby"],
    "php": ["php"],
    "swift": ["swift"],
    "kotlin": ["kotlin"],
    "scala": ["scala"],
    "r": ["r programming", "r language", "rstudio", "r-studio"],
    "sql": ["sql", "mysql", "postgresql", "postgres", "mssql", "oracle db"],
    "nosql": ["nosql", "mongodb", "mongo", "cassandra", "dynamodb", "couchdb"],
    "react": ["react", "reactjs", "react.js"],
    "angular": ["angular"],
    "vue": ["vue", "vuejs", "vue.js"],
    "svelte": ["svelte"],
    "next.js": ["next.js", "nextjs"],
    "node.js": ["node.js", "nodejs", "node"],
    "django": ["django"],
    "flask": ["flask"],
    "fastapi": ["fastapi"],
    "spring": ["spring", "spring boot", "springboot"],
    "rails": ["rails", "ruby on rails"],
    "express": ["express", "expressjs"],
    "docker": ["docker", "containerization"],
    "kubernetes": ["kubernetes", "k8s"],
    "aws": ["aws", "amazon web services"],
    "azure": ["azure", "microsoft azure"],
    "gcp": ["gcp", "google cloud"],
    "terraform": ["terraform"],
    "ansible": ["ansible"],
    "ci/cd": ["ci/cd", "cicd", "ci cd", "jenkins", "github actions", "gitlab ci"],
    "git": ["git", "github", "gitlab", "bitbucket"],
    "linux": ["linux", "ubuntu", "debian", "centos"],
    "devops": ["devops"],
    "machine learning": ["machine learning", "ml", "deep learning", "neural network"],
    "data science": ["data science", "data scientist"],
    "data engineering": ["data engineering", "data engineer", "etl", "data pipeline"],
    "ai": ["artificial intelligence", " ai ", "llm", "nlp", "computer vision"],
    "tensorflow": ["tensorflow"],
    "pytorch": ["pytorch"],
    "agile": ["agile", "scrum", "kanban"],
    "project management": ["project management", "project manager", "pm"],
    "product management": ["product management", "product manager", "product owner"],
    "qa": ["qa", "quality assurance", "testing", "test automation"],
    "selenium": ["selenium"],
    "cypress": ["cypress"],
    "security": ["security", "cybersecurity", "infosec", "penetration testing"],
    "networking": ["networking", "tcp/ip", "dns", "vpn"],
    "frontend": ["frontend", "front-end", "front end", "ui"],
    "backend": ["backend", "back-end", "back end"],
    "fullstack": ["fullstack", "full-stack", "full stack"],
    "mobile": ["mobile", "android", "ios", "react native", "flutter"],
    "redis": ["redis"],
    "kafka": ["kafka"],
    "rabbitmq": ["rabbitmq"],
    "elasticsearch": ["elasticsearch", "elastic"],
    "graphql": ["graphql"],
    "rest api": ["rest api", "restful", "rest"],
    "microservices": ["microservices", "micro-services"],
    "figma": ["figma"],
    "ux": ["ux design", "ux research", "user experience", "ux/ui"],
    "html/css": ["html", "css", "sass", "scss"],
    "power bi": ["power bi", "powerbi"],
    "tableau": ["tableau"],
    "excel": ["excel"],
    "jira": ["jira"],
    "confluence": ["confluence"],
}

# Seniority/role keywords
ROLE_KEYWORDS = [
    "senior", "junior", "lead", "principal", "staff", "head", "manager",
    "architect", "intern", "trainee", "mid-level", "entry-level",
]

# Experience patterns
EXP_PATTERN = re.compile(r"(\d+)\+?\s*(?:years?|aasta[t]?|a\.)\s*(?:of\s+)?(?:experience|kogemust?|töö)?", re.IGNORECASE)


@dataclass
class CVProfile:
    """Extracted profile from a CV."""
    raw_text: str
    skills: set[str] = field(default_factory=set)
    years_experience: int | None = None
    role_level: str | None = None
    all_keywords: set[str] = field(default_factory=set)


def parse_pdf(file_bytes: bytes) -> str:
    """Extract text from PDF bytes."""
    import io
    text_parts = []
    with pdfplumber.open(io.BytesIO(file_bytes)) as pdf:
        for page in pdf.pages:
            page_text = page.extract_text()
            if page_text:
                text_parts.append(page_text)
    return "\n".join(text_parts)


def extract_profile(text: str) -> CVProfile:
    """Extract skills, experience level, and keywords from CV text."""
    profile = CVProfile(raw_text=text)
    text_lower = text.lower()

    # Extract skills
    for skill_name, aliases in SKILL_ALIASES.items():
        for alias in aliases:
            if alias.strip() in text_lower:
                profile.skills.add(skill_name)
                break

    # Extract years of experience
    matches = EXP_PATTERN.findall(text)
    if matches:
        profile.years_experience = max(int(m) for m in matches)

    # Extract role level
    for role in ROLE_KEYWORDS:
        if role in text_lower:
            profile.role_level = role
            break

    # Collect all meaningful keywords (words 3+ chars, not common stopwords)
    stopwords = {
        "the", "and", "for", "with", "from", "that", "this", "have", "has",
        "are", "was", "were", "been", "being", "will", "would", "could",
        "should", "may", "can", "not", "but", "all", "also", "more", "about",
        "which", "their", "them", "than", "other", "into", "its", "over",
        "such", "our", "you", "your", "who", "how", "what", "when", "where",
        "each", "she", "his", "her", "him", "they", "some", "any", "only",
        "very", "just", "most", "own", "same", "both", "few", "new", "now",
        "way", "use", "used", "using", "work", "worked", "working",
    }
    words = set(re.findall(r"[a-z][a-z+#.]{2,}", text_lower))
    profile.all_keywords = words - stopwords

    return profile


def _count_job_skills(job_text: str) -> set[str]:
    """Count how many distinct skills are mentioned in a job text."""
    found = set()
    for skill_name, aliases in SKILL_ALIASES.items():
        for alias in aliases:
            if alias.strip() in job_text:
                found.add(skill_name)
                break
    return found


def score_job(profile: CVProfile, job: dict) -> int:
    """Score a job listing against a CV profile. Returns 0-100 percentage."""
    job_text = " ".join([
        job.get("title", ""),
        job.get("company", ""),
        job.get("department", "") or "",
        job.get("description_snippet", "") or "",
        job.get("location", ""),
    ]).lower()

    title_lower = job.get("title", "").lower()
    dept = (job.get("department") or "").lower()

    # --- 1. Skill match (45% weight) ---
    # Find skills the job mentions that are also in the CV
    job_skills = _count_job_skills(job_text)
    matched_skills = profile.skills & job_skills

    if job_skills:
        # "Do you have what the job needs?" — ratio of job's skills covered by CV
        coverage = len(matched_skills) / len(job_skills)
    else:
        coverage = 0.0

    # Absolute match bonus: more matched skills = better fit regardless
    # 1 match=0.4, 2=0.7, 3+=1.0
    abs_bonus = min(len(matched_skills) * 0.35, 1.0) if matched_skills else 0.0

    # Blend both signals
    skill_score = coverage * 0.6 + abs_bonus * 0.4

    # --- 2. Title/role relevance (25% weight) ---
    title_words = set(re.findall(r"[a-z][a-z+#.]{2,}", title_lower))
    title_overlap = len(profile.all_keywords & title_words)
    title_score = min(title_overlap / 2, 1.0)  # 2+ keyword hits = full score

    # --- 3. Seniority match (15% weight) ---
    seniority_score = 0.0
    if profile.role_level:
        if profile.role_level in title_lower:
            seniority_score = 1.0
        elif any(r in title_lower for r in ROLE_KEYWORDS):
            seniority_score = 0.3

    # --- 4. Department relevance (15% weight) ---
    dept_score = 0.0
    if dept:
        dept_overlap = sum(1 for kw in profile.all_keywords if kw in dept and len(kw) > 3)
        dept_score = min(dept_overlap / 2, 1.0)

    # Weighted total
    pct = (
        skill_score * 45
        + title_score * 25
        + seniority_score * 15
        + dept_score * 15
    )

    return min(round(pct), 100)


def match_jobs(profile: CVProfile, jobs: list[dict], top_n: int = 20) -> list[dict]:
    """Score and rank jobs against CV profile. Returns top N matches with percentage."""
    scored = []
    for job in jobs:
        pct = score_job(profile, job)
        if pct > 0:
            scored.append({
                **job,
                "match_pct": pct,
                "matched_skills": _get_matched_skills(profile, job),
            })

    scored.sort(key=lambda x: x["match_pct"], reverse=True)
    return scored[:top_n]


def _get_matched_skills(profile: CVProfile, job: dict) -> list[str]:
    """Return list of skills from profile that match this job."""
    job_text = " ".join([
        job.get("title", ""),
        job.get("department", "") or "",
        job.get("description_snippet", "") or "",
    ]).lower()

    matched = []
    for skill_name, aliases in SKILL_ALIASES.items():
        if skill_name not in profile.skills:
            continue
        for alias in aliases:
            if alias.strip() in job_text:
                matched.append(skill_name)
                break
    return matched
