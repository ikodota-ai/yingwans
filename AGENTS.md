# Repository Guidelines

This repository hosts a RuoYi (若依) admin system: a Java 17 / Spring Boot Maven backend paired with a Vue 2 (Element UI) frontend.

## Project Structure & Module Organization
- `ruoyi-admin/` — application entry, web controllers, and runtime config (`src/main/resources`).
- `ruoyi-framework/` — core framework, security, and web configuration.
- `ruoyi-system/` — business domain, services, and MyBatis mappers.
- `ruoyi-common/` — shared utils, annotations, enums, and constants.
- `ruoyi-quartz/` — scheduled tasks; `ruoyi-generator/` — code generation.
- `ruoyi-ui/` — Vue frontend (`src/api`, `src/views`, `src/components`, `src/store`).
- `sql/` — database schemas; `doc/`, `bin/` — docs and helper scripts.

## Build, Test, and Development Commands
- `mvn clean package` — build all backend modules and produce `ruoyi-admin.jar`.
- `mvn spring-boot:run -pl ruoyi-admin` — run the backend locally (port 8080).
- `./ry.sh start|stop|restart|status` — manage the packaged jar.
- `cd ruoyi-ui && npm install` — install frontend dependencies.
- `npm run dev` — start the UI dev server (default port 80, proxied to backend).
- `npm run build:prod` — build the production frontend bundle.

## Coding Style & Naming Conventions
- Java: 4-space indent, UTF-8; classes `PascalCase`, methods/fields `camelCase`, packages under `com.ruoyi.*`.
- Controllers end in `Controller`, services in `ServiceImpl`, mappers in `Mapper`.
- Vue/JS: 2-space indent, LF endings, final newline (see `ruoyi-ui/.editorconfig`).
- Vue components use `PascalCase`; API modules mirror backend resource names.

## Testing Guidelines
- No formal test suite ships with the project; verify changes by running the app.
- If adding backend tests, use JUnit under `src/test/java` with `*Test.java` names and run `mvn test`.
- Validate frontend flows manually via `npm run dev` before submitting.

## Commit & Pull Request Guidelines
- Commit messages are concise, imperative summaries (often Chinese), e.g. `优化代码`, `修复脱敏不生效问题`; keep to one focused change per commit.
- PRs should describe the change, link related issues, and include screenshots for UI updates.
- Confirm the backend builds (`mvn clean package`) and the UI builds (`npm run build:prod`) before requesting review.

## Security & Configuration Tips
- Configure datasource and Redis in `ruoyi-admin/src/main/resources/application*.yml`; never commit real credentials.
- Apply schema changes from `sql/` and note migrations in your PR.
