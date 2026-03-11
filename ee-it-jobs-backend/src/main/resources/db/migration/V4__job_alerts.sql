ALTER TABLE user_profiles ADD COLUMN email_alerts BOOLEAN DEFAULT false;
ALTER TABLE user_profiles ADD COLUMN alert_threshold INTEGER DEFAULT 70;
ALTER TABLE user_profiles ADD COLUMN last_alert_sent_at TIMESTAMP;
