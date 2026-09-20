# Repository Guidelines

This directory is the RuoYi frontend: a Vue 2.6 single-page admin built on Element UI, Vue Router, and Vuex, served by Vue CLI.

## Project Structure & Module Organization
- `src/api/` — HTTP modules per backend resource (`system/`, `monitor/`, `tool/`, `login.js`).
- `src/views/` — page components grouped by domain (`system/`, `monitor/`, `dashboard/`, `tool/`).
- `src/components/` — reusable UI widgets (`Editor`, `Pagination`, `FileUpload`).
- `src/store/` — Vuex modules; `src/router/` — route definitions.
- `src/utils/` — helpers (`request.js`, `auth.js`, `permission.js`, `ruoyi.js`).
- `src/assets/`, `src/directive/`, `src/plugins/` — assets, custom directives, global plugins.
- `public/` — static shell; `build/` — build scripts; `vue.config.js` — dev server and proxy.

## Build, Test, and Development Commands
- `npm install` — install dependencies (Node 8.9+; use a matching registry).
- `npm run dev` — start the dev server on port 80 with `/dev-api` proxied to the backend.
- `npm run build:prod` — build the production bundle into `dist/`.
- `npm run build:stage` — build using the staging environment.
- `npm run preview` — serve a built bundle locally for inspection.

## Coding Style & Naming Conventions
- 2-space indent, UTF-8, LF endings, final newline (see `.editorconfig`).
- Components use `PascalCase` filenames and folders; utility/api files use `camelCase`.
- API modules mirror backend paths; export named request functions using `src/utils/request.js`.
- Prefer Element UI components and existing utilities before adding dependencies.

## Testing Guidelines
- No automated test suite is configured; verify changes manually via `npm run dev`.
- Confirm affected pages, permissions, and API calls work, and that `npm run build:prod` succeeds before submitting.

## Commit & Pull Request Guidelines
- Use concise, imperative commit summaries (often Chinese), e.g. `优化页签滚动条层级`; one focused change per commit.
- PRs should describe the change, link related issues, and include before/after screenshots for UI updates.

## Configuration Tips
- Environment variables live in `.env.development`, `.env.staging`, `.env.production`.
- Set the API base via `VUE_APP_BASE_API`; adjust backend `proxy.target` in `vue.config.js`.
