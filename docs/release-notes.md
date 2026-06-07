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
