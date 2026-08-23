---
name: ProjectMentor AI
description: An evidence-first verification dossier for traceable project claims.
colors:
  evidence-ink: "#0b1220"
  graphite: "#2b313b"
  muted-slate: "#667085"
  stone-rule: "#e2e5e9"
  stone-rule-strong: "#c9ced6"
  cool-paper: "#f5f6f7"
  evidence-surface: "#ffffff"
  verification-cobalt: "#2c5aa0"
  verification-cobalt-dark: "#20477f"
  verification-cobalt-soft: "#e8eff9"
  inspection-surface: "#101722"
  inspection-surface-deep: "#0c121c"
  inspection-rule: "#2a3444"
  inspection-text: "#edf1f7"
  inspection-muted: "#aeb7c5"
  supported: "#22613d"
  supported-soft: "#eaf4ed"
  partial: "#76540c"
  partial-soft: "#f7f0dc"
  doc-only: "#31598f"
  doc-only-soft: "#e9eff8"
  no-evidence: "#5b6472"
  no-evidence-soft: "#edf0f3"
  risky: "#983838"
  risky-soft: "#f7e9e8"
typography:
  display:
    fontFamily: '"IBM Plex Sans", "PingFang SC", "Noto Sans CJK SC", "Microsoft YaHei UI", sans-serif'
    fontSize: "clamp(3.15rem, 6vw, 5.8rem)"
    fontWeight: 600
    lineHeight: 0.98
    letterSpacing: "-0.035em"
  headline:
    fontFamily: '"IBM Plex Sans", "PingFang SC", "Noto Sans CJK SC", "Microsoft YaHei UI", sans-serif'
    fontSize: "clamp(2.2rem, 4.2vw, 4.4rem)"
    fontWeight: 600
    lineHeight: 1.05
    letterSpacing: "-0.035em"
  title:
    fontFamily: '"IBM Plex Sans", "PingFang SC", "Noto Sans CJK SC", "Microsoft YaHei UI", sans-serif'
    fontSize: "20px"
    fontWeight: 600
    lineHeight: 1.45
    letterSpacing: "-0.015em"
  body:
    fontFamily: '"IBM Plex Sans", "PingFang SC", "Noto Sans CJK SC", "Microsoft YaHei UI", sans-serif'
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.7
    letterSpacing: "normal"
  action:
    fontFamily: '"IBM Plex Sans", "PingFang SC", "Noto Sans CJK SC", "Microsoft YaHei UI", sans-serif'
    fontSize: "14px"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "normal"
  label:
    fontFamily: '"IBM Plex Mono", "SFMono-Regular", Consolas, monospace'
    fontSize: "10px"
    fontWeight: 400
    lineHeight: 1
    letterSpacing: "0.08em"
  code:
    fontFamily: '"IBM Plex Mono", "SFMono-Regular", Consolas, monospace'
    fontSize: "11px"
    fontWeight: 400
    lineHeight: 1.75
    letterSpacing: "normal"
motion:
  fast: "120ms"
  base: "200ms"
  slow: "320ms"
  easeOut: "cubic-bezier(0.16, 1, 0.3, 1)"
  easeStandard: "cubic-bezier(0.2, 0.8, 0.2, 1)"
rounded:
  sm: "4px"
  md: "8px"
spacing:
  "1": "4px"
  "2": "8px"
  "3": "12px"
  "4": "16px"
  "5": "20px"
  "6": "24px"
  "8": "32px"
  "10": "40px"
  "12": "48px"
  "16": "64px"
