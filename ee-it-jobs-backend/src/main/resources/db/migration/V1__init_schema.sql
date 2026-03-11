CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    linkedin_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_admin BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    cover_letter TEXT,
    cv_file_path VARCHAR(500),
    skills JSONB DEFAULT '[]'::jsonb,
    preferences JSONB DEFAULT '{}'::jsonb,
    cv_raw_text TEXT,
    years_experience INTEGER,
    role_level VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE scrape_runs (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    status VARCHAR(50),
    total_jobs INTEGER DEFAULT 0,
    total_new_jobs INTEGER DEFAULT 0,
    total_errors INTEGER DEFAULT 0,
    source_stats JSONB DEFAULT '{}'::jsonb,
    triggered_by VARCHAR(100)
);

CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    company VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    url VARCHAR(1000) NOT NULL,
    source VARCHAR(100) NOT NULL,
    date_posted DATE,
    date_scraped DATE,
    job_type VARCHAR(50) DEFAULT 'UNKNOWN',
    workplace_type VARCHAR(50) DEFAULT 'UNKNOWN',
    department VARCHAR(255),
    salary_text VARCHAR(500),
    description_snippet TEXT,
    full_description TEXT,
    dedup_key VARCHAR(1000),
    scrape_run_id BIGINT REFERENCES scrape_runs(id),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED'
        CHECK (status IN ('SUBMITTED','INTERVIEW','OFFER','REJECTED','GHOSTED')),
    notes TEXT,
    applied_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(user_id, job_id)
);

CREATE TABLE scraper_configs (
    id BIGSERIAL PRIMARY KEY,
    scraper_type VARCHAR(100) NOT NULL,
    company_name VARCHAR(255),
    config_json JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT true,
    requires_browser BOOLEAN NOT NULL DEFAULT false
);

-- Indexes
CREATE INDEX idx_jobs_company ON jobs(company);
CREATE INDEX idx_jobs_source ON jobs(source);
CREATE INDEX idx_jobs_dedup_key ON jobs(dedup_key);
CREATE INDEX idx_jobs_is_active ON jobs(is_active);
CREATE INDEX idx_jobs_date_scraped ON jobs(date_scraped);
CREATE INDEX idx_applications_user_id ON applications(user_id);
CREATE INDEX idx_applications_status ON applications(status);
