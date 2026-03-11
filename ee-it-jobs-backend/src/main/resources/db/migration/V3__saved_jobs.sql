CREATE TABLE saved_jobs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    saved_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(user_id, job_id)
);
CREATE INDEX idx_saved_jobs_user_id ON saved_jobs(user_id);