components:
  brand-logo:
    textColor: "{colors.evidence-ink}"
    size: "36px"
    tagline: "Authenticity. Evidence. Confidence."
  button-primary:
    backgroundColor: "{colors.verification-cobalt}"
    textColor: "{colors.evidence-surface}"
    typography: "{typography.action}"
    rounded: "{rounded.sm}"
    padding: "0 20px"
    height: "48px"
  button-primary-hover:
    backgroundColor: "{colors.verification-cobalt-dark}"
    textColor: "{colors.evidence-surface}"
    typography: "{typography.action}"
    rounded: "{rounded.sm}"
    padding: "0 20px"
    height: "48px"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.evidence-ink}"
    typography: "{typography.action}"
    rounded: "{rounded.sm}"
    padding: "0 20px"
    height: "48px"
  button-secondary-hover:
    backgroundColor: "{colors.evidence-surface}"
    textColor: "{colors.evidence-ink}"
    typography: "{typography.action}"
    rounded: "{rounded.sm}"
    padding: "0 20px"
    height: "48px"
  language-switch:
    backgroundColor: "{colors.evidence-surface}"
    textColor: "{colors.graphite}"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "0 8px"
    height: "44px"
    width: "40px"
  language-switch-active:
    backgroundColor: "{colors.verification-cobalt-soft}"
    textColor: "{colors.verification-cobalt-dark}"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "0 8px"
    height: "44px"
    width: "40px"
  audit-workbench:
    backgroundColor: "{colors.inspection-surface}"
    textColor: "{colors.inspection-text}"
    rounded: "{rounded.md}"
    width: "100%"
  footer-ledger:
    backgroundColor: "{colors.cool-paper}"
    textColor: "{colors.muted-slate}"
    width: "100%"
  verdict-supported:
    textColor: "{colors.supported}"
    typography: "{typography.label}"
  verdict-partial:
    textColor: "{colors.partial}"
    typography: "{typography.label}"
  verdict-doc-only:
    textColor: "{colors.doc-only}"
    typography: "{typography.label}"
  verdict-no-evidence:
    textColor: "{colors.no-evidence}"
    typography: "{typography.label}"
  verdict-risky:
    textColor: "{colors.risky}"
    typography: "{typography.label}"
---

# Design System: ProjectMentor AI

## Overview

**Creative North Star: "Verification Dossier / Engineering Audit Ledger"**

ProjectMentor AI presents evidence with the composure of a premium engineering dossier: serious, precise, editorial, and enterprise-capable without becoming ornamental. The visual system uses cool paper, dark ink, measured cobalt, compact controls, and source-led grids so the interface reads like a verification instrument rather than an AI spectacle.

The system is dense where evidence benefits from comparison and spacious where claims need interpretation. Rules, paths, and status rows carry the visual authority; AI remains backstage. Flat, ruled surfaces are the default, while the dark audit workbench provides the single signature inspection environment.

**Key Characteristics:**

- Cool paper and white evidence surfaces anchored by dark ink and stone rules.
- Verification Cobalt reserved for decisive actions, focus, and selected evidence.
- Editorial IBM Plex Sans hierarchy paired with narrowly scoped IBM Plex Mono metadata.
- Compact square controls, explicit borders, and five text-and-symbol evidence states.
- Responsive evidence sequences that preserve claim, source, and verdict relationships.

## Colors

The palette is a cool technical neutral field with one cobalt action voice and five quiet, explicit evidence states.

### Primary

- **Verification Cobalt:** The only general-purpose accent, used for primary actions, focus outlines, active controls, and selected source evidence.
- **Deep Verification Cobalt:** The hover and pressed continuation of the primary accent; it communicates action state without glow or movement.
- **Soft Verification Cobalt:** A restrained active or selected fill for compact controls and evidence emphasis.

### Neutral

- **Evidence Ink:** Primary text, dark boundary fields, and the visual anchor for the system.
- **Graphite:** Supporting copy and secondary control text when full ink would be too forceful.
- **Muted Slate:** Explanatory copy, secondary metadata, and low-priority system information.
- **Stone Rule / Strong Stone Rule:** Dividers, table structure, control borders, and section boundaries; strong stone is reserved for higher-order separation.
- **Cool Paper:** The default application and landing-page ground.
- **Evidence Surface:** White controls and content surfaces that need clear separation from paper.
- **Inspection Surface / Deep Inspection Surface:** Near-ink fields that hold claim-ledger and source-code evidence without turning the rest of the product dark.
- **Inspection Rule / Text / Muted:** Cool dark-field dividers and light text values tuned for dense audit reading.

### Semantic Evidence States

