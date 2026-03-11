-- Job portals
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('CV_EE', 'CV.ee', '{}'),
('CV_KESKUS', 'CVKeskus', '{}'),
('KANDIDEERI', 'Kandideeri', '{}');

-- Lever ATS
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('LEVER', 'Pipedrive', '{"company_slug":"pipedrive","api_host":"api.lever.co"}'),
('LEVER', 'SEB', '{"company_slug":"seb","api_host":"api.eu.lever.co"}'),
('LEVER', 'CoinsPaid', '{"company_slug":"coinspaid","api_host":"api.eu.lever.co"}'),
('LEVER', 'Quadcode', '{"company_slug":"quadcode","api_host":"api.eu.lever.co"}');

-- Greenhouse ATS
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('GREENHOUSE', 'Veriff', '{"board_token":"veriff"}'),
('GREENHOUSE', 'Bondora', '{"board_token":"bondora"}'),
('GREENHOUSE', 'Gelato', '{"board_token":"gelato"}'),
('GREENHOUSE', 'Bolt', '{"board_token":"boltv2"}'),
('GREENHOUSE', 'Testlio', '{"board_token":"testlio"}');

-- SmartRecruiters ATS
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('SMART_RECRUITERS', 'Playtech', '{"company_id":"Playtech"}'),
('SMART_RECRUITERS', 'Wise', '{"company_id":"Wise"}'),
('SMART_RECRUITERS', 'Proekspert', '{"company_id":"Proekspert"}');

-- Workable ATS
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('WORKABLE', 'Skeleton Technologies', '{"account_slug":"skeletontech"}');

-- Teamtailor ATS
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('TEAMTAILOR', 'Starship Technologies', '{"base_url":"https://starship.teamtailor.com"}'),
('TEAMTAILOR', 'Luminor', '{"base_url":"https://luminorbank.teamtailor.com"}'),
('TEAMTAILOR', 'Swedbank', '{"base_url":"https://jobs.swedbank.com"}'),
('TEAMTAILOR', 'Thorgate', '{"base_url":"https://thorgate.teamtailor.com"}');

-- Custom scrapers
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('NORTAL', 'Nortal', '{}'),
('HELMES', 'Helmes', '{}');
