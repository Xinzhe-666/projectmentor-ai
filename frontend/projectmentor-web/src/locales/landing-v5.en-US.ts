export default {
  nav: {
    system: 'Project Evidence Audit System',
    login: 'Log in',
    workspace: 'Open workspace',
    start: 'Start free audit',
    more: 'More',
    feedback: 'Feedback',
    support: 'Support the maker'
  },
  meta: {
    release: 'PMAI V5',
    environment: 'Public Preview',
    rules: 'Rule engine online',
    privacy: 'Do not upload secrets'
  },
  hero: {
    title: 'Make every project claim survive scrutiny.',
    description: 'ProjectMentor AI puts README text, source, and configuration into one evidence chain—testing what your project can honestly claim, then turning weak spots into safer resume wording and interview deep-dives.',
    primary: 'Audit a project',
    secondary: 'See the method',
    note: 'Rule scans are free; AI deep review uses credits.'
  },
  audit: {
    title: 'PROJECT AUTHENTICITY AUDIT / PMAI-WEB',
    illustrative: 'ILLUSTRATIVE AUDIT',
    illustrativeNote: 'This interface uses real paths from the repository; it is not a project certification.',
    phase: {
      searching: 'LOCATING EVIDENCE',
      found: 'SOURCE FOUND',
      verified: 'CLAIM VERIFIED'
    },
    claim: 'CLAIM',
    evidence: 'EVIDENCE SOURCE',
    verdict: 'VERDICT',
    selected: 'CURRENT INSPECTION',
    sourceMatch: 'SOURCE MATCH',
    sourceReason: 'Initialization rejects secrets under 32 characters and constructs an HMAC signing key from configuration.',
    line: 'LINES 27–30',
    confidence: 'RULE CONFIDENCE 0.96',
    claims: {
      jwt: 'The project signs and validates login identity with JWT',
      async: 'Audit reports execute through an asynchronous task',
      preview: 'The product is currently in Public Preview',
      redis: 'The cache layer uses Redis',
      scale: 'The system supports high-concurrency production load'
    },
    paths: {
      jwt: 'backend/.../util/JwtUtil.java',
      async: 'backend/.../analysis/service/AnalysisTaskAsyncExecutor.java',
      preview: 'README.md',
      redis: 'No Redis configuration or implementation found',
      scale: 'No load-test or deployment evidence supplied'
    }
  },
  status: {
    supported: 'SUPPORTED',
    partial: 'PARTIAL',
    docOnly: 'DOC_ONLY',
    noEvidence: 'NO_EVIDENCE',
    risky: 'RISKY'
  },
  method: {
    title: 'Move from claim to verdict without skipping the evidence.',
    description: 'PMAI is not a polished score generator. It turns each conclusion back into a source you can inspect.',
    claimTitle: 'Claim',
    claimDesc: 'Extract verifiable statements from README text, resume wording, and project descriptions.',
    evidenceTitle: 'Evidence',
    evidenceDesc: 'Inspect source, configuration, SQL, and deployment material with paths and match reasons.',
    verdictTitle: 'Verdict',
    verdictDesc: 'State support, gaps, and wording risk with a precise evidence status.'
  },
  taxonomy: {
    title: 'Five states answer how much evidence exists.',
    description: 'Color is secondary; every verdict keeps its name, source, and explanation.',
    supported: 'Implementation evidence directly supports the claim.',
    partial: 'Related implementation exists, but not enough for the full wording.',
    docOnly: 'The claim appears in documentation without implementation evidence.',
    noEvidence: 'No verifiable source was found in the supplied material.',
    risky: 'The wording is stronger than the evidence and invites scrutiny.'
  },
  layers: {
    title: 'Rules make stable findings. AI adds depth when you choose.',
    description: 'Base scanning stays separate from generation: inspect the materials with repeatable rules, then decide whether to spend credits.',
    ruleTitle: 'Rule-based evidence scan',
    ruleLabel: 'Free / repeatable',
    ruleDesc: 'Locate risk language, file paths, configuration matches, and the base Claim–Evidence Matrix. Every result can be checked against source material.',
    aiTitle: 'AI deep enhancement',
    aiLabel: 'Uses credits',
    aiDesc: 'Use existing evidence to produce deeper explanations, safer resume wording, project Q&A, and mock-interview follow-ups. Failed calls are refunded.'
  },
  boundary: {
    title: 'Protect the code before you audit it.',
    description: 'Public Preview is for learning, project review, and interview preparation—not security auditing or resume certification.',
    warning: 'Do not upload real keys, company code, trade secrets, or sensitive files.',
    details: 'Read the complete preview boundaries',
    item1: 'Incomplete materials can produce incomplete evidence findings.',
    item2: 'AI output is a reference; you still need to understand and verify your code.',
    item3: 'Larger ZIP uploads may require more processing time.',
    primary: 'Start a safe audit',
    secondary: 'Log in first'
  }
}
