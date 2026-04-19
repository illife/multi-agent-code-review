import api from './api'
import type {
  ApiResponse,
  CodeReviewRequest,
  CodeReviewResponse,
  ReviewIssue,
  AgentExecution,
} from '../types'

export const codeReviewService = {
  /**
   * Submit code for review (async)
   * Returns the review ID as a number
   */
  async submitReview(data: CodeReviewRequest): Promise<ApiResponse<number>> {
    const response = await api.post<ApiResponse<number>>('/review/submit-async', data)
    return response.data
  },

  /**
   * Get review status
   */
  async getReviewStatus(reviewId: number): Promise<ApiResponse<CodeReviewResponse>> {
    const response = await api.get<ApiResponse<CodeReviewResponse>>(`/review/${reviewId}`)
    return response.data
  },

  /**
   * Get review issues
   */
  async getReviewIssues(reviewId: number): Promise<ApiResponse<ReviewIssue[]>> {
    const response = await api.get<ApiResponse<ReviewIssue[]>>(`/review/${reviewId}/issues`)
    return response.data
  },

  /**
   * Get agent executions
   */
  async getAgentExecutions(reviewId: number): Promise<ApiResponse<AgentExecution[]>> {
    const response = await api.get<ApiResponse<AgentExecution[]>>(`/review/${reviewId}/agents`)
    return response.data
  },

  /**
   * Get review detail with teaching report
   */
  async getReviewDetail(reviewId: number): Promise<ApiResponse<any>> {
    const response = await api.get<ApiResponse<any>>(`/review/${reviewId}/detail`)
    return response.data
  },

  /**
   * Download teaching report as Markdown
   */
  async downloadTeachingReport(reviewId: number): Promise<Blob> {
    const response = await api.get(`/review/${reviewId}/teaching-report/download`, {
      responseType: 'blob',
    })
    return response.data
  },

  /**
   * Download review report as PDF
   */
  async downloadReviewReport(reviewId: number): Promise<Blob> {
    const response = await api.get(`/review/${reviewId}/report/pdf`, {
      responseType: 'blob',
    })
    return response.data
  },

  /**
   * Get recent reviews
   */
  async getRecentReviews(): Promise<ApiResponse<CodeReviewResponse[]>> {
    const response = await api.get<ApiResponse<CodeReviewResponse[]>>('/reviews')
    return response.data
  },
}
