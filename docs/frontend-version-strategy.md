# PMAI frontend version strategy

## Product contract

- Every fresh page load starts in **V4.6 Complete / V4.6 完整版**.
- Refreshing, opening a direct link, clicking in from another site, opening a
  new tab, clearing browser data, or restarting the browser must not restore
  V6.3 automatically.
- The version selector is always visible above every public, authentication,
  application, administration, and version-specific page.
- V6.3 is loaded only after the user explicitly selects **V6.3 Latest / V6.3
  最新版**.
- The selection exists only in the in-memory Pinia store. It must never be
  stored in `localStorage`, cookies, or another persistence layer.
- Changing versions preserves the current sign-in state, route, route params,
  query, shared user store, Router instance, and language state whenever the
  selected version owns the route.
- Direct Classic access to Project Defense resolves to the matching V4.6
  Project Detail. Direct Classic access to Settings resolves to the V4.6
  Dashboard.

## Source ownership

- `frontend/projectmentor-web/src/v46/` is the protected V4.6 frontend snapshot.
- The V4.6 visual and page baseline comes from commit `216ae7c` (`V4.6 Public
  Preview Launch Pack`).
- V4.6 registration UI and authentication API compatibility come from commit
  `8160af5`; the registration form includes email verification required by the
  current backend.
- `frontend/projectmentor-web/src/views/`, `components/`, and `layouts/` own the
  V6.3 Workbench experience.
- `frontend/projectmentor-web/src/experiences/registry.ts` is the only map that
  pairs V4.6 and V6.3 route components.
- A route may never point its Classic and Workbench loaders at the same Vue
  file. Authenticated routes resolve `src/v46/layouts/MainLayout.vue` for V4.6
  and `src/layouts/MainLayout.vue` for V6.3.
- Both versions intentionally share the current user Pinia store, Router,
  locale state, backend contracts, and authentication session.

## Approved V6.3 logo source

V6.3 logos are exact PNG crops from `ChatGPT Image 2026年8月16日
20_40_19.png`, whose required source dimensions are `1448 × 1086`. The marks
must not be redrawn, re-typeset, approximated with SVG, or regenerated.

| Output asset | Exact crop |
| --- | --- |
| `brand-board-primary.png` | `860x230+80+178` |
| `brand-board-compact.png` | `310x145+1042+138` |
| `brand-board-icon.png` | `205x195+1094+315` |
| `brand-board-inverse-primary.png` | `455x118+170+688` |

The files live in `frontend/projectmentor-web/src/assets/brand/cropped/`.
`BrandLogo.vue`, report output, authentication pages, public reports, shell,
landing, footer, and favicon may use only these approved crops.

## CSS isolation

- V4.6 owns `src/v46/style.css`, `src/v46/styles/product-theme.css`, and
  `src/v46/styles/animations.css`.
- V4.6 global selectors are scoped to `html[data-experience="classic"]`.
- V6.3 design variables live in `src/styles/workbench-tokens.css`.
- V6.3 global selectors, Element Plus overrides, dialogs, dropdowns, and other
  teleported surfaces are scoped through
  `html[data-experience="workbench"]`.
- `src/styles/version-switcher.css` is experience-neutral and is the only
  version-bar stylesheet.
- Any new global Workbench rule must carry the Workbench data-attribute scope;
  any new V4.6 global rule must carry the Classic scope.

## Release acceptance checklist

1. `/` opens V4.6 and the selector shows V4.6 Complete.
2. Selecting V6.3 replaces the complete Landing component and its brand assets.
3. Refreshing after selecting V6.3 returns to V4.6.
4. `/login` and `/register` resolve independent components in both versions.
5. The V4.6 registration form includes email, verification code, send-code
   action, and the current backend-compatible payload.
6. Authenticated routes resolve independent layouts and view files.
7. Direct Classic Defense and Settings links redirect to their defined V4.6
   destinations.
8. Desktop and 390px layouts keep the selector visible without horizontal
   overflow or content obstruction.
9. V6.3 logo consumers and favicon load only the approved crop files.
10. A CSS AST check finds no unscoped ordinary rules in Workbench global CSS.
11. `npm run build` passes `vue-tsc --noEmit` and the Vite production build.
12. Build output contains separate lazy chunks for V4.6 and V6.3 Landing,
    Dashboard, Project Detail, and the other paired routes.
13. `git diff --check` passes and `node_modules`, `dist`, archives, and secrets
    remain outside the release commit.
