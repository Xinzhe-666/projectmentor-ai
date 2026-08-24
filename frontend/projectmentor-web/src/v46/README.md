# Protected V4.6 frontend snapshot

This directory preserves the V4.6 Complete frontend as an independently owned
experience. Its visual and page baseline is commit `216ae7c`; registration and
email-verification compatibility come from commit `8160af5`.

Do not apply V6.3 Workbench visual changes, components, layouts, tokens, or CSS
overrides to this directory. V4.6 pages and `src/views` pages must never be
collapsed into the same loader or Vue file.

The snapshot shares the current Router, i18n instance, user Pinia store, and
authentication session by design. Changes required by a backend contract must
be narrowly documented and must preserve the V4.6 visual baseline.

Global V4.6 styles must remain under
`html[data-experience="classic"]`. Workbench CSS must never target this
directory's layout or components without its own Workbench scope.
