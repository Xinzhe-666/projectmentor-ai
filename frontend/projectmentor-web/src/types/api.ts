export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
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

export interface SkippedProjectFile {
  filePath: string
  reason: string
}

export interface UploadZipResult {
  projectId: number
  savedFileCount: number
  skippedFileCount: number
  files: ParsedProjectFile[]
  skippedFiles?: SkippedProjectFile[]
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

export type ClaimEvidenceStatus =
  | 'SUPPORTED'
  | 'PARTIAL'
  | 'DOC_ONLY'
  | 'NO_EVIDENCE'
  | 'RISKY'

export type ClaimCategory =
  | 'AUTH'
  | 'DATABASE'
  | 'CACHE'
  | 'AI'
  | 'RAG_OR_QA'
  | 'FILE_UPLOAD'
  | 'REPORT'
  | 'INTERVIEW'
  | 'ADMIN'
  | 'CREDIT'
  | 'DEPLOYMENT'
  | 'FRONTEND'
  | 'SECURITY'
  | 'PERFORMANCE'
  | 'BUSINESS_OR_PRODUCT'
  | 'GENERAL'

export interface ClaimEvidenceFile {
  fileId?: number
  filePath: string
  fileType?: string
  evidenceLevel: 'STRONG' | 'WEAK' | string
  matchedKeywords: string[]
  snippet?: string
  reason?: string
}

export interface ClaimEvidenceItem {
  claimText: string
  sourceType: 'PROJECT_DESCRIPTION' | 'TECH_STACK' | 'README' | string
  sourceSnippet?: string
  category: ClaimCategory | string
  status: ClaimEvidenceStatus
  confidenceScore?: number
  reason?: string
  evidenceFiles: ClaimEvidenceFile[]
  resumeAdvice?: string
  interviewQuestion?: string
}

export interface ClaimEvidenceAiItem {
  claimText: string
  aiExplanation?: string
  saferResumeExpression?: string
  likelyInterviewQuestions?: string[]
  improvementSuggestion?: string
}

export interface ClaimEvidenceAiEnhancement {
  aiEnhanced?: boolean
  aiEnhancedAt?: string
  aiSummary?: string
  aiRiskOverview?: string
  aiResumeStrategy?: string
  aiInterviewStrategy?: string
  aiEnhancedItems?: ClaimEvidenceAiItem[]
  aiFallbackText?: string
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
  claimEvidenceList?: ClaimEvidenceItem[]
  claimEvidenceAi?: ClaimEvidenceAiEnhancement
  suggestions?: string
  resumeBasic?: string
  resumeStandard?: string
  resumeAdvanced?: string
  createTime?: string
}

export interface ReportListItem {
  reportId: number
  projectId: number
  projectName?: string
  authenticityScore?: number
  healthScore?: number
  totalScore?: number
  status?: string
  createTime?: string
  updateTime?: string
  shared?: boolean
  shareToken?: string
  summary?: string
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
  claimEvidenceList?: ClaimEvidenceItem[]
  claimEvidenceAi?: ClaimEvidenceAiEnhancement
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
  aiUsed?: boolean
  creditsRefunded?: boolean
}

export interface InterviewMessage {
  id: number
  role: string
  content: string
  score?: number
  feedback?: string
  questionCategory?: string
  evidenceStrength?: 'STRONG' | 'MEDIUM' | 'WEAK' | 'NONE' | string
  sourceFile?: string
  reason?: string
  questionIndex?: number
  skipped?: boolean
  createTime?: string
}

export interface InterviewSession {
  id: number
  projectId: number
  projectName?: string
  mode: string
  status: string
  aiEnabled?: boolean
  totalScore?: number
  summary?: string
  createTime?: string
  finishTime?: string
  messages: InterviewMessage[]
}

export interface InterviewSessionListItem {
  sessionId: number
  projectId: number
  projectName?: string
  totalScore?: number
  questionCount: number
  answeredCount: number
  skippedCount: number
  status?: string
  createTime?: string
  updateTime?: string
}

export interface DashboardSummary {
  projectCount: number
  creditBalance: number
  reportCount: number
  interviewSessionCount: number
  recentProjects: Project[]
  recentReports: ReportListItem[]
  recentInterviews: InterviewSessionListItem[]
}

export interface AdminMe {
  admin: boolean
  userId?: number
  email?: string
}

export interface AdminStats {
  userCount: number
  projectCount: number
  reportCount: number
  qaCount: number
  shareCount: number
  todayUserCount: number
  todayProjectCount: number
  todayReportCount: number
  todayQaCount: number
}

export interface AdminRecentUser {
  id: number
  email: string
  nickname?: string
  createTime?: string
}

export interface AdminRecentProject {
  id: number
  userId: number
  name: string
  techStack?: string
  status?: string
  createTime?: string
}

export interface AdminRecentReport {
  id: number
  projectId: number
  userId?: number
  totalScore?: number
  createTime?: string
}

export interface AdminRecentQa {
  id: number
  userId: number
  projectId: number
  question: string
  aiUsed: boolean
  createTime?: string
}

export interface AdminCreditUser {
  userId: number
  email: string
  nickname?: string
  creditBalance: number
  createTime?: string
}

export interface AdminCreditTransaction {
  id: number
  changeAmount: number
  type: string
  reason?: string
  createTime?: string
}

export interface AdminCreditUserDetail {
  userId: number
  email: string
  nickname?: string
  creditBalance: number
  recentTransactions: AdminCreditTransaction[]
}

export interface AdminGrantCreditPayload {
  userId: number
  amount: number
  reason: string
}

export interface AdminGrantCreditResult {
  userId: number
  email: string
  grantedAmount: number
  newBalance: number
  transactionId: number
}

export type FeedbackType =
  | 'BUG'
  | 'UX'
  | 'AUDIT_INACCURATE'
  | 'QA_INACCURATE'
  | 'INTERVIEW_QUESTION'
  | 'UPLOAD'
  | 'OTHER'

export type FeedbackStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'WONTFIX'

export interface FeedbackSubmitPayload {
  type: FeedbackType
  content: string
  contact?: string
  pageUrl?: string
}

export interface FeedbackSubmitResult {
  id: number
  type: FeedbackType
  status: FeedbackStatus
  createTime?: string
}

export interface AdminFeedback {
  id: number
  userId: number
  userEmail?: string
  type: FeedbackType
  content: string
  contact?: string
  pageUrl?: string
  status: FeedbackStatus
  adminNote?: string
  createTime?: string
  updateTime?: string
}

export type AdminFeedbackDetail = AdminFeedback

export interface AdminFeedbackPage {
  records: AdminFeedback[]
  total: number
  page: number
  size: number
}

export interface AdminFeedbackListParams {
  type?: FeedbackType | ''
  status?: FeedbackStatus | ''
  keyword?: string
  page?: number
  size?: number
}

export interface AdminFeedbackStatusPayload {
  status: FeedbackStatus
  adminNote?: string
}
