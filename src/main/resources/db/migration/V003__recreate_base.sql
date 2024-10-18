DROP TABLE IF EXISTS localisation;

CREATE TABLE IF NOT EXISTS localisation_override (
    id SERIAL NOT NULL,
    namespace VARCHAR(100) NOT NULL,
    localisation_key VARCHAR(200) NOT NULL,
    locale CHAR(2) NOT NULL,
    localisation_value TEXT NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by TEXT NOT NULL,
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS localisation_unique_key_check ON localisation_override (namespace, localisation_key, locale);
