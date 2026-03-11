-- New job portal scrapers
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('MEETFRANK', 'MeetFrank', '{}'),
('INDEED', 'Indeed', '{}'),
('TOOTUKASSA', 'Töötukassa', '{}');

-- Additional Teamtailor companies
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('TEAMTAILOR', 'Telia', '{"base_url":"https://telia.teamtailor.com"}'),
('TEAMTAILOR', 'Elisa', '{"base_url":"https://elisa.teamtailor.com"}'),
('TEAMTAILOR', 'LHV', '{"base_url":"https://lhv.teamtailor.com"}');

-- Additional Greenhouse companies
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('GREENHOUSE', 'Monese', '{"board_token":"monese"}'),
('GREENHOUSE', 'Glia', '{"board_token":"gaborgroszio"}');
