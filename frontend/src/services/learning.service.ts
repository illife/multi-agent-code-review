import api from './api'
import type {
  ApiResponse,
  LearningPath,
  Exercise,
  ExerciseSubmission,
  LearningProgress,
} from '../types'

export const learningService = {
  /**
   * Get all learning paths
   */
  async getLearningPaths(): Promise<ApiResponse<LearningPath[]>> {
    const response = await api.get<ApiResponse<LearningPath[]>>('/learning/paths')
    return response.data
  },

  /**
   * Get learning path details
   */
  async getLearningPath(pathId: string): Promise<ApiResponse<LearningPath>> {
    const response = await api.get<ApiResponse<LearningPath>>(`/learning/paths/${pathId}`)
    return response.data
  },

  /**
   * Get exercises
   */
  async getExercises(filters?: { difficulty?: string; language?: string }): Promise<ApiResponse<Exercise[]>> {
    const params = new URLSearchParams()
    if (filters?.difficulty) params.append('difficulty', filters.difficulty)
    if (filters?.language) params.append('language', filters.language)

    const response = await api.get<ApiResponse<Exercise[]>>(`/learning/exercises?${params}`)
    return response.data
  },

  /**
   * Get exercise details
   */
  async getExercise(exerciseId: string): Promise<ApiResponse<Exercise>> {
    const response = await api.get<ApiResponse<Exercise>>(`/learning/exercises/${exerciseId}`)
    return response.data
  },

  /**
   * Submit exercise solution
   */
  async submitExercise(exerciseId: string, code: string): Promise<ApiResponse<ExerciseSubmission>> {
    const response = await api.post<ApiResponse<ExerciseSubmission>>(`/learning/exercises/${exerciseId}/submit`, { code })
    return response.data
  },

  /**
   * Get learning progress
   */
  async getLearningProgress(): Promise<ApiResponse<LearningProgress>> {
    const response = await api.get<ApiResponse<LearningProgress>>('/learning/progress')
    return response.data
  },

  /**
   * Start learning path
   */
  async startLearningPath(pathId: string): Promise<ApiResponse<void>> {
    const response = await api.post<ApiResponse<void>>(`/learning/paths/${pathId}/start`, {})
    return response.data
  },

  /**
   * Complete module
   */
  async completeModule(moduleId: string): Promise<ApiResponse<void>> {
    const response = await api.post<ApiResponse<void>>(`/learning/modules/${moduleId}/complete`, {})
    return response.data
  },
}
