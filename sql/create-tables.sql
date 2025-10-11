-- Создание таблиц для приложения is-lab1

-- Удаляем таблицы если существуют (в правильном порядке из-за внешних ключей)
DROP TABLE IF EXISTS flats CASCADE;
DROP TABLE IF EXISTS houses CASCADE;
DROP TABLE IF EXISTS coordinates CASCADE;

-- Создаем таблицу coordinates
CREATE TABLE coordinates (
    id BIGSERIAL PRIMARY KEY,
    x BIGINT NOT NULL,
    y REAL NOT NULL
);

-- Создаем таблицу houses
CREATE TABLE houses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    year INTEGER NOT NULL CHECK (year > 0),
    number_of_flats_on_floor INTEGER NOT NULL CHECK (number_of_flats_on_floor > 0)
);

-- Создаем таблицу flats
CREATE TABLE flats (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    coordinates_id BIGINT NOT NULL REFERENCES coordinates(id),
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    area BIGINT NOT NULL CHECK (area > 0),
    price BIGINT NOT NULL CHECK (price > 0),
    balcony BOOLEAN,
    time_to_metro_on_foot BIGINT NOT NULL CHECK (time_to_metro_on_foot > 0),
    number_of_rooms INTEGER NOT NULL CHECK (number_of_rooms > 0),
    furnish VARCHAR(255) NOT NULL,
    view VARCHAR(255) NOT NULL,
    living_space BIGINT NOT NULL CHECK (living_space > 0),
    house_id BIGINT REFERENCES houses(id)
);

-- Создаем индексы для улучшения производительности
CREATE INDEX idx_flats_coordinates ON flats(coordinates_id);
CREATE INDEX idx_flats_house ON flats(house_id);
CREATE INDEX idx_flats_name ON flats(name);
CREATE INDEX idx_houses_name ON houses(name);

-- Вставляем тестовые данные
INSERT INTO coordinates (x, y) VALUES (100, 200.5);
INSERT INTO coordinates (x, y) VALUES (150, 300.7);

INSERT INTO houses (name, year, number_of_flats_on_floor) VALUES ('Дом на Невском', 2020, 4);
INSERT INTO houses (name, year, number_of_flats_on_floor) VALUES ('ЖК Северный', 2019, 6);

INSERT INTO flats (name, coordinates_id, area, price, balcony, time_to_metro_on_foot, number_of_rooms, furnish, view, living_space, house_id)
VALUES ('Квартира 1', 1, 50, 5000000, true, 10, 2, 'DESIGNER', 'YARD', 35, 1);

INSERT INTO flats (name, coordinates_id, area, price, balcony, time_to_metro_on_foot, number_of_rooms, furnish, view, living_space, house_id)
VALUES ('Квартира 2', 2, 70, 7000000, false, 15, 3, 'FINE', 'STREET', 50, 2);
