# Защищённый Task Manager

Установите JDK 22. В переменных среды Windows задайте `JAVA_HOME` — папку JDK, а в `Path` добавьте `%JAVA_HOME%\bin`.
Затем откройте PowerShell в папке проекта. Maven загрузится автоматически; первый запуск требует интернета.

Установка зависимостей и сборка:

```powershell
.\mvnw.cmd package
```

Запуск тестов:

```powershell
.\mvnw.cmd test
```

Запуск:

```powershell
java -jar .\target\11-security-api-1.0-SNAPSHOT.jar --spring.profiles.active=demo
```

Настройки уже заполнены в `.env` и загружаются автоматически. Посмотреть пароль администратора можно командой `notepad .env`.

Открыть [Swagger UI](http://localhost:8080/swagger-ui.html). Для входа через `/auth/login`: email `admin@example.com`, пароль — значение `DEMO_ADMIN_PASSWORD` из файла `.env`. Полученный `accessToken` вставить в поле `Authorize`.

Остановка — `Ctrl+C`.
