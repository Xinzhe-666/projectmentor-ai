# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Primary users are computer-science students, internship and early-career job seekers, and early-stage developers who need to verify whether a project description is supported by code and prepare to explain their real contribution under interview scrutiny.

Secondary users include reviewers who need to compare a project's stated capabilities with the repository evidence supplied by its owner.

## Product Purpose

ProjectMentor AI (PMAI) helps developers prove rather than package their work. It turns README content, project descriptions, source files, configuration, SQL, and deployment material into an evidence-backed review that identifies unsupported claims, risky wording, missing proof, and likely interview follow-ups.

Success means a user can distinguish what the project claims, what the repository actually proves, what remains uncertain, and how to describe the work accurately in a resume or interview.

## Positioning

PMAI is an evidence-first project authenticity audit and interview deep-dive workbench. Its differentiated mechanism is a traceable Claim–Evidence chain: deterministic rule scanning and source evidence come first, while AI is an optional enhancement for explanation, safer wording, and interview preparation. AI is infrastructure, not the product's visible theme.

## Operating Context

Users create a project, provide a README or upload a source ZIP, run a free rule-based scan, inspect the Claim–Evidence Matrix and source paths, optionally generate AI-enhanced audit material, review risks and confidence, prepare interview follow-ups, and share a read-only report.

The product is currently a bilingual Chinese / English Public Preview used for learning, project review, resume-expression checks, and technical interview preparation.

## Capabilities and Constraints

- Preserve the existing Vue 3, TypeScript, Vite, Element Plus, Pinia, Vue Router, vue-i18n, Axios, and ECharts frontend stack.
- Preserve authentication, project management, README and ZIP ingestion, rule scanning, Claim–Evidence auditing, reports, project Q&A, hallucination checks, interview flows, credits, feedback, voluntary creator support, admin functions, public sharing, and every existing route and API contract.
- Rule-based evidence scans and the base Claim–Evidence Matrix are free; AI-enhanced features consume credits under existing product rules and failures are refunded according to current behavior.
- Claim statuses are `SUPPORTED`, `PARTIAL`, `DOC_ONLY`, `NO_EVIDENCE`, and `RISKY`; their presentation must remain semantically consistent and must not rely on color alone.
- The current product is not an enterprise security audit, official certification, guaranteed truth system, mature vector-database RAG product, or payment platform.
- Users must not upload secrets, tokens, passwords, commercial confidential material, or company-internal code. Outputs remain reference material that users must validate against real code and personal contribution.
- Frontend redesigns must not modify backend behavior, API contracts, routes, or stored business data.

## Brand Commitments

- Product name: ProjectMentor AI; short form: PMAI.
- Voice: serious, precise, restrained, evidence-driven, technically literate, and honest about uncertainty.
- Durable message: help developers prove and explain their work, not inflate or decorate it.
- Avoid AI-company clichés such as brains, robots, sparkles, magic wands, gradient orbs, and claims of effortless intelligence.
- The intended brand asset direction is a geometric PM / PMAI monogram grounded in evidence, verification, audit structure, grids, brackets, checks, and precision. Final logo artwork may be replaced later, so implementation must keep logo assets and components centralized.

## Evidence on Hand

- The repository README documents the product scope, current capabilities, limitations, workflow, credits behavior, privacy boundaries, deployment, and Claim–Evidence semantics.
- The frontend contains working Chinese and English locale files, real authentication-aware routing, feedback and support dialogs, report sharing, and existing product flows.
- The repository does not currently contain a confirmed final PMAI logo SVG set; the current Landing and app shell use ad-hoc text marks.
- No testimonials, customer logos, external certification, commercial security claims, or performance benchmarks are confirmed and none may be fabricated.

## Product Principles

1. Evidence before interpretation: every important conclusion should lead back to a claim, source, file path, configuration, or code excerpt.
2. Explain uncertainty: confidence, partial support, missing evidence, and risk must be visible instead of being hidden behind a single score.
3. AI stays backstage: deterministic checks and traceability define the product; AI only deepens explanation and preparation.
4. Preserve user truth: help users describe real contribution safely rather than maximize perceived project sophistication.
5. Keep the system operational: visual improvements must preserve existing workflows, routes, accessibility, internationalization, and production behavior.

## Accessibility & Inclusion

The web experience must support keyboard navigation, visible focus, semantic headings and landmarks, text alternatives and labels, WCAG-appropriate contrast, non-color status cues, reduced motion, touch targets suitable for mobile, responsive layouts down to 390px, and readable Chinese / English typography with practical Windows, macOS, and Linux fallbacks.
