DROP TABLE IF EXISTS PERSONS;
CREATE TABLE PERSONS (
    id BIGINT NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

DROP SEQUENCE IF EXISTS orm_sequence;
CREATE SEQUENCE orm_sequence start with 4;

INSERT INTO persons(id, first_name, last_name) VALUES (1, 'Andrii', 'Bobrov');
INSERT INTO persons(id, first_name, last_name) VALUES (2, 'Ivan', 'Petrov');
INSERT INTO persons(id, first_name, last_name) VALUES (3, 'John', 'Doe');

DROP TABLE IF EXISTS ADDRESS;
CREATE TABLE ADDRESS (
    id BIGINT NOT NULL AUTO_INCREMENT,
    address_line VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);