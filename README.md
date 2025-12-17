# Система Антиплагиата

## Описание
Микросервисная система для проверки студенческих работ на плагиат. Система принимает работы от студентов, анализирует их и формирует отчеты о заимствованиях.

## Архитектура системы

Система состоит из трех микросервисов:

### 1. API Gateway (порт 8080)
**Ответственность:** Центральная точка входа для всех клиентских запросов. Маршрутизирует запросы к соответствующим микросервисам.

**Endpoints:**
- `POST /api/works` - Отправка работы на проверку
- `GET /api/works/{workId}/reports` - Получение отчетов по работе

### 2. File Storing Service (порт 8081)
**Ответственность:** Хранение и выдача файлов работ.

**Endpoints:**
- `POST /files` - Сохранение файла
- `GET /files/{fileId}` - Получение файла
- `GET /files/{fileId}/content` - Получение содержимого файла

**База данных:** PostgreSQL (таблица `files`)

### 3. File Analysis Service (порт 8082)
**Ответственность:** Анализ работ на плагиат, создание и хранение отчетов.

**Endpoints:**
- `POST /analysis` - Запуск анализа работы
- `GET /analysis/reports/{workId}` - Получение всех отчетов по работе
- `GET /analysis/reports/work/{reportId}` - Получение конкретного отчета

**База данных:** PostgreSQL (таблицы `works`, `reports`)

## Алгоритм определения плагиата

Система использует следующий алгоритм для определения признаков плагиата:

### 1. Сравнение содержимого файлов
- Извлекается текстовое содержимое загруженной работы
- Содержимое сравнивается со всеми ранее сданными работами по тому же заданию

### 2. Критерии определения плагиата
Плагиат **обнаружен**, если выполняется хотя бы одно из условий:

**a) Точное совпадение (100% плагиат)**
- Содержимое файлов идентично побайтово
- Или совпадение текста после нормализации (удаление пробелов, приведение к нижнему регистру) >= 95%

**b) Высокое совпадение (плагиат)**
- Совпадение > 80% после нормализации текста
- И работа была сдана позже другой работы с таким совпадением

**c) Подозрительное совпадение (требует проверки)**
- Совпадение 50-80% текста
- Помечается флагом для ручной проверки преподавателем

### 3. Расчет процента совпадения
Используется алгоритм **Longest Common Subsequence (LCS)**:
```
similarity = (2 * LCS_length) / (length_text1 + length_text2) * 100
```

### 4. Формирование отчета
Для каждой работы создается отчет, содержащий:
- Процент оригинальности (100% - max_similarity)
- Список подозрительных совпадений с указанием:
  - ID работы, с которой обнаружено совпадение
  - Автор той работы
  - Процент совпадения
  - Дата сдачи работы-источника
- Финальный вердикт: `ORIGINAL`, `SUSPICIOUS`, `PLAGIARISM`

## Взаимодействие сервисов

### Сценарий 1: Отправка работы на проверку

```
Клиент → API Gateway → File Storing Service → API Gateway → File Analysis Service
```

**Последовательность:**
1. Студент отправляет POST запрос с файлом работы в API Gateway
2. API Gateway перенаправляет файл в File Storing Service
3. File Storing Service сохраняет файл в БД и на диске, возвращает fileId
4. API Gateway получает fileId и отправляет запрос в File Analysis Service для создания записи о работе
5. File Analysis Service:
   - Создает запись о работе в БД
   - Запрашивает содержимое файла из File Storing Service
   - Запускает анализ (сравнивает с предыдущими работами по этому заданию)
   - Создает отчет с результатами
6. API Gateway возвращает клиенту информацию о созданной работе и начатом анализе

### Сценарий 2: Получение отчетов

```
Клиент → API Gateway → File Analysis Service → API Gateway → Клиент
```

**Последовательность:**
1. Преподаватель отправляет GET запрос за отчетами по работе
2. API Gateway перенаправляет запрос в File Analysis Service
3. File Analysis Service извлекает отчеты из БД
4. API Gateway возвращает JSON с отчетами клиенту

## Обработка ошибок

### Недоступность File Storing Service
- API Gateway возвращает 503 Service Unavailable
- Повторные попытки с exponential backoff (3 попытки)
- Логирование ошибки

### Недоступность File Analysis Service  
- API Gateway возвращает 503 Service Unavailable
- Если сервис упал во время анализа - создается отчет со статусом FAILED
- При восстановлении можно запустить повторный анализ

### Ошибки валидации
- 400 Bad Request для невалидных данных (пустой файл, неверный формат)
- 404 Not Found для несуществующих работ/отчетов

## Модели данных

### File Storing Service - таблица `files`
```sql
CREATE TABLE files (
    id UUID PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100),
    file_size BIGINT,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### File Analysis Service - таблица `works`
```sql
CREATE TABLE works (
    id UUID PRIMARY KEY,
    student_name VARCHAR(255) NOT NULL,
    assignment_id VARCHAR(100) NOT NULL,
    file_id UUID NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### File Analysis Service - таблица `reports`
```sql
CREATE TABLE reports (
    id UUID PRIMARY KEY,
    work_id UUID NOT NULL REFERENCES works(id),
    status VARCHAR(50) NOT NULL, -- PENDING, COMPLETED, FAILED
    plagiarism_detected BOOLEAN DEFAULT FALSE,
    originality_percent DECIMAL(5,2),
    verdict VARCHAR(50), -- ORIGINAL, SUSPICIOUS, PLAGIARISM
    details TEXT, -- JSON с деталями совпадений
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
```

## Технологический стек

- **Java:** 17+
- **Framework:** Spring Boot 3.x
- **База данных:** PostgreSQL 15
- **Контейнеризация:** Docker, Docker Compose
- **HTTP клиент:** RestTemplate / WebClient
- **Build tool:** Maven

## Запуск системы

### Предварительные требования
- Docker
- Docker Compose

### Запуск
```bash
docker compose up --build
```

Система будет доступна на:
- API Gateway: http://localhost:8080
- File Storing Service: http://localhost:8081
- File Analysis Service: http://localhost:8082

### Остановка
```bash
docker compose down
```

### Остановка с удалением данных
```bash
docker compose down -v
```

## Примеры использования

### Отправка работы
```bash
curl -X POST http://localhost:8080/api/works \
  -F "file=@work.txt" \
  -F "studentName=Иван Иванов" \
  -F "assignmentId=homework-1"
```

### Получение отчетов
```bash
curl http://localhost:8080/api/works/{workId}/reports
```

## Swagger / Postman

Коллекция Postman доступна в файле `postman_collection.json` в корне проекта.

Swagger UI доступен по адресу: http://localhost:8080/swagger-ui.html

## Разработчик

Федор, Papa ML Team
