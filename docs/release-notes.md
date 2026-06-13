# v4.6

Public Preview Launch Pack.

ProjectMentor AI is a project authenticity audit and interview deep-dive platform. It uses code evidence chains, rule-based scanning, and AI deep review to help developers verify project claims, improve resume wording, and prepare for technical interviews.

This version adds:

* Static SEO metadata, browser title, Open Graph, Twitter card, application name, and robots directives.
* A bilingual Public Preview footer with credits guidance, privacy notes, upload warnings, and a lightweight Privacy & Disclaimer disclosure.
* A bilingual privacy and disclaimer document covering uploaded materials, AI limitations, credits, sensitive data, and public-report boundaries.
* More trustworthy public reports with evidence-generation context, base-rule versus AI-enhanced status, and a clear “Not an official certification” label.
* Nginx security response headers without changing the existing `/api` proxy, history fallback, or upload limits.
* VPS and Docker deployment guidance for DNS records, `server_name`, HTTPS certificates, reverse proxy behavior, and optional future sitemap setup.

Boundaries:

* No backend API, database field, dependency, payment system, analytics integration, external image, AI provider, credit-cost rule, API key, `.env.development`, or LICENSE change is included.
* No sitemap is shipped until a canonical public domain is configured; this avoids publishing placeholder or IP-based URLs.

# v4.5-5

Frontend experience, onboarding, and demo-flow refinement.

This version adds:

* A clearer bilingual Landing page for project authenticity audits and interview deep-dives, with students included as an audience rather than the primary positioning.
* Three core value cards, a four-step workflow, a Rule vs AI comparison, audience cards, and an eight-step demo path.
* A zero-project onboarding card with project creation, credit rules, and demo-guide actions.
* Project-creation guidance and a read-only example that never writes sample data.
* A collapsible report-reading guide and explanations for `SUPPORTED`, `PARTIAL`, `DOC_ONLY`, `NO_EVIDENCE`, and `RISKY`.
* A clearer credit center covering the 10-credit registration gift, free capabilities, AI costs, refunds, and beta-credit boundaries.
* Actionable empty states for project, report, interview, credit, Claim-Evidence, AI enhancement, and admin views.
* More consistent admin usage cards, credit-management spacing, table readability, and empty-data actions.
* Updated README, demo guide, roadmap, interview preparation, and architecture documentation.

Boundaries:

* No backend API, database field, dependency, payment system, AI provider, credit-cost rule, external image, API key, or `.env.development` change is included.
* Rule-based scanning remains free. AI-powered audit, explanation, Q&A, hallucination checks, and mock interviews continue to consume the existing credit amounts.

# v4.5-4

AI cost monitoring, admin credit operations, and basic registration abuse protection.

This version adds:

* Admin credit-user pagination with search, balance and cumulative credit metrics.
* Paginated user credit logs with type, module, and time filters.
* Admin grant and deduction endpoints using `ADMIN_GRANT` and `ADMIN_DEDUCT` audit logs.
* An AI usage overview aggregated from existing credit logs, including daily and lifetime calls, consumption, refunds, module ranking, user ranking, and recent AI transactions.
* A bilingual admin UI for AI usage, credit grants, deductions, and user credit logs, with confirmation dialogs for write operations.
* Registration limits of 3 successful accounts per IP per hour and 10 per IP per day.
* Redis-backed registration counters with an in-memory fallback when Redis is unavailable.
* Unit tests for admin authorization, credit adjustments, AI usage aggregation, registration limits, and the existing 10-credit registration gift.

Boundaries:

* No payment system, third-party dependency, database table, AI provider, API key, or `.env.development` change is included.
* Rule scanning, uploads, project management, Dashboard access, history views, and public sharing remain free.

# v4.5-3

Full AI credit enforcement across every OpenAI-compatible LLM entry point.

This version adds:

