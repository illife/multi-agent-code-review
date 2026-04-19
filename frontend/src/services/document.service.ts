import api from './api'
import type {
  ApiResponse,
  PaginatedResponse,
} from '../types'
import type {
  DocumentDto,
  UploadResponseDto,
  ReindexResponseDto,
  DocumentContentDto,
  DocumentStatus,
} from '../types/knowledge-base'

/**
 * Progress callback for file upload
 */
export type UploadProgressCallback = (progress: number) => void

/**
 * Document Service
 * Handles all document-related API operations
 */
export const documentService = {
  /**
   * Upload a document
   * @param file - The file to upload
   * @param title - Optional document title
   * @param description - Optional document description
   * @param fileMd5 - Optional file MD5 for instant upload
   * @param onProgress - Optional progress callback
   * @returns Upload response with document info
   */
  async uploadDocument(
    file: File,
    title?: string,
    description?: string,
    fileMd5?: string,
    onProgress?: UploadProgressCallback
  ): Promise<ApiResponse<UploadResponseDto>> {
    try {
      const formData = new FormData()
      formData.append('file', file)
      if (title) formData.append('title', title)
      if (description) formData.append('description', description)
      if (fileMd5) formData.append('fileMd5', fileMd5)

      const response = await api.post<ApiResponse<UploadResponseDto>>(
        '/documents/upload',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
          onUploadProgress: (progressEvent) => {
            if (onProgress && progressEvent.total) {
              const progress = Math.round(
                (progressEvent.loaded * 100) / progressEvent.total
              )
              onProgress(progress)
            }
          },
        }
      )

      return response.data
    } catch (error) {
      console.error('Document upload failed:', error)
      throw error
    }
  },

  /**
   * Get documents list with pagination
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @returns Paginated document list
   */
  async getDocuments(
    page: number = 0,
    size: number = 10
  ): Promise<ApiResponse<PaginatedResponse<DocumentDto>>> {
    try {
      const response = await api.get<ApiResponse<PaginatedResponse<DocumentDto>>>(
        '/documents',
        { params: { page, size } }
      )
      return response.data
    } catch (error) {
      console.error('Failed to get documents:', error)
      throw error
    }
  },

  /**
   * Get current user's documents (alias endpoint)
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @returns Paginated document list
   */
  async getMyDocuments(
    page: number = 0,
    size: number = 10
  ): Promise<ApiResponse<PaginatedResponse<DocumentDto>>> {
    try {
      const response = await api.get<ApiResponse<PaginatedResponse<DocumentDto>>>(
        '/documents/my',
        { params: { page, size } }
      )
      return response.data
    } catch (error) {
      console.error('Failed to get my documents:', error)
      throw error
    }
  },

  /**
   * Get document by ID
   * @param documentId - Document ID
   * @returns Document details
   */
  async getDocument(documentId: number): Promise<ApiResponse<DocumentDto>> {
    try {
      const response = await api.get<ApiResponse<DocumentDto>>(
        `/documents/${documentId}`
      )
      return response.data
    } catch (error) {
      console.error(`Failed to get document ${documentId}:`, error)
      throw error
    }
  },

  /**
   * Search documents by keyword
   * @param keyword - Search keyword
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @returns Paginated search results
   */
  async searchDocuments(
    keyword: string,
    page: number = 0,
    size: number = 10
  ): Promise<ApiResponse<PaginatedResponse<DocumentDto>>> {
    try {
      const response = await api.get<ApiResponse<PaginatedResponse<DocumentDto>>>(
        '/documents/search',
        { params: { keyword, page, size } }
      )
      return response.data
    } catch (error) {
      console.error('Failed to search documents:', error)
      throw error
    }
  },

  /**
   * Delete document by ID
   * @param documentId - Document ID
   * @returns Success message
   */
  async deleteDocument(documentId: number): Promise<ApiResponse<string>> {
    try {
      const response = await api.delete<ApiResponse<string>>(
        `/documents/${documentId}`
      )
      return response.data
    } catch (error) {
      console.error(`Failed to delete document ${documentId}:`, error)
      throw error
    }
  },

  /**
   * Update document status
   * @param documentId - Document ID
   * @param status - New status
   * @returns Success message
   */
  async updateDocumentStatus(
    documentId: number,
    status: DocumentStatus
  ): Promise<ApiResponse<string>> {
    try {
      const response = await api.put<ApiResponse<string>>(
        `/documents/${documentId}/status`,
        null,
        { params: { status } }
      )
      return response.data
    } catch (error) {
      console.error(`Failed to update document status ${documentId}:`, error)
      throw error
    }
  },

  /**
   * Download document
   * @param documentId - Document ID
   * @returns Blob with file content
   */
  async downloadDocument(documentId: number): Promise<Blob> {
    try {
      const response = await api.get<Blob>(
        `/documents/${documentId}/download`,
        { responseType: 'blob' }
      )
      return response.data
    } catch (error) {
      console.error(`Failed to download document ${documentId}:`, error)
      throw error
    }
  },

  /**
   * Preview document (for browser-displayable files)
   * @param documentId - Document ID
   * @returns Blob with file content
   */
  async previewDocument(documentId: number): Promise<Blob> {
    try {
      const response = await api.get<Blob>(
        `/documents/${documentId}/preview`,
        { responseType: 'blob' }
      )
      return response.data
    } catch (error) {
      console.error(`Failed to preview document ${documentId}:`, error)
      throw error
    }
  },

  /**
   * Get document text content
   * @param documentId - Document ID
   * @returns Document content metadata
   */
  async getDocumentContent(documentId: number): Promise<ApiResponse<DocumentContentDto>> {
    try {
      const response = await api.get<ApiResponse<DocumentContentDto>>(
        `/documents/${documentId}/content`
      )
      return response.data
    } catch (error) {
      console.error(`Failed to get document content ${documentId}:`, error)
      throw error
    }
  },

  /**
   * Reindex all documents
   * @returns Reindex summary
   */
  async reindexAllDocuments(): Promise<ApiResponse<ReindexResponseDto>> {
    try {
      const response = await api.post<ApiResponse<ReindexResponseDto>>(
        '/documents/reindex'
      )
      return response.data
    } catch (error) {
      console.error('Failed to reindex all documents:', error)
      throw error
    }
  },

  /**
   * Reindex a single document
   * @param documentId - Document ID
   * @returns Success message
   */
  async reindexDocument(documentId: number): Promise<ApiResponse<string>> {
    try {
      const response = await api.post<ApiResponse<string>>(
        `/documents/${documentId}/reindex`
      )
      return response.data
    } catch (error) {
      console.error(`Failed to reindex document ${documentId}:`, error)
      throw error
    }
  },
}

export default documentService
