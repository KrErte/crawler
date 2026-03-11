ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS notification_preferences JSONB DEFAULT '{}';

COMMENT ON COLUMN user_profiles.notification_preferences IS 'User notification preferences: jobTypes, workplaceTypes, minSalary, skills';
