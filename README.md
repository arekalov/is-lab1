# Информационные системы
## Лабораторная работа #1

### Задание
Разработать REST API по варианту, реализующий доступ к базе данных.

Вариант: Квартиры и дома

### Технологический стек
- Java 17
- Jakarta EE 10
- WildFly 26.1.3
- PostgreSQL (Neon DB)
- Gradle

### API Endpoints

#### Квартиры (Flats)

`GET /api/flats` - получение списка всех квартир с пагинацией
```
Параметры:
- page (integer) - номер страницы
- size (integer) - размер страницы

Пример: GET /api/flats?page=0&size=10
```

`GET /api/flats/{id}` - получение квартиры по ID
```
Пример: GET /api/flats/1
```

`POST /api/flats` - создание новой квартиры
```
Body:
{
    "name": "Квартира 1",
    "coordinates": {
        "x": 1.0,
        "y": 2.0
    },
    "area": 100,
    "numberOfRooms": 3,
    "furnish": "DESIGNER",
    "view": "YARD",
    "house": {
        "name": "Дом 1",
        "year": 2020,
        "numberOfFloors": 5
    }
}
```

`PUT /api/flats/{id}` - обновление квартиры
```
Пример: PUT /api/flats/1
Body: аналогично POST
```

`DELETE /api/flats/{id}` - удаление квартиры
```
Пример: DELETE /api/flats/1
```

#### Дома (Houses)

`GET /api/houses` - получение списка всех домов
```
Пример: GET /api/houses
```

`GET /api/houses/{id}` - получение дома по ID
```
Пример: GET /api/houses/1
```

`POST /api/houses` - создание нового дома
```
Body:
{
    "name": "Дом 1",
    "year": 2020,
    "numberOfFloors": 5
}
```

`PUT /api/houses/{id}` - обновление дома
```
Пример: PUT /api/houses/1
Body: аналогично POST
```

`DELETE /api/houses/{id}` - удаление дома
```
Пример: DELETE /api/houses/1
```