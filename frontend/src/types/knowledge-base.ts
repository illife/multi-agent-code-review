// ==================== Knowledge Base Types ====================

/**
 * Document status enum
 */
export type DocumentStatus = 'UPLOADED' | 'PROCESSING' | 'INDEXED' | 'FAILED'

/**
 * Document entity from backend
 */
export interface DocumentDto {
  id: number
  title: string
  description?: string
  fileName: string
  filePath?: string
  fileType: string
  fileSize?: number
  uploadedBy: string
  tags?: string[]
  isPublic: boolean
  status: DocumentStatus
  indexedAt?: string
  storagePath?: string
  fileMd5?: string
  metadata?: Record<string, any>
  createdAt: string
  updatedAt: string
}

/**
 * Upload response from document upload
 */
export interface UploadResponseDto {
  id: number
  fileName: string
  title: string
  status: DocumentStatus
  alreadyExists?: boolean
  message: string
}

/**
 * Reindex response
 */
export interface ReindexResponseDto {
  totalDocuments: number
  successCount: number
  failCount: number
  message: string
}

/**
 * Document content response
 */
export interface DocumentContentDto {
  documentId: number
  fileName: string
  fileType: string
  content: string
  contentLength: number
}

// ==================== Search Types ====================

/**
 * Search hit result from Elasticsearch
 */
export interface SearchHitDto {
  id: string
  documentId: number
  chunkIndex: number
  content: string
  title: string
  fileName: string
  fileType: string
  department?: string
  tags?: string[]
  score: number
  bm25Score?: number
  knnScore?: number
  rrfScore?: number
  highlight?: string
}

/**
 * Search request
 */
export interface SearchRequestDto {
  query: string
  page?: number
  size?: number
  departments?: string[]
  tags?: string[]
  fileTypes?: string[]
  dateFrom?: string
  dateTo?: string
}

/**
 * Hybrid search response
 */
export interface HybridSearchResponseDto {
  results: SearchHitDto[]
  total: number
  page: number
  size: number
  totalPages: number
  query: string
  bm25Count: number
  knnCount: number
}

/**
 * Search suggestion
 */
export interface SearchSuggestionDto {
  text: string
  score?: number
}

// ==================== Q&A Types ====================

/**
 * Answer DTO from Q&A service
 */
export interface AnswerDto {
  answer: string
  sources: Array<Record<string, any>>
  citations: Array<Record<string, any>>
}

/**
 * Chunk info from retrieval
 */
export interface ChunkInfoDto {
  id: string
  score: number
  content: string
  title: string
  fileName: string
  documentId: number
  highlight?: string
}

/**
 * Question request
 */
export interface QuestionRequestDto {
  question: string
  sessionId?: string
}

/**
 * Chat history item
 */
export interface ChatHistoryItem {
  id: number
  userId: string
  question: string
  answer: string
  sources?: any
  contextUsed?: any
  feedback?: number
  createdAt: string
}

/**
 * Chat session
 */
export interface ChatSession {
  sessionId: string
  userId: string
  title: string
  questionCount: number
  lastQuestionAt: string
  createdAt: string
}

// ==================== WebSocket Types ====================

/**
 * WebSocket message types for Q&A streaming
 */
export type WebSocketMessageType =
  | 'question_start'
  | 'question_chunk'
  | 'question_complete'
  | 'question_error'
  | 'document_status'
  | 'search_progress'

/**
 * Base WebSocket message
 */
export interface WebSocketMessage<T = any> {
  type: WebSocketMessageType
  data: T
  timestamp?: string
}

/**
 * Question start message
 */
export interface QuestionStartMessage extends WebSocketMessage {
  type: 'question_start'
  data: {
    questionId: string
    question: string
  }
}

/**
 * Question chunk message (streaming answer)
 */
export interface QuestionChunkMessage extends WebSocketMessage {
  type: 'question_chunk'
  data: {
    questionId: string
    chunk: string
  }
}

/**
 * Question complete message
 */
