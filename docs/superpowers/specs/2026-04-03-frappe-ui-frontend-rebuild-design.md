# Frappe UI Frontend Rebuild Design

## Context

The current project at `E:\File\hrms` uses:

- Vue 3 + Vite
- TypeScript
- Element Plus
- Pinia
- Axios
- A Spring Boot backend exposed through `/api`

The reference project at `E:\File\hrms-github\frontend` uses `frappe-ui` and a different frontend structure. Its visual language and component organization are useful references, but its data model, routing assumptions, and backend contract do not match this project.

The goal of this work is to rebuild most core frontend pages using `frappe-ui` visual and component patterns while preserving maximum compatibility with the current backend.

## Goal

Refactor the current frontend so that:

- the application adopts a `frappe-ui`-driven UI layer and visual language,
- most core pages are redesigned to resemble the reference frontend style,
- current backend endpoints, request payloads, response fields, auth flow, and route semantics remain compatible,
- functional regressions are minimized during the first iteration.

This is a compatibility-first frontend rebuild, not a full product migration to the reference project's business architecture.

## Scope

### In Scope

- Rebuild the main application shell with a new layout and navigation system.
- Rebuild the login page and home page.
- Rebuild most core module pages using a unified `frappe-ui`-based page system.
- Introduce reusable primitives for cards, toolbars, filters, tables, forms, dialogs, and empty states.
- Add a shared theme layer to unify spacing, color, typography, and interaction states.
- Keep current API and store integrations compatible.

### Out of Scope

- Backend API redesign.
- Authorization model redesign.
- Route restructuring that breaks existing navigation semantics.
- Changes to business calculations, approval logic, import/export logic, or backend validation rules.
- One-to-one migration of the reference repository's internal resource abstractions.

## Constraints

- Existing backend contract must remain unchanged wherever feasible.
- Existing auth token storage and route guard behavior must continue to work.
- Existing route paths should remain stable unless a compatibility alias is preserved.
- The first iteration may temporarily retain selected `Element Plus` controls where replacing them immediately would create high risk.
- The rebuilt UI must work primarily as a desktop admin system, even though the reference project includes mobile-oriented patterns.

## Recommended Approach

### Option A: Shell-Only Refresh

Rebuild only the login page, home page, layout shell, and navigation while leaving most business pages visually close to the old system.

Pros:

- Lowest implementation risk
- Fastest delivery
- Minimal business regression

Cons:

- Weak visual consistency across modules
- Core business pages still feel legacy

### Option B: Compatibility-First Core Page Rebuild

Rebuild the application shell and most major pages using `frappe-ui` primitives while keeping existing API modules, store semantics, and backend payloads intact.

Pros:

- Best alignment with the requested outcome
- Strong visual consistency
- High backend compatibility
- Lower long-term maintenance risk than a direct clone

Cons:

- Moderate implementation effort
- Requires careful adaptation of existing page logic

### Option C: Reference-Driven Frontend Migration

Attempt to mirror the reference repository's page organization and interaction model as closely as possible, then adapt current backend calls as needed.

Pros:

- Closest visual and structural match to the reference frontend

Cons:

- Highest integration risk
- Large amount of compatibility glue
- More likely to introduce regressions and future maintenance cost

### Recommendation

Use Option B.

This approach gives the requested visual and structural refresh without forcing the current project into a frontend architecture designed around a different backend and workflow model.

## Target Architecture

### 1. Theme Layer

Create a theme layer that defines:

- color tokens,
- spacing scale,
- border radius scale,
- shadows,
- typography rules,
- status colors,
- focus and hover behavior.

This layer becomes the visual source of truth for all rebuilt pages.

### 2. Application Shell

Replace the current shell with a new layout system responsible for:

- sidebar navigation,
- top navigation bar,
- breadcrumb/title context,
- user menu,
- responsive collapsing,
- page content container.

The shell should look and behave closer to the reference frontend, but remain optimized for desktop workflow density.

### 3. Page Primitives

Introduce shared primitives that wrap `frappe-ui` components and normalize common admin patterns:

- page header,
- stats card,
- content card,
- action toolbar,
- search and filter row,
- table wrapper,
- form section,
- detail panel,
- modal or drawer wrapper,
- empty state,
- loading state,
- error state.

These primitives prevent each module from reinventing structure and styling.

### 4. Form Primitives

Create shared form wrappers for common fields and behaviors:

- text input,
- select,
- date picker,
- textarea,
- checkbox or switch,
- upload field,
- submit and cancel actions,
- validation message display.

