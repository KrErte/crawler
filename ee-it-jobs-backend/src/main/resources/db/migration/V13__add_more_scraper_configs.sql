-- SwissBorg on Lever (has Estonia/Tallinn jobs)
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('LEVER', 'SwissBorg', '{"company_slug":"swissborg","api_host":"api.lever.co"}');

-- Lendurai - drone tech startup in Tallinn (Teamtailor)
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('TEAMTAILOR', 'Lendurai', '{"base_url":"https://lendurai.teamtailor.com"}');

-- ESTO - payment solutions, Tallinn (Teamtailor, redirects from estonew.teamtailor.com)
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('TEAMTAILOR', 'ESTO', '{"base_url":"https://careers.esto.eu"}');

-- Upvest - fintech with Tallinn hub (Ashby)
INSERT INTO scraper_configs (scraper_type, company_name, config_json) VALUES
('ASHBY', 'Upvest', '{"board_name":"upvest"}');