- **Supported:** Green foreground with a pale green field for repository-backed claims.
- **Partial:** Ochre foreground with a pale amber field for incomplete support.
- **Document Only:** Slate-cobalt foreground with a pale blue field for claims backed only by documentation.
- **No Evidence:** Neutral slate foreground with a cool gray field for an absence of proof.
- **Risky:** Restrained red foreground with a pale red field for unsafe or overstated wording.

**The One Accent Rule.** Verification Cobalt is the sole general action accent; semantic colors remain attached to evidence meaning and never become decoration.

**The Non-Color Verdict Rule.** Every evidence state includes a text label and square status marker, so color never carries the verdict alone.

## Typography

**Display Font:** IBM Plex Sans (with PingFang SC, Noto Sans CJK SC, Microsoft YaHei UI, and sans-serif fallbacks)\
**Body Font:** IBM Plex Sans (with the same explicit CJK stack)\
**Label/Mono Font:** IBM Plex Mono (with SFMono-Regular and Consolas fallbacks)

**Character:** The pairing is technical without looking terminal-themed. Sans carries the editorial argument; mono exposes proof, identifiers, paths, timestamps, and machine state.

### Hierarchy

- **Display:** Semibold, tightly tracked, near-solid leading; reserved for the largest page thesis.
- **Headline:** Semibold and tightly tracked with slightly more leading; used for section propositions.
- **Title:** Semibold with modest negative tracking; used for method steps, ledger groups, and component headings.
- **Body:** Regular with generous reading leading; supporting copy stays near a 60-character measure where the layout allows.
- **Action:** Semibold sans at compact UI scale; used for primary, secondary, and navigation buttons.
- **Label:** Regular mono with open tracking and uppercase treatment for system metadata and compact statuses.
- **Code:** Regular mono with generous line height for source excerpts and paths; long paths may wrap anywhere rather than overflow.

**The Mono Evidence Rule.** Use IBM Plex Mono only for paths, source, metadata, identifiers, timestamps, line numbers, and statuses; editorial copy and calls to action stay in IBM Plex Sans.

**The CJK Continuity Rule.** Chinese text falls through the explicit CJK sans stack and keeps the same hierarchy and density instead of receiving a separate visual personality.

## Layout

The spatial model is a source-led editorial grid within a centered maximum width of 1320px. Major sections use one- or two-column propositions, ruled ledgers, and generous vertical intervals; internal controls and evidence rows use the compact 4px-based rhythm. Full-width colored fields may extend beyond the content grid, but their contents return to the same centered measure.

At 1060px, wide two-column systems begin to stack. At 820px, hero, section, ledger, and inspection columns become linear reading sequences. At 560px, the header compacts, actions become full-width, the audit ledger becomes one claim-source-verdict sequence per row, and page gutters reduce to 16px or less where the inspection surface intentionally reaches the edge. Interactive controls retain at least a 44px target.

**The Evidence Sequence Rule.** When columns collapse, claim, source, and verdict remain in the same reading unit; paths wrap and actions remain visible.

## Elevation & Depth

The system is flat and ruled by default. Depth comes from paper-to-surface contrast, one-pixel stone dividers, selected-row fills, and dark-to-light field changes. The full audit workbench receives the one strong structural shadow (`0 26px 64px rgba(11, 18, 32, 0.18)`); transient utility menus may use a smaller functional lift (`0 16px 32px rgba(11, 18, 32, 0.12)`) while open.

### Shadow Vocabulary

- **Audit Workbench:** Strong structural separation for the signature inspection environment only.
- **Transient Menu:** Short-lived lift for a menu that must clear the header plane; it is not a decorative ambient shadow.

**The Flat Ledger Rule.** Static content surfaces stay flat and gain hierarchy through rules, tone, and density rather than stacked shadows.

## Shapes

The form language is compact, square, and precise. Standard controls and method nodes use a tight 4px radius; structural containers and transient panels may use 8px, while evidence markers remain square. One-pixel borders and square-ended linework define the brand mark, grids, statuses, and disclosure rows. Shapes never become soft enough to read as bubbles or pills.

**The Two-Radius Rule.** Use 4px for controls and method nodes, 8px for structural containers, and no larger radius as a default system choice; status markers remain square.

## Components

### Buttons

