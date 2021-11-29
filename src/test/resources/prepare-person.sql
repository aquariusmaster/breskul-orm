CREATE TABLE IF NOT EXISTS PERSONS (
    id BIGINT NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO persons(first_name, last_name) VALUES ('Andrii', 'Bobrov');
INSERT INTO persons(first_name, last_name) VALUES ('Ivan', 'Petrov');
INSERT INTO persons(first_name, last_name) VALUES ( 'John', 'Doe');
