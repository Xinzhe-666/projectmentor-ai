export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface UserInfo {
  id: number
  username: string
  email: string
  role: string
}

export interface LoginResponse {
  token: string
  userInfo: UserInfo
  remainingCredits: number
}

export interface Project {
  id: number
  name: string
  githubUrl?: string
  description?: string
  projectType?: string
  techStack?: string
  status?: string
  createTime?: string
  updateTime?: string
}

export interface ProjectFile {
  id: number
  projectId: number
  filePath: string
  fileType: string
  content?: string
  contentLength?: number
  createTime?: string
  updateTime?: string
}

export interface ParsedProjectFile {
  id: number
  filePath: string
  fileType: string
  contentLength: number
}

export interface UploadZipResult {
  projectId: number
  savedFileCount: number
  skippedFileCount: number
  files: ParsedProjectFile[]
  warnings: string[]
  skippedByReason: Record<string, number>
}

export interface ProjectQaEvidence {
  filePath: string
  reason: string
  snippet: string
}

export interface ProjectQaResponse {
  question: string
  answer: string
  aiUsed: boolean
  evidences: ProjectQaEvidence[]
  suggestedFollowUps: string[]
  evidenceLevel?: 'STRONG' | 'MEDIUM' | 'WEAK' | 'NONE' | string
  evidenceLevelText?: string
  evidenceSummary?: string
  interviewAnswer?: string
  resumeRisk?: string
  confidenceScore?: number
}

export interface ProjectQaHistoryRecord extends ProjectQaResponse {
  id: number
  createTime: string
}

export interface RuleScanRisk {
  riskLevel: string
  riskType: string
  keyword?: string
  sourceFile?: string
  message: string
  evidence?: string
  suggestion?: string
}

export interface RuleScanEvidence {
  conclusion: string
  sourceFile?: string
  detail?: string
  evidence?: string
  suggestion?: string
  riskLevel?: string
}

export interface RuleScanResult {
  projectId: number
  projectName: string
  hasReadme: boolean
  fileCount: number
  totalRiskCount: number
  highRiskCount: number
  mediumRiskCount: number
  lowRiskCount: number
  risks: RuleScanRisk[]
  evidences: RuleScanEvidence[]
  suggestions: string[]
}

export interface AnalysisTask {
  taskId: number
  projectId: number
  taskType: string
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | string
  progress: number
  reportId?: number
  failReason?: string
  message?: string
  createTime?: string
  finishTime?: string
}

export interface AnalysisReport {
  id: number
  projectId: number
  totalScore?: number
  runnabilityScore?: number
  authenticityScore?: number
  structureScore?: number
  readmeScore?: number
  securityScore?: number
  engineeringScore?: number
  interviewScore?: number
  summary?: string
  strengths?: string
  weaknesses?: string
  riskPoints?: string
  evidenceChain?: string
  suggestions?: string
  resumeBasic?: string
  resumeStandard?: string
  resumeAdvanced?: string
  createTime?: string
}

export interface ReportShare {
  reportId: number
  shareToken?: string
  shareUrl?: string
  enabled: boolean
  expireTime?: string
}

export interface PublicReport {
  projectName: string
  projectType?: string
  techStack?: string
  totalScore?: number
  runnabilityScore?: number
  authenticityScore?: number
  structureScore?: number
  readmeScore?: number
  securityScore?: number
  engineeringScore?: number
  interviewScore?: number
  summary?: string
  strengths?: string
  weaknesses?: string
  riskPoints?: string
  evidenceChain?: string
  suggestions?: string
  resumeBasic?: string
  resumeStandard?: string
  resumeAdvanced?: string
  createTime?: string
}

export interface CreditInfo {
  userId: number
  planType: string
  remainingCredits: number
  expireTime?: string
}

export interface CreditLog {
  id: number
  userId: number
  changeAmount: number
  beforeAmount: number
  afterAmount: number
  operationType: string
  businessId?: number
  remark?: string
  createTime?: string
}

export interface AiStatus {
  enabled: boolean
  configured: boolean
  model: string
  baseUrl: string
  message: string
}

export interface HallucinationIssue {
  riskLevel: string
  issueType: string
  matchedText?: string
  message: string
  evidence?: string
  suggestion?: string
}

export interface HallucinationCheckResult {
  credibilityScore: number
  objectivityScore: number
  riskLevel: string
  overEncouragementRisk: boolean
  missingEvidenceRisk: boolean
  resumeRisk: boolean
  issueCount: number
  issues: HallucinationIssue[]
  unsafeResumeStatements: string[]
  saferRewrite?: string
}

export interface InterviewMessage {
  id: number
  role: string
  content: string
  score?: number
  feedback?: string
  createTime?: string
}

export interface InterviewSession {
  id: number
  projectId: number
  projectName?: string
  mode: string
  status: string
  totalScore?: number
  summary?: string
  createTime?: string
  finishTime?: string
  messages: InterviewMessage[]
}
