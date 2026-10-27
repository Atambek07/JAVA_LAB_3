# 🏛️ Library — Лабораторная работа № 3: REST API на Spring Boot

Проект **Library** — лабораторная работа № 3 по курсу Java.

> **Тема:** разработка REST API на Spring Boot 4.1.

## 📌 Краткое описание

Веб-приложение «Библиотека» предоставляет REST API для управления каталогом книг,
читателями и выдачами. Работа построена на **доменной модели лабораторной работы № 2**
(пакет `kg.course.library`), перенесённой в новый Spring Boot-проект.

## ⚙️ Требования к окружению

| Компонент | Версия |
|---|---|
| Java | **21** |
| Maven | 3.8.7+ |
| Spring Boot | **4.1.2** |
| springdoc (OpenAPI) | 3.1.1 |

## 🚀 Запуск приложения

```bash
# сборка и тесты
mvn clean verify

# запуск
mvn spring-boot:run
```

После запуска:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs
- REST API: http://localhost:8080/api/...
- Переменная загрузка книг: `library.import-file=books.csv`

## ⚒️ Команды Maven

```bash
# Сборка без тестов
mvn package -DskipTests

# Только тесты
mvn test

# Полная проверка: чистка + тесты + jar
mvn clean verify

# Отдельный тест
mvn test -Dtest=LibraryApiIntegrationTest
```

## 🗂️ Расположение документации

| Документ | Путь |
|---|---|
| 📘 **Полный гайд** | [`docs/LAB3_COMPLETE_GUIDE.md`](docs/LAB3_COMPLETE_GUIDE.md) |
| 📙 **Handoff для лабораторной № 4** | [`docs/LAB4_AGENT_HANDOFF.md`](docs/LAB4_AGENT_HANDOFF.md) |
| 🔌 **Примеры API** | [`docs/API_EXAMPLES.md`](docs/API_EXAMPLES.md) |
| 📑 **Гайд (PDF)** | [`docs/LAB3_COMPLETE_GUIDE.pdf`](docs/LAB3_COMPLETE_GUIDE.pdf) |

## 🔌 Основные эндпоинты

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/api/books` | список книг (с `?q=...` — поиск по названию) |
| `GET` | `/api/books/{isbn}` | книга по ISBN |
| `POST` | `/api/books` | добавление книги |
| `DELETE` | `/api/books/{isbn}` | удаление книги |
| `GET` | `/api/readers` | список читателей |
| `POST` | `/api/readers` | регистрация читателя |
| `GET` | `/api/loans` | выдачи (с `?overdue=true` — просроченные) |
| `GET` | `/api/loans/{id}` | выдача по номеру |
| `POST` | `/api/loans` | выдача книги |
| `POST` | `/api/loans/{id}/return` | возврат книги |
| `GET` | `/api/reports/top?n=5` | топ-N популярных книг |
| `GET` | `/api/reports/by-category` | выдачи по категориям |
| `GET` | `/api/reports/authors` | все авторы |
