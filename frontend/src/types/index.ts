// ==================== Auth Types ====================
export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  fullName?: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  userId: number
  username: string
  email?: string
  role?: string
}

export interface UserInfo {
  id: number
  username: string
  email: string
  fullName?: string
  role: string
  isActive: boolean
  createdAt?: string
  lastLoginAt?: string
}

// ==================== Code Review Types ====================
export interface CodeReviewRequest {
  code: string
  language?: string
  fileName?: string
  description?: string
}

export interface CodeReviewResponse {
  reviewId: number
  codeContent: string
  language: string
  fileName: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  totalIssues: number
  issues?: ReviewIssue[]
  teachingReport?: any
}

export interface ReviewIssue {
  id: number
  reviewId: number
  agentType: 'CODE_STANDARDS_INSPECTOR' | 'ARCHITECTURE_GUARDIAN' | 'SECURITY_AUDITOR' | 'PERFORMANCE_OPTIMIZER'
  severity: 'HIGH' | 'MEDIUM' | 'LOW'
  category: string
  title: string
  description: string
  lineNumber?: number
  codeSnippet?: string
  suggestion?: string
  teachingExplanation?: string
  createdAt?: string
}

export interface AgentExecution {
  agentType: string
  agentName: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  startedAt?: string
  completedAt?: string
  issuesFound: number
  duration?: number
}

// ==================== Project Types ====================
export interface ProjectUploadResponse {
  projectId: number
  projectName: string
  status: string
  message: string
  uploadType: string
  totalFiles: number
}

export interface ProjectInfo {
  id: number
  userId: number
  projectName: string
  description?: string
  uploadType: 'ZIP' | 'MULTIFILE' | 'GIT'
  sourceUrl?: string
  storagePath?: string
  totalFiles: number
  totalSize?: number
  language?: string
  analyzedFiles: number
  totalIssues: number
  status: 'PENDING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'
  visibility: 'PRIVATE' | 'PUBLIC' | 'TEAM'
  createdAt: string
  updatedAt: string
}

export interface ProjectStatusDTO {
  projectId: number
  projectName: string
  status: 'PENDING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'
  totalFiles: number
  analyzedFiles: number
  totalIssues: number
  progress: number
  createdAt: string
  updatedAt: string
  uploadType: string
}

export interface ProjectFile {
  fileId: number
  filePath: string
  fileName: string
  language?: string
  fileSize: number
  lineCount?: number
  isAnalyzed: boolean
  analysisPriority?: number
  reviewId?: number
  createdAt: string
}

export interface ProjectReport {
  projectId: number
  summary: string
  overallScore: number
  riskLevel: string
  metrics: Record<string, any>
  recommendations: string
  fileStatistics: Record<string, any>
  createdAt: string
  fullMarkdownReport?: string
  fileIssueDetails?: FileIssueDetail[]
  severityDistribution?: Record<string, number>
}

export interface FileIssueDetail {
  fileName: string
  filePath: string
  language: string
  totalIssues: number
  issues: IssueDetail[]
}

export interface IssueDetail {
  agentName: string
  severity: string
  category: string
  title: string
  description: string
  suggestion?: string
  lineNumber?: number
  codeSnippet?: string
}

// ==================== Learning Types ====================
export interface LearningPath {
  pathId: string
  title: string
  description: string
  difficulty: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  estimatedHours: number
  progress: number
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'
  modules: LearningModule[]
}

export interface LearningModule {
  moduleId: string
  title: string
  description: string
  order: number
  completed: boolean
}

export interface Exercise {
  exerciseId: string
  title: string
  description: string
  difficulty: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
  language: string
  estimatedMinutes: number
  xpReward: number
  instructions: string
  starterCode?: string
  testCases?: TestCase[]
}

export interface TestCase {
  input: string
  expectedOutput: string
  isHidden: boolean
}

export interface ExerciseSubmission {
  submissionId: string
  exerciseId: string
  code: string
  submittedAt: string
  status: 'PENDING' | 'PASSED' | 'FAILED'
  score?: number
  feedback?: string
}

// ==================== Teaching Types ====================
export interface SkillInfo {
  language: string
  level: number
  xp: number
  rank: string
}

export interface Achievement {
  achievementId: string
  title: string
  description: string
  icon: string
  category: string
  xpReward: number
  unlockedAt?: string
  progress?: number
  target?: number
}

export interface LeaderboardEntry {
  rank: number
  userId: number
  username: string
  xp: number
  level: number
}

export interface LearningProgress {
  totalXp: number
  level: number
  currentPathId?: string
  pathsCompleted: number
  exercisesCompleted: number
  streakDays: number
}