export interface QuestionCompleteMessage extends WebSocketMessage {
  type: 'question_complete'
  data: {
    questionId: string
    answer: string
    sources: Array<Record<string, any>>
  }
}

/**
 * Question error message
 */
export interface QuestionErrorMessage extends WebSocketMessage {
  type: 'question_error'
  data: {
    questionId: string
    error: string
  }
}

/**
 * Document status update message
 */
export interface DocumentStatusMessage extends WebSocketMessage {
  type: 'document_status'
  data: {
    documentId: number
    status: DocumentStatus
    message?: string
  }
}

/**
 * Search progress message
 */
export interface SearchProgressMessage extends WebSocketMessage {
  type: 'search_progress'
  data: {
    searchId: string
    progress: number
    resultsFound: number
  }
}

// ==================== Teaching Document Types ====================

/**
 * Teaching document type enum
 */
export type TeachingDocumentType = 'LESSON' | 'PRACTICE' | 'REVIEW' | 'KNOWLEDGE_GAP' | 'CUSTOM'

/**
 * Teaching document status enum
 */
export type TeachingDocumentStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

/**
 * Teaching document entity from backend
 */
export interface TeachingDocument {
  id: number
  title: string
  userId: string
  documentType: TeachingDocumentType
  knowledgePointIds?: string
  testResultId?: number
  content: string
  metadata?: string
  status: TeachingDocumentStatus
  priority: number
  tags?: string
  createdAt: string
  updatedAt: string
  createdBy?: string
}

/**
 * Request to generate teaching document
 */
export interface GenerateTeachingRequest {
  documentType: TeachingDocumentType
  testResultId?: number
  knowledgePointIds: string[]
  title: string
}

/**
 * Request to generate personalized lesson
 */
export interface PersonalizedLessonRequest {
  testResultId: number
  weakPoints: string[]
}

/**
 * Teaching documents list response
 */
export interface TeachingDocumentsResponse {
  data: TeachingDocument[]
  total: number
  currentPage: number
  totalPages: number
  pageSize: number
  hasNext: boolean
  hasPrevious: boolean
  first: boolean
  last: boolean
}

/**
 * Teaching statistics
 */
export interface TeachingStats {
  total: number
  published: number
  draft: number
  byType: Record<string, number>
}

// ==================== Agent Task Types ====================

/**
 * Agent task type enum
 */
export type AgentTaskType =
  | 'KNOWLEDGE_EXTRACTION'
  | 'CONTENT_GENERATION'
  | 'DOCUMENT_ANALYSIS'
  | 'CODE_REVIEW'
  | 'TEST_GENERATION'
  | 'LEARNING_PATH'
  | 'KNOWLEDGE_GAP_ANALYSIS'
  | 'CUSTOM'

/**
 * Agent task status enum
 */
export type AgentTaskStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

/**
 * Agent task entity from backend
 */
export interface AgentTask {
  id: number
  taskName: string
  taskType: AgentTaskType
  userId: string
  config?: string
  status: AgentTaskStatus
  priority: number
  result?: string
  errorMessage?: string
  startedAt?: string
  completedAt?: string
  retryCount: number
  maxRetries: number
  nextRunAt?: string
  cronExpression?: string
  metadata?: string
  createdAt: string
  updatedAt: string
  createdBy?: string
}

/**
 * Request to create new task
 */
export interface CreateTaskRequest {
  taskName: string
  taskType: AgentTaskType
  config?: string
  priority?: number
}

/**
 * Request to create scheduled task
 */
export interface ScheduledTaskRequest {
  taskName: string
  taskType: AgentTaskType
  config?: string
  cronExpression: string
}

/**
 * Agent tasks list response
 */
export interface AgentTasksResponse {
  data: AgentTask[]
  total: number
  currentPage: number
  totalPages: number
  pageSize: number
  hasNext: boolean
  hasPrevious: boolean
  first: boolean
  last: boolean
}

/**
 * Agent task statistics
 */
export interface AgentTaskStats {
  pending: number
  running: number
  completed: number
  failed: number
  cancelled: number
}
