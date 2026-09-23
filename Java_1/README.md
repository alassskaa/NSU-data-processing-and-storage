# Java Key Generation Server

Микросервис генерирует RSA-ключи и X.509-сертификаты для клиентов по их имени.

## Запуск

### 1. Создать ключ подписи сервера

```bash
./gradlew runSigningKeyGenerator --args="signing.key"
```

Выполняется один раз.

### 2. Запустить сервер

```bash
/gradlew runServer --args="9000 4 signing.key MyServer"
```

Параметры:

- 9000 — порт;
- 4 — количество потоков генерации;
- signing.key — ключ подписи сервера;
- MyServer — имя сервера в сертификатах.

### 3. Запустить клиента

```bash
./gradlew runClient --args="Alice localhost 9000"
```

Результат:

- Alice.key — приватный ключ;
- Alice.crt — сертификат.

## Дополнительные параметры клиента

Задержка перед чтением ответа:

```bash
./gradlew runClient --args="Alice localhost 9000 --delay 30"
```

Имитация завершения клиента до получения ответа:

```bash
./gradlew runClient --args="Alice localhost 9000 --crash"
```

Повторный запрос с тем же именем возвращает ранее сгенерированные ключи и сертификат.