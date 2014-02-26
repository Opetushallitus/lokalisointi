--
-- Add access counter to localisation
--
alter table localisation add column accesscount int default(0);
