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

## 🎨 Design Philosophy: Premium Pro-Trader Look

The UI uses a **Glassmorphism Dark Theme** designed for high productivity and professional aesthetics.

### 1. Color Palette & Contrast
-   **Background**: Deep `06070a` (Main) to `0a0c12` (Sidebar).
-   **Text Contrast**: High contrast is mandatory. 
    -   Primary: `#ffffff`
    -   Secondary: `#cbd5e1` (readable light slate)
    -   Muted/Gray: `#94a3b8` (not darker!)
-   **Accents**: `--brand-primary` (`#3b82f6`) and `--brand-secondary` (`#10b981`). Use gradients (`--grad-primary`) sparingly.

### 2. Responsiveness (Mobile-First)
-   Use the `.feature-grid` or `.card-grid` classes for layout.
-   Grids must stack vertically on screens smaller than **1024px**.
-   Tables must be wrapped in a `.scroll-container` (`shared/ui`) to prevent overflow.

### 3. Micro-interactions
-   Add `:hover` effects to all clickable cards and buttons (`transform: translateY(-2px)`).
-   Use `.fade-in` animation for all page and panel entries.

---

*Maintainer: Antigravity AI (Google DeepMind)*
