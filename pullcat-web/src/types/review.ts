export type AnalysisStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
export type SessionStatus = 'FETCHING' | 'ANALYZING' | 'COMPLETED' | 'FAILED' | 'PUBLISHED'
export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO'
export type AnalysisType = 'summary' | 'risk' | 'quality' | 'consistency' | 'testing'

export interface Issue {
  id: string
  severity: Severity
  file: string | null
  line: number | null
  title: string
  description: string
  suggestion: string
  confidence: number
  selected: boolean
  sourceDimensions?: string[]
  feedback?: string | null
  feedbackReason?: string | null
  suggestionCode?: string | null
}

export interface AnalysisResult {
  type: AnalysisType
  status: AnalysisStatus
  model: string
  content: string
  issues: Issue[]
  tokensUsed: number
  startedAt: string | null
  completedAt: string | null
  errorMessage: string | null
}

export interface PRMetadata {
  title: string
  description: string
  owner: string
  repo: string
  pullNumber: number
  baseBranch: string
  headBranch: string
  fileCount: number
  additions: number
  deletions: number
}

export interface ReviewSession {
  id: string
  prUrl: string
  status: SessionStatus
  prMetadata: PRMetadata | null
  analyses: Record<string, AnalysisResult>
  createdAt: string
  completedAt: string | null
  publishedCommentId: number | null
  repositoryFullName: string | null
  rawDiff: string | null
}

export interface Repo {
  owner: string
  repo: string
  fullName: string
  description: string | null
  stars: number | null
  language: string | null
  addedAt: string
}

export interface ReviewListResponse {
  items: ReviewSession[]
  total: number
  page: number
  size: number
}

export interface StatsOverview {
  totalReviews: number
  totalIssues: number
  repoCount: number
  avgIssuesPerReview: number
  severityDistribution: Record<Severity, number>
  commonIssueTypes: { type: string; count: number }[]
}

export interface TaskState {
  name: string
  label: string
  status: AnalysisStatus
  model: string
  startedAt: string | null
  completedAt: string | null
}

export interface SSEEvent {
  type: string
  data: unknown
}

export const TASK_LABELS: Record<string, string> = {
  summary: '变更总结',
  risk: '风险检测',
  quality: '代码质量',
  consistency: '一致性分析',
  testing: '测试覆盖',
}

export const SEVERITY_COLORS: Record<Severity, string> = {
  CRITICAL: 'bg-red-100 text-red-800 border-red-300 dark:bg-red-900/30 dark:text-red-400 dark:border-red-800',
  HIGH: 'bg-orange-100 text-orange-800 border-orange-300 dark:bg-orange-900/30 dark:text-orange-400 dark:border-orange-800',
  MEDIUM: 'bg-yellow-100 text-yellow-800 border-yellow-300 dark:bg-yellow-900/30 dark:text-yellow-400 dark:border-yellow-800',
  LOW: 'bg-gray-100 text-gray-600 border-gray-300 dark:bg-gray-800 dark:text-gray-400 dark:border-gray-700',
  INFO: 'bg-blue-100 text-blue-800 border-blue-300 dark:bg-blue-900/30 dark:text-blue-400 dark:border-blue-800',
}

export const SEVERITY_BG: Record<Severity, string> = {
  CRITICAL: 'border-red-200 bg-red-50 dark:border-red-900/50 dark:bg-red-950/30',
  HIGH: 'border-orange-200 bg-orange-50 dark:border-orange-900/50 dark:bg-orange-950/30',
  MEDIUM: 'border-yellow-200 bg-yellow-50 dark:border-yellow-900/50 dark:bg-yellow-950/30',
  LOW: 'border-gray-200 bg-gray-50 dark:border-gray-800 dark:bg-gray-900/30',
  INFO: 'border-blue-200 bg-blue-50 dark:border-blue-900/50 dark:bg-blue-950/30',
}

export const SEVERITY_BAR_COLORS: Record<Severity, string> = {
  CRITICAL: '#ef4444',
  HIGH: '#f97316',
  MEDIUM: '#eab308',
  LOW: '#6b7280',
  INFO: '#3b82f6',
}

export const ANALYSIS_TYPES: AnalysisType[] = ['summary', 'risk', 'quality', 'consistency', 'testing']
