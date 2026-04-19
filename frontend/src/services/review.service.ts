import api from './api'
import type {
  ApiResponse,
  PaginatedResponse,
  CodeReviewRequest,
  CodeReviewResponse,
  ReviewIssue,
} from '../types'

export const reviewService = {
  /**
   * Submit code for review (synchronous)
   */
  async submitReview(data: CodeReviewRequest): Promise<ApiResponse<CodeReviewResponse>> {
    const response = await api.post<ApiResponse<CodeReviewResponse>>('/review/submit', data)
    return response.data
  },

  /**
   * Submit code for review (asynchronous)
   */
  async submitReviewAsync(data: CodeReviewRequest): Promise<ApiResponse<CodeReviewResponse>> {
    const response = await api.post<ApiResponse<CodeReviewResponse>>('/review/submit-async', data)
    return response.data
  },

  /**
   * Get review details
   */
  async getReview(reviewId: string): Promise<ApiResponse<CodeReviewResponse>> {
    const response = await api.get<ApiResponse<CodeReviewResponse>>(`/review/${reviewId}`)
    return response.data
  },

  /**
   * Get user's review list
   */
  async getReviewList(page = 0, size = 10): Promise<ApiResponse<PaginatedResponse<CodeReviewResponse>>> {
    const response = await api.get<ApiResponse<PaginatedResponse<CodeReviewResponse>>>('/review/list', {
      params: { page, size },
    })
    return response.data
  },

  /**
   * Get review issues
   */
  async getReviewIssues(reviewId: string): Promise<ApiResponse<ReviewIssue[]>> {
    const response = await api.get<ApiResponse<ReviewIssue[]>>(`/review/${reviewId}/issues`)
    return response.data
  },

  /**
   * Delete review
   */
  async deleteReview(reviewId: string): Promise<ApiResponse<void>> {
    const response = await api.delete<ApiResponse<void>>(`/review/${reviewId}`)
    return response.data
  },
}
