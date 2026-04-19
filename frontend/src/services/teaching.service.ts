import api from './api'
import type {
  ApiResponse,
  TeachingDocument,
  GenerateTeachingRequest,
  PersonalizedLessonRequest,
  TeachingDocumentsResponse,
  TeachingStats,
} from '../types'

export const teachingService = {
  /**
   * 生成教学文档
   */
  async generateTeachingDocument(request: GenerateTeachingRequest): Promise<ApiResponse<TeachingDocument>> {
    const response = await api.post<ApiResponse<TeachingDocument>>('/teaching/generate', request)
    return response.data
  },

  /**
   * 生成个性化教学文档（基于薄弱知识点）
   */
  async generatePersonalizedLesson(request: PersonalizedLessonRequest): Promise<ApiResponse<TeachingDocument>> {
    const response = await api.post<ApiResponse<TeachingDocument>>('/teaching/personalized', request)
    return response.data
  },

  /**
   * 获取用户教学文档列表（分页）
   */
  async getUserDocuments(page = 0, size = 10): Promise<ApiResponse<TeachingDocumentsResponse>> {
    const response = await api.get<ApiResponse<TeachingDocumentsResponse>>('/teaching/documents', {
      params: { page, size },
    })
    return response.data
  },

  /**
   * 获取已发布的教学文档
   */
  async getPublishedDocuments(): Promise<ApiResponse<TeachingDocument[]>> {
    const response = await api.get<ApiResponse<TeachingDocument[]>>('/teaching/published')
    return response.data
  },

  /**
   * 搜索教学文档
   */
  async searchDocuments(keyword: string): Promise<ApiResponse<TeachingDocument[]>> {
    const response = await api.get<ApiResponse<TeachingDocument[]>>('/teaching/search', {
      params: { keyword },
    })
    return response.data
  },

  /**
   * 删除教学文档
   */
  async deleteDocument(documentId: number): Promise<ApiResponse<string>> {
    const response = await api.delete<ApiResponse<string>>(`/teaching/documents/${documentId}`)
    return response.data
  },

  /**
   * 获取教学文档统计信息
   */
  async getStats(): Promise<ApiResponse<TeachingStats>> {
    const response = await api.get<ApiResponse<TeachingStats>>('/teaching/stats')
    return response.data
  },
}
