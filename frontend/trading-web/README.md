# Quant Trading Dashboard: UI/UX & Frontend Philosophy

This document outlines the design standards and technical architecture for this paper trading web application. Future maintenance and development (including by AI assistants) should adhere to these principles.

## 🏛️ Architecture: Feature-Sliced Design (FSD)

The project follows the [FSD architecture](https://feature-sliced.design/) to maintain scalability and modularity.

-   **`app/`**: Global initialization (styles, providers, routing).
-   **`pages/`**: Full-page compositions. Very few logic; primarily connects features.
-   **`features/`**: Domain-specific logic and UI (e.g., `catalog-management`, `kis-management`). This is where most complex interactions reside.
-   **`entities/`**: Domain data models and types (e.g., `symbol`, `market`).
-   **`shared/`**: Common UI components, API wrappers, and utilities.

## 🛠️ Technical Stack & Data Fetching

-   **State Management**: Use **TanStack React Query** (`useQuery`, `useMutation`) for all server-side state. Avoid manual `useEffect` + `fetch` chains.
-   **Modularity**: Large feature panels must be broken down into smaller sub-components (e.g., `CatalogTable`, `SelectionChips`) to keep the code readable and easy to debug.
-   **Type Safety**: Always use TypeScript and prefer shared model types from `entities/`.

## 🎨 Design System

The dashboard uses **shadcn/ui Base Nova** on Tailwind CSS 4 with a light, data-dense trading theme.

- Primitive components live in `src/shared/ui/shadcn`.
- Existing application-wide compositions live in `src/shared/ui` and compose those primitives.
- Feature-specific UI stays in its `features/*/ui` folder.
- Semantic colors, spacing and radii come from `src/app/styles/index.css`; component-level hex/rgb values are not allowed.
- Trading direction uses dedicated order and market tokens so action states do not get confused with price movement.
- Tables use the shared Table primitive, which provides horizontal overflow handling.

The implementation contract and examples are documented in `docs/UI_GUIDE.md`.