These should maintain existing payload shapes and validation expectations.

### 5. Data Compatibility Layer

Keep current API modules and stores in place. Add only lightweight display adapters where needed for:

- label formatting,
- option normalization,
- date rendering,
- status badge mapping,
- table display composition.

Do not introduce a new data access model that changes request semantics.

## Page Mapping Strategy

### Login

Rebuild `frontend/src/views/Login.vue` using the new theme and `frappe-ui` patterns.

Preserve:

- existing login API call,
- token persistence,
- store updates,
- redirect behavior.

### Main Layout

Rebuild `frontend/src/layouts/MainLayout.vue` as the new admin shell.

Preserve:

- route structure,
- active menu behavior,
- user session entry points,
- permission-based navigation semantics where already implemented.

### Home

Rebuild `frontend/src/views/Home.vue` using:

- summary cards,
- trend or dashboard sections,
- quick links,
- grouped informational panels.

Preserve:

- existing data sources,
- current backend metrics fields.

### Core Modules

Rebuild the main business pages to follow a consistent pattern:

- header and action zone,
- filters/search,
- primary data surface,
- row actions,
- create/edit/view interaction.

Priority modules:

- system management,
- permission management,
- attendance,
- salary.

Lower-priority modules follow after the core pattern is stable:

- recruitment,
- care,
- finance.

## Compatibility Rules

The implementation must follow these rules:

1. Do not change backend endpoint URLs.
2. Do not change request method, parameter names, or payload structure unless the existing frontend already has a bug that requires a targeted fix.
3. Do not change auth token storage semantics during the first iteration.
4. Do not rewrite approval or business calculation logic unless required to keep the UI working.
5. Do not remove `Element Plus` entirely in the first iteration if selected controls still depend on it.
6. Prefer view-layer replacement over deep logic-layer rewrite.

## Implementation Order

### Phase 1: Foundation

- Add `frappe-ui` dependency and supporting setup.
- Create theme tokens and global style baseline.
- Create shared layout and page primitives.

### Phase 2: Shell and Entry Pages

- Rebuild `App.vue` integration points if needed.
- Rebuild `MainLayout.vue`.
- Rebuild `Login.vue`.
- Rebuild `Home.vue`.

### Phase 3: Core Modules

Rebuild representative high-traffic modules first:

- system,
- permission,
- attendance,
- salary.

These modules establish repeatable page patterns for the rest of the app.

### Phase 4: Secondary Modules

Rebuild:

- recruitment,
- care,
- finance.

### Phase 5: Cleanup and Consolidation

- Reduce duplicated styles,
- simplify component usage,
- assess whether remaining `Element Plus` usage can be narrowed further,
- verify build and page-level regressions.

## Error Handling and UX Rules

- All rebuilt pages must keep explicit loading, empty, and failure states.
- Form submission must preserve current success/failure behavior and messaging semantics.
- Destructive actions must continue to require clear user confirmation.
- Page-level errors must not break the full shell layout.
- If an API response shape is inconsistent across modules, adaptation should happen locally in a display adapter rather than globally mutating backend assumptions.

## Testing Strategy

### Functional Validation

At minimum, validate:

- login and logout flow,
- route guard behavior,
- home/dashboard rendering,
- list page querying,
- create/edit/delete flows where available,
- approval actions where applicable,
- import/export entry points where applicable.

### Build Validation

- `npm install`
- `npm run build`

### Regression Focus

Pay special attention to:

- auth and redirect behavior,
- pagination and filters,
- modal and form state reset,
- file upload or download entry points,
- date and number formatting consistency,
- permission-sensitive navigation items.

## Acceptance Criteria

The rebuild is acceptable when:

- login, home, layout, and major business pages share a coherent `frappe-ui`-driven design language,
- backend integrations remain compatible with the current project,
- major user flows continue to function without material regression,
- the frontend builds successfully,
- the remaining transitional `Element Plus` usage is limited and intentional.

## Risks

- `frappe-ui` is not a drop-in replacement for a desktop admin framework, so some primitives must be adapted rather than used verbatim.
- Existing pages likely combine request logic and UI logic in single files, which increases refactor sensitivity.
- A full `Element Plus` removal in the first pass would likely slow delivery and increase breakage risk.
- Visual parity with the reference frontend should be interpreted as directional, not literal, because the current project has different workflow constraints.

## Decision Summary

Proceed with a compatibility-first rebuild of the current frontend using `frappe-ui` as the primary UI layer.

Use the reference repository for visual direction and component composition ideas, but preserve the current backend contract and core frontend semantics.