- **Shape:** Compact rectangular controls with a tight 4px radius and a minimum height of 48px; header actions may use the 44px compact height.
- **Primary:** Solid Verification Cobalt with white text, a matching border, semibold sans text, and 20px horizontal padding.
- **Hover / Focus:** Hover deepens to Deep Verification Cobalt in 120ms; keyboard focus uses the global two-pixel cobalt outline with a three-pixel offset.
- **Secondary:** Transparent paper with an explicit strong-stone border and Evidence Ink text; hover sharpens the border to ink and fills white.
- **Dark Secondary:** Boundary-field variant uses a cool gray border and light text, then gains a slightly lighter border and ink-toned surface on hover.

### Language Switch

Two 40-by-44px mono controls share one 4px ruled container. A single vertical divider separates languages; the active option uses Soft Verification Cobalt and dark cobalt text. The selected state is also exposed with `aria-pressed`.

### Brand Logo

BrandLogo V2 combines a geometric PM verification mark with square audit brackets and an integrated cobalt check. The primary lockup pairs `ProjectMentor AI` with `Authenticity. Evidence. Confidence.`; compact uses `PMAI`; icon uses the mark alone. Default, inverted, and single-color monochrome treatments share the same centralized SVG geometry so Landing, authentication, shell, reports, public sharing, footer, and favicon never diverge.

### Motion

Motion communicates state and hierarchy rather than attracting attention. Fast (120ms) handles hover and focus color, Base (200ms) handles active navigation and selected evidence, and Slow (320ms) handles the single page/report entrance moment. Use the shared exponential ease-out for entrances and the standard state easing for selections. No bounce, scale pop, rotation, parallax, infinite animation, or decorative loop is part of the product system. Reduced-motion mode removes spatial movement while preserving the final selected, expanded, and loaded state.

### Cards / Containers

- **Corner Style:** Flat sections are uncontained; signature or operational containers use the 8px structural radius.
- **Background:** Cool Paper carries the page, white carries evidence surfaces, and dark ink carries high-attention inspection or boundary fields.
- **Shadow Strategy:** Follow the Flat Ledger Rule; only the audit workbench and an open transient menu lift.
- **Border:** One-pixel Stone Rule boundaries create hierarchy and column structure.
- **Internal Padding:** Compact rows use 12-20px; explanatory panels use 20-32px while preserving the shared spacing rhythm.

### Evidence Verdict

Each verdict is a compact mono label paired with a seven-pixel outlined square. The same five semantic state pairs appear on light and dark surfaces, with dark-surface values adjusted for legibility while meaning and wording remain unchanged.

### Audit Workbench

The signature inspection component is a dark, full-width ruled ledger that keeps claim, source path, verdict, and selected-source detail in one environment. Its rows use explicit column dividers, mono metadata, one cobalt selected-row rule, and an evidence-resolution transition that changes state rather than decorating the page.

### Navigation and Footer

Navigation uses a compact brand header, square controls, and text actions with no decorative chrome. The footer is an information ledger: three ruled columns for summary, product facts, and privacy disclosure, collapsing to two columns and then one while retaining underlined text links and 44px disclosure targets.

## Do's and Don'ts

### Do:

- **Do** let claim, source, and verdict remain visibly traceable in evidence-oriented components.
- **Do** use rules, alignment, text hierarchy, and state labels before adding containers or depth.
- **Do** reserve Verification Cobalt for decisive actions, focus, and selected evidence.
- **Do** preserve keyboard focus, reduced-motion behavior, 44px mobile targets, and non-color verdict cues.
- **Do** keep brand assets centralized so the geometric evidence mark can be replaced without redesigning consumers.

### Don't:

- **Don't** use purple or teal gradients, aurora fields, glass, glow, or atmospheric blur as the product identity.
- **Don't** present the product as a floating dashboard mockup, Bento composition, or stack of nested cards.
- **Don't** use pills or chips as layout structure, huge rounding, or soft bubble geometry.
- **Don't** animate decoration; motion may communicate audit resolution, state change, disclosure, or navigation only.
- **Don't** use IBM Plex Mono for prose, headlines, or calls to action.
- **Don't** let semantic evidence colors become interchangeable accents or rely on color alone.
