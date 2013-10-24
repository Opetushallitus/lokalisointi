
    create table localisation (
        id int8 not null unique,
        version int8 not null,
        accessed timestamp not null,
        xcategory varchar(32) not null,
        created timestamp not null,
        createdBy varchar(255),
        description text,
        xkey varchar(512) not null,
        xlanguage varchar(32) not null,
        modified timestamp not null,
        modifiedBy varchar(255),
        xvalue text,
        primary key (id),
        unique (xcategory, xlanguage, xkey)
    );

    create sequence hibernate_sequence;
