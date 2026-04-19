import api from './api'
import SparkMD5 from 'spark-md5'
import type {
  ApiResponse,
  PaginatedResponse,
  Document,
  DocumentChunk,
  SearchRequest,
  QARequest,
  QAResponse,
  HybridSearchResponseDto,
} from '../types'

/**
 * Calculate MD5 hash of a file using SparkMD5
 * Uses chunked reading to handle large files efficiently
 */
async function calculateMD5(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const spark = new SparkMD5.ArrayBuffer()
    const fileReader = new FileReader()
    const chunkSize = 2 * 1024 * 1024 // 2MB chunks
    let offset = 0

    fileReader.onload = (e) => {
      if (e.target?.result) {
        spark.append(e.target.result as ArrayBuffer)
        offset += chunkSize

        if (offset < file.size) {
          readNextChunk()
        } else {
          resolve(spark.end())
        }
      }
    }

    fileReader.onerror = () => {
      reject(new Error('Failed to read file for MD5 calculation'))
    }

    function readNextChunk() {
      const end = Math.min(offset + chunkSize, file.size)
      fileReader.readAsArrayBuffer(file.slice(offset, end))
    }

    readNextChunk()
  })
}

export const knowledgeService = {
  /**
   * Upload a document
   */
  async uploadDocument(file: File, onProgress?: (progress: number) => void): Promise<ApiResponse<Document>> {
    // Calculate MD5 hash for instant upload check
    const fileMd5 = await calculateMD5(file)

    const formData = new FormData()
    formData.append('file', file)
    formData.append('fileMd5', fileMd5)

    const response = await api.post<ApiResponse<Document>>('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(progress)
        }
      },
    })
    return response.data
  },

  /**
   * Get document list with pagination
   */
  async getDocuments(page: number = 0, size: number = 20): Promise<ApiResponse<PaginatedResponse<Document>>> {
    const response = await api.get<ApiResponse<PaginatedResponse<Document>>>('/documents', {
      params: { page, size },
    })
    return response.data
  },

  /**
   * Get document by ID
   */
  async getDocument(documentId: number): Promise<ApiResponse<Document>> {
    const response = await api.get<ApiResponse<Document>>(`/documents/${documentId}`)
    return response.data
  },

  /**
   * Get document chunks
   */
  async getDocumentChunks(documentId: number, page: number = 0, size: number = 20): Promise<ApiResponse<PaginatedResponse<DocumentChunk>>> {
    const response = await api.get<ApiResponse<PaginatedResponse<DocumentChunk>>>(`/documents/${documentId}/chunks`, {
      params: { page, size },
    })
    return response.data
  },

  /**
   * Delete document
   */
  async deleteDocument(documentId: number): Promise<ApiResponse<void>> {
    const response = await api.delete<ApiResponse<void>>(`/documents/${documentId}`)
    return response.data
  },

  /**
   * Re-index document
   */
  async reindexDocument(documentId: number): Promise<ApiResponse<void>> {
    const response = await api.post<ApiResponse<void>>(`/documents/${documentId}/reindex`)
    return response.data
  },

  /**
   * Download document
   */
  async downloadDocument(documentId: number): Promise<Blob> {
    const response = await api.get(`/documents/${documentId}/download`, {
      responseType: 'blob',
    })
    return response.data
  },

  /**
   * Search documents
   */
  async search(request: SearchRequest): Promise<ApiResponse<HybridSearchResponseDto>> {
    const response = await api.post<ApiResponse<HybridSearchResponseDto>>('/search', request)
    return response.data
  },

  /**
   * Ask a question
   */
  async askQuestion(request: QARequest): Promise<ApiResponse<QAResponse>> {
    const response = await api.post<ApiResponse<QAResponse>>('/qa/ask', request)
    return response.data
  },

  /**
   * Get chat history
   */
  async getChatHistory(sessionId?: string): Promise<ApiResponse<any[]>> {
    const response = await api.get<ApiResponse<any[]>>('/qa/history', {
      params: sessionId ? { sessionId } : {},
    })
    return response.data
  },

  /**
   * Delete chat session
   */
  async deleteChatSession(sessionId: string): Promise<ApiResponse<void>> {
    const response = await api.delete<ApiResponse<void>>(`/qa/sessions/${sessionId}`)
    return response.data
  },
}
