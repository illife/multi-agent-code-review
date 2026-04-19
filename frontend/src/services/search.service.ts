import api from './api'
import type { ApiResponse } from '../types'
import type {
  SearchRequestDto,
  SearchHitDto,
  HybridSearchResponseDto,
  SearchSuggestionDto,
} from '../types/knowledge-base'

/**
 * Search Service
 * Handles all search-related API operations including BM25, KNN, and hybrid search
 */
export const searchService = {
  /**
   * Perform BM25 keyword search
   * @param request - Search request with query and filters
   * @returns Search results
   */
  async searchByBM25(request: SearchRequestDto): Promise<ApiResponse<SearchHitDto[]>> {
    try {
      const response = await api.post<ApiResponse<SearchHitDto[]>>(
        '/search/bm25',
        request
      )
      return response.data
    } catch (error) {
      console.error('BM25 search failed:', error)
      throw error
    }
  },

  /**
   * Perform KNN vector search
   * @param request - Search request with query and filters
   * @returns Search results
   */
  async searchByKNN(request: SearchRequestDto): Promise<ApiResponse<SearchHitDto[]>> {
    try {
      const response = await api.post<ApiResponse<SearchHitDto[]>>(
        '/search/knn',
        request
      )
      return response.data
    } catch (error) {
      console.error('KNN search failed:', error)
      throw error
    }
  },

  /**
   * Perform hybrid search (BM25 + KNN with RRF fusion)
   * This is the recommended search method as it combines keyword and semantic search
   * @param request - Search request with query, filters, and pagination
   * @returns Hybrid search results with pagination info
   */
  async hybridSearch(
    request: SearchRequestDto
  ): Promise<ApiResponse<HybridSearchResponseDto>> {
    try {
      const response = await api.post<ApiResponse<HybridSearchResponseDto>>(
        '/search/hybrid',
        request
      )
      return response.data
    } catch (error) {
      console.error('Hybrid search failed:', error)
      throw error
    }
  },

  /**
   * Simple hybrid search with minimal parameters
   * @param query - Search query string
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @param filters - Optional filters (departments, tags, fileTypes, dateRange)
   * @returns Hybrid search results
   */
  async search(
    query: string,
    page: number = 0,
    size: number = 10,
    filters?: {
      departments?: string[]
      tags?: string[]
      fileTypes?: string[]
      dateFrom?: string
      dateTo?: string
    }
  ): Promise<ApiResponse<HybridSearchResponseDto>> {
    return this.hybridSearch({
      query,
      page,
      size,
      ...filters,
    })
  },

  /**
   * Get search suggestions based on prefix
   * @param prefix - Query prefix
   * @returns Search suggestions
   */
  async getSuggestions(prefix: string): Promise<ApiResponse<SearchSuggestionDto[]>> {
    try {
      const response = await api.get<ApiResponse<SearchSuggestionDto[]>>(
        '/search/suggestions',
        { params: { prefix } }
      )
      return response.data
    } catch (error) {
      console.error('Failed to get suggestions:', error)
      throw error
    }
  },
}

export default searchService
