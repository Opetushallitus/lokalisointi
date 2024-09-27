drop table if exists localisation;

create table if not exists localisation_override (
    id serial not null,
    namespace text not null,
    localisation_key text not null,
    locale text not null,
    localisation_value text not null,
    created_at timestamp with time zone not null default current_timestamp,
    created_by text not null
);

create unique index if not exists i_localization_uniq_check on localisation_override (namespace, localisation_key, locale);
