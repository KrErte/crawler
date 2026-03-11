ALTER TABLE jobs ADD COLUMN skills JSONB DEFAULT '[]'::jsonb;
