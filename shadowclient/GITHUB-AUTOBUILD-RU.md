# Shadow Client — GitHub AutoBuild

Автоматическая сборка Fabric-мода для Minecraft 1.21.11 на GitHub Actions.

## Как пользоваться
1. Создай пустой репозиторий на GitHub.
2. Загрузи **содержимое папки ShadowClient** в корень репозитория (так, чтобы `build.gradle` и `gradle.properties` лежали в корне).
3. Открой вкладку **Actions** и разреши запуск workflows, если GitHub попросит.
4. Сделай push в ветку `main`/`master` или запусти workflow вручную через **Actions → Build Shadow Client → Run workflow**.
5. Открой завершившийся запуск и скачай **Shadow-Client-JAR** в разделе **Artifacts**.

## Важно
- Workflow использует Java 21 и Gradle 8.14.3.
- Исходный проект не содержит Gradle Wrapper, поэтому workflow устанавливает Gradle на runner и вызывает `gradle clean build`.
- Этот архив только настраивает автоматическую сборку; он не подтверждает, что исходники компилируются. Если сборка завершится ошибкой, открой лог шага **Build Fabric mod**.
