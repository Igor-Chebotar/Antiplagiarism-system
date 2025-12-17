# Инструкция по запуску и тестированию системы Антиплагиата

## Быстрый старт

### 1. Запуск системы

```bash
cd antiplagiarism-system
docker compose up --build
```

Дождитесь, пока все сервисы запустятся. Вы увидите логи от всех контейнеров.

### 2. Проверка работоспособности

Откройте в браузере или выполните curl запросы:

```bash
# Проверка API Gateway
curl http://localhost:8080/api/health

# Проверка File Storing Service
curl http://localhost:8081/files/health

# Проверка File Analysis Service
curl http://localhost:8082/analysis/health
```

Все должны вернуть сообщение о том, что сервис работает.

### 3. Swagger UI

Откройте в браузере:
- API Gateway: http://localhost:8080/swagger-ui.html
- File Storing Service: http://localhost:8081/swagger-ui.html
- File Analysis Service: http://localhost:8082/swagger-ui.html

## Тестирование через curl

### Сценарий 1: Отправка первой работы (оригинальная)

```bash
curl -X POST http://localhost:8080/api/works \
  -F "file=@test_work1.txt" \
  -F "studentName=Иван Иванов" \
  -F "assignmentId=homework-1"
```

Сохраните полученный `workId`.

### Сценарий 2: Получение отчета

```bash
curl http://localhost:8080/api/works/{WORK_ID}/reports
```

Вы должны увидеть отчет со статусом `COMPLETED`, `verdict: ORIGINAL` и `plagiarismDetected: false`.

### Сценарий 3: Отправка похожей работы (плагиат)

```bash
curl -X POST http://localhost:8080/api/works \
  -F "file=@test_work2_plagiarism.txt" \
  -F "studentName=Петр Петров" \
  -F "assignmentId=homework-1"
```

Сохраните новый `workId`.

### Сценарий 4: Проверка обнаружения плагиата

```bash
curl http://localhost:8080/api/works/{NEW_WORK_ID}/reports
```

Отчет должен показать:
- `plagiarismDetected: true`
- `verdict: PLAGIARISM` (если совпадение > 80%)
- `originalityPercent`: низкий процент (< 20%)
- В `details` должна быть информация о совпадении с первой работой

### Сценарий 5: Отправка оригинальной работы

```bash
curl -X POST http://localhost:8080/api/works \
  -F "file=@test_work3_original.txt" \
  -F "studentName=Мария Сидорова" \
  -F "assignmentId=homework-1"
```

Эта работа должна пройти проверку как оригинальная.

## Тестирование через Postman

1. Импортируйте файл `postman_collection.json` в Postman
2. Используйте коллекцию "Antiplagiarism System API"
3. В запросе "Submit Work" укажите путь к тестовым файлам
4. Скопируйте полученный `workId` и используйте его в запросе "Get Reports by Work ID"

## Проверка базы данных

### Подключение к PostgreSQL (File Storing Service)

```bash
docker exec -it postgres-files psql -U postgres -d filestorage
```

```sql
SELECT * FROM files;
\q
```

### Подключение к PostgreSQL (File Analysis Service)

```bash
docker exec -it postgres-analysis psql -U postgres -d analysis
```

```sql
SELECT * FROM works;
SELECT * FROM reports;
\q
```

## Просмотр логов

```bash
# Логи всех сервисов
docker compose logs -f

# Логи конкретного сервиса
docker compose logs -f api-gateway
docker compose logs -f file-storing-service
docker compose logs -f file-analysis-service
```

## Остановка системы

```bash
# Остановка с сохранением данных
docker compose down

# Остановка с удалением данных
docker compose down -v
```

## Проверка работы алгоритма плагиата

Алгоритм работает следующим образом:

1. **Нормализация текста**: приведение к нижнему регистру, удаление лишних пробелов
2. **Расчет LCS (Longest Common Subsequence)** между текущей работой и всеми предыдущими
3. **Формула similarity**: `(2 * LCS_length) / (length1 + length2) * 100`
4. **Вердикты**:
   - similarity >= 80% → PLAGIARISM
   - similarity 50-80% → SUSPICIOUS  
   - similarity < 50% → ORIGINAL

## Тестовые файлы

В проекте есть 3 тестовых файла:

1. `test_work1.txt` - оригинальная работа о технологиях
2. `test_work2_plagiarism.txt` - почти идентичная работа (плагиат)
3. `test_work3_original.txt` - оригинальная работа на другую тему

Сценарий тестирования:
1. Отправьте work1 → получите ORIGINAL
2. Отправьте work2 → получите PLAGIARISM (совпадение с work1)
3. Отправьте work3 → получите ORIGINAL (не похожа на предыдущие)

## Обработка ошибок

### Сервис недоступен

Попробуйте остановить один из сервисов:

```bash
docker stop file-storing-service
```

Затем попробуйте отправить работу через API Gateway. Вы должны получить ошибку `503 Service Unavailable`.

Перезапустите сервис:

```bash
docker start file-storing-service
```

## Troubleshooting

### Порты заняты

Если порты 8080, 8081, 8082 заняты, измените их в `docker-compose.yml`:

```yaml
ports:
  - "9080:8080"  # вместо 8080:8080
```

### База данных не инициализируется

Удалите volumes и пересоздайте:

```bash
docker compose down -v
docker compose up --build
```

### Сервис не может подключиться к другому сервису

Проверьте логи:

```bash
docker compose logs -f
```

Убедитесь, что все сервисы в одной сети `antiplagiarism-network`.

## Дополнительные возможности

### Прямое обращение к микросервисам

Вы можете обращаться напрямую к микросервисам, минуя API Gateway:

```bash
# Загрузка файла напрямую в File Storing Service
curl -X POST http://localhost:8081/files \
  -F "file=@test_work1.txt"

# Создание работы напрямую в File Analysis Service
curl -X POST http://localhost:8082/analysis \
  -H "Content-Type: application/json" \
  -d '{
    "fileId": "ваш-file-id",
    "studentName": "Тестовый студент",
    "assignmentId": "homework-1"
  }'
```

Это полезно для тестирования отдельных компонентов системы.