// ==================== Task Execution Types ====================
export interface TaskExecution {
  executionId: string
  agentType: string
  agentName: string
  status: string
  startedAt: string
  completedAt?: string
  result?: any
}

// ==================== Common Types ====================
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

export interface PaginatedResponse<T> {
  data: T[]
  total: number
  currentPage: number
  pageSize: number
  totalPages: number
  first: boolean
  last: boolean
  hasNext: boolean
  hasPrevious: boolean
}

// ==================== Architecture Analysis Types ====================
export interface ModuleInfo {
  name: string
  path: string
  artifactId?: string
  groupId?: string
  version?: string
  description?: string
  type?: string
  javaFileCount: number
}

export interface CircularDependency {
  cycle: string[]
  description: string
}

export interface InfrastructureUsage {
  databaseUsers: string[]
  redisUsers: string[]
  kafkaUsers: string[]
  objectStorageUsers: string[]
  elasticsearchUsers: string[]
}

export interface LayerInfo {
  name: string
  description: string
  modules: string[]
}

export interface ArchitectureReport {
  projectRoot: string
  analysisTime: string
  modules: ModuleInfo[]
  dependencyGraph: Record<string, string[]>
  circularDependencies: CircularDependency[]
  externalDependencies: Record<string, string[]>
  infrastructureUsage: InfrastructureUsage
  layers: LayerInfo[]
  recommendations: string[]
}

export interface ArchitectureRecommendations {
  recommendations: string[]
  circularDependencies: CircularDependency[]
  moduleCount: number
  hasIssues: boolean
}

// ==================== Knowledge Base Types ====================
export interface Document {
  id: number
  fileName: string
  title?: string
  description?: string
  fileSize: number
  fileType: string  // 后端使用fileType而不是mimeType
  uploadedBy: string
  tags?: string[]
  isPublic: boolean
  status: 'UPLOADED' | 'PROCESSING' | 'INDEXED' | 'FAILED'
  createdAt: string  // 后端使用createdAt而不是uploadDate
  indexedAt?: string
  indexedChunkCount?: number  // 需要从后端添加
  errorMessage?: string
  fileMd5?: string
}

export interface DocumentMetadata {
  id: number
  fileName: string
  fileSize: number
  mimeType: string
  uploadDate: string
  status: Document['status']
  indexedChunkCount: number
  processingStartedAt?: string
  processingCompletedAt?: string
  errorMessage?: string
}

export interface DocumentChunk {
  id: number
  documentId: number
  chunkIndex: number
  content: string
  metadata?: Record<string, any>
}

export interface SearchRequest {
  query: string
  page?: number
  size?: number
  documentType?: string
  dateFrom?: string
  dateTo?: string
}

export interface SearchResult {
  chunkId: string
  documentId: string
  documentName: string
  chunkIndex: number
  content: string
  highlight?: string
  score: number
  metadata?: Record<string, any>
}

export interface QARequest {
  question: string
  contextSize?: number
}

export interface QAResponse {
  answer: string
  sources: SourceReference[]
  confidence: number
}

export interface SourceReference {
  documentId: string
  documentName: string
  chunkId: string
  content: string
  relevance: number
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  sources?: SourceReference[]
  isStreaming?: boolean
}

// ==================== Re-export Knowledge Base Types ====================
// Export all types from the dedicated knowledge-base types file
export type {
  DocumentStatus,
  DocumentDto,
  UploadResponseDto,
  ReindexResponseDto,
  DocumentContentDto,
  SearchHitDto,
  SearchRequestDto,
  HybridSearchResponseDto,
  SearchSuggestionDto,
  AnswerDto,
  ChunkInfoDto,
  QuestionRequestDto,
  ChatHistoryItem,
  ChatSession,
  WebSocketMessage,
  WebSocketMessageType,
  QuestionStartMessage,
  QuestionChunkMessage,
  QuestionCompleteMessage,
  QuestionErrorMessage,
  DocumentStatusMessage,
  SearchProgressMessage,
  // Teaching Document Types
  TeachingDocumentType,
  TeachingDocumentStatus,
  TeachingDocument,
  GenerateTeachingRequest,
  PersonalizedLessonRequest,
  TeachingDocumentsResponse,
  TeachingStats,
  // Agent Task Types
  AgentTaskType,
  AgentTaskStatus,
  AgentTask,
  CreateTaskRequest,
  ScheduledTaskRequest,
  AgentTasksResponse,
  AgentTaskStats,
} from './knowledge-base'