* Unified costs: AI audit report 2, Claim-Evidence enhancement 2, project Q&A 1, hallucination check 1, mock interview 2, and standalone resume optimization 1 credit.
* Credit checks and reservation before every paid AI entry point.
* Refund transactions when an AI call fails, returns an unusable result, or the generated business result cannot be saved.
* Rule fallback without a charge for project Q&A, hallucination checks, and interview startup when AI is unavailable.
* Estimated-cost labels and confirmation prompts on all AI actions, with synchronized Chinese and English copy.
* Credit-center documentation for the complete AI cost table and new operation labels.
* Unit tests that mock LLM calls and cover insufficient credits, refunds, registration credits, and free history access.

Boundaries:

* Rule scanning, uploads, project management, Dashboard access, history views, public report sharing, and admin feedback remain free.
* No database column, AI provider, dependency, payment system, API key, or `.env.development` change is included.

# v4.5-2

AI Claim-Evidence enhancement and credit-cost unification.

This version adds:

* New users now receive 10 credits on registration via the existing `REGISTER_GIFT` credit log.
* A shared AI credit-cost constant set, including `AI_CLAIM_EVIDENCE = 2`.
* `POST /api/reports/{reportId}/claim-evidence/ai-enhance` for authenticated users to AI-enhance their own report's Claim-Evidence Matrix.
* A report detail button with a 2-credit confirmation prompt, loading state, success refresh, and bilingual UI copy.
* Stored AI enhancement fields inside `pm_analysis_report.claim_evidence` JSON, without adding database columns.
* Refund handling for failed AI calls or failed enhancement persistence.
* Public report rendering for already-saved AI enhancement content without exposing credit data, AI logs, or source code.

Boundaries:

* Rule-based scanning, upload parsing, history views, and Dashboard browsing remain free of credit deductions.
* No new AI provider, vector database, external dependency, or `.env.development` change is included.
* AI enhancement only interprets structured Claim-Evidence data; it does not rescan the full repository or invent implementation details.

# v4.5-1

Claim-Evidence audit engine.

This version adds:

* Rule-based claim extraction from project descriptions, tech stacks, and README files.
* Claim categories for authentication, database, cache, AI, project Q&A, uploads, reports, interviews, admin, credits, deployment, frontend, security, performance, and product capabilities.
* Evidence matching across source code, configuration, SQL, Docker, Nginx, frontend files, and operational scripts.
* Claim statuses: `SUPPORTED`, `PARTIAL`, `DOC_ONLY`, `NO_EVIDENCE`, and `RISKY`.
* A Claim-Evidence Matrix on private and shared report pages, with status filters, risk-first ordering, expandable evidence, and interview explanation copying.
* Evidence snippets limited to 300 characters with secret, password, token, API key, JWT, and database credential redaction.
* A dedicated `pm_analysis_report.claim_evidence` JSON field.

Boundaries:

* Claim extraction and evidence matching are rule-based and do not add another AI call.
* This is not a legal, academic, or authoritative authenticity determination.
* Claim statuses and AI results are not guaranteed to be fully accurate and require human review against real project evidence.
* The feature is intended for learning, project review, resume wording checks, and interview preparation.
* No vector database, complex NLP dependency, or credit-rule change is included.

# v4.4-public-preview

ProjectMentor AI public preview version.

This version includes:

* Project authenticity audit based on README, source files, configurations, deployment files, and project evidence.
* Rule-based scanning and evidence chain generation.
* AI-enhanced audit reports with fallback to rule-based reports.
* AI hallucination check for overclaimed or unsupported project descriptions.
* Project Q&A with lightweight evidence retrieval.
* Interview deep-dive sessions with progress, skip, finish, scoring, and review.
* Report history and interview history.
* Dashboard summary based on real user data.
* Credit account, credit transaction logs, admin credit grants, and feedback management.
* Chinese / English internationalization.
* Large project ZIP upload optimization, up to 800MB, with strict parsing boundaries.
* Docker Compose + Nginx deployment.
* MySQL backup, restore, and production check scripts.
* Nginx security hardening for common scan paths.
* Source-Available / All Rights Reserved license notice.

Known boundaries:

* This is a personal SaaS MVP and public preview, not an enterprise-grade code audit platform.
* AI results are for learning, project review, resume preparation, and interview practice only.
* Project Q&A currently uses lightweight evidence retrieval rather than a vector database.
* Payment integration is not implemented.
* Production use, commercial use, institutional use, or redistribution requires explicit written permission from the author.
