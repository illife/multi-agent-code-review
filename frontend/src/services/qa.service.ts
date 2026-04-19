import api from './api'
import type { ApiResponse } from '../types'
import type {
  AnswerDto,
  ChunkInfoDto,
  ChatHistoryItem,
} from '../types/knowledge-base'

/**
 * Q&A Service
 * Handles all question-answering related API operations
 */
export const qaService = {
  /**
   * Ask a question and get an AI-generated answer
   * Uses RAG (Retrieval-Augmented Generation) with knowledge base
   * @param question - The question to ask
   * @param sessionId - Optional session ID for conversation context
   * @returns Answer with sources and citations
   */
  async askQuestion(
    question: string,
    sessionId?: string
  ): Promise<ApiResponse<AnswerDto>> {
    try {
      const response = await api.post<ApiResponse<AnswerDto>>(
        '/qa/ask',
        { question, sessionId }
      )
      return response.data
    } catch (error) {
      console.error('Failed to ask question:', error)
      throw error
    }
  },

  /**
   * Retrieve relevant document chunks for a query
   * Useful for showing source context before generating full answer
   * @param question - The query/question
   * @returns Relevant chunks with scores
   */
  async retrieveChunks(question: string): Promise<ApiResponse<ChunkInfoDto[]>> {
    try {
      const response = await api.post<ApiResponse<ChunkInfoDto[]>>(
        '/qa/retrieve',
        { question }
      )
      return response.data
    } catch (error) {
      console.error('Failed to retrieve chunks:', error)
      throw error
    }
  },

  /**
   * Get chat history for a session
   * @param sessionId - Session ID
   * @returns Chat history items
   */
  async getChatHistory(sessionId: string): Promise<ApiResponse<ChatHistoryItem[]>> {
    try {
      const response = await api.get<ApiResponse<ChatHistoryItem[]>>(
        `/qa/history/${sessionId}`
      )
      return response.data
    } catch (error) {
      console.error(`Failed to get chat history for session ${sessionId}:`, error)
      throw error
    }
  },

  /**
   * Delete a chat session
   * @param sessionId - Session ID to delete
   * @returns Success message
   */
  async deleteSession(sessionId: string): Promise<ApiResponse<string>> {
    try {
      const response = await api.delete<ApiResponse<string>>(
        `/qa/session/${sessionId}`
      )
      return response.data
    } catch (error) {
      console.error(`Failed to delete session ${sessionId}:`, error)
      throw error
    }
  },

  /**
   * Submit feedback for a Q&A response
   * @param historyId - History item ID
   * @param feedback - Feedback rating (1-5)
   * @returns Success message
   */
  async submitFeedback(
    historyId: number,
    feedback: number
  ): Promise<ApiResponse<string>> {
    try {
      const response = await api.post<ApiResponse<string>>(
        `/qa/feedback/${historyId}`,
        { feedback }
      )
      return response.data
    } catch (error) {
      console.error(`Failed to submit feedback for ${historyId}:`, error)
      throw error
    }
  },
}

export default qaService
