-- EU27 standard VAT rates as of 2025-09 with scheduled future changes.
-- rate_basis_points is integer (1900 = 19.00 percent). Source: EU Commission VAT rates database.

INSERT INTO vat_rates (country_code, rate_type, rate_basis_points, effective_from, effective_to, source_note) VALUES
('AT', 'STANDARD', 2000, '2020-01-01', NULL, 'Austria 20%'),
('BE', 'STANDARD', 2100, '1996-01-01', NULL, 'Belgium 21%'),
('BG', 'STANDARD', 2000, '1999-01-01', NULL, 'Bulgaria 20%'),
('CY', 'STANDARD', 1900, '2014-01-13', NULL, 'Cyprus 19%'),
('CZ', 'STANDARD', 2100, '2013-01-01', NULL, 'Czechia 21%'),
('DE', 'STANDARD', 1900, '2007-01-01', NULL, 'Germany 19%'),
('DK', 'STANDARD', 2500, '1992-01-01', NULL, 'Denmark 25%'),
('EE', 'STANDARD', 2200, '2024-01-01', NULL, 'Estonia 22% from 2024'),
('ES', 'STANDARD', 2100, '2012-09-01', NULL, 'Spain 21%'),
('FI', 'STANDARD', 2550, '2024-09-01', '2025-12-31', 'Finland 25.5% from 2024-09'),
('FI', 'STANDARD', 2550, '2026-01-01', NULL, 'Finland 25.5% continues into 2026 unless amended'),
('FR', 'STANDARD', 2000, '2014-01-01', NULL, 'France 20%'),
('GR', 'STANDARD', 2400, '2016-06-01', NULL, 'Greece 24%'),
('HR', 'STANDARD', 2500, '2013-01-01', NULL, 'Croatia 25%'),
('HU', 'STANDARD', 2700, '2012-01-01', NULL, 'Hungary 27% (highest in EU)'),
('IE', 'STANDARD', 2300, '2012-01-01', NULL, 'Ireland 23%'),
('IT', 'STANDARD', 2200, '2013-10-01', NULL, 'Italy 22%'),
('LT', 'STANDARD', 2100, '2009-09-01', '2025-12-31', 'Lithuania 21%'),
('LT', 'STANDARD', 2100, '2026-01-01', NULL, 'Lithuania remains 21% standard; reduced 9->12 scheduled for 2026'),
('LU', 'STANDARD', 1700, '2024-01-01', NULL, 'Luxembourg 17% (lowest in EU)'),
('LV', 'STANDARD', 2100, '2009-01-01', NULL, 'Latvia 21%'),
('MT', 'STANDARD', 1800, '2004-01-01', NULL, 'Malta 18%'),
('NL', 'STANDARD', 2100, '2012-10-01', NULL, 'Netherlands 21%'),
('PL', 'STANDARD', 2300, '2011-01-01', NULL, 'Poland 23%'),
('PT', 'STANDARD', 2300, '2011-01-01', NULL, 'Portugal 23%'),
('RO', 'STANDARD', 1900, '2017-01-01', NULL, 'Romania 19%'),
('SE', 'STANDARD', 2500, '1993-01-01', NULL, 'Sweden 25%'),
('SI', 'STANDARD', 2200, '2013-07-01', NULL, 'Slovenia 22%'),
('SK', 'STANDARD', 2300, '2025-01-01', NULL, 'Slovakia 23% from 2025');

-- 2026 scheduled reduced-rate changes
INSERT INTO vat_rates (country_code, rate_type, rate_basis_points, effective_from, effective_to, source_note) VALUES
('FI', 'REDUCED', 1350, '2026-01-01', NULL, 'Finland reduced rate 14->13.5 from 2026'),
('LT', 'REDUCED', 1200, '2026-01-01', NULL, 'Lithuania reduced rate 9->12 from 2026');
