# IRP UI

Phase 1 dashboard for the Incident Response Platform. Vite + React 18 + TypeScript + Tailwind.

## Develop

```bash
cd ui
npm install
npm run dev
```

The dev server runs on http://localhost:5173 and proxies `/api/*` to the incident-service at `http://localhost:8080`.

## Screens

- `/` — incident list + create form
- `/incidents/:id` — incident detail with lifecycle action buttons and event timeline
