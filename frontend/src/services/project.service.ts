import api from './api'
import type {
  ApiResponse,
  ProjectUploadResponse,
  ProjectInfo,
  ProjectStatusDTO,
  ProjectFile,
  ProjectReport,
  ArchitectureReport,
  InfrastructureUsage,
  ArchitectureRecommendations,
} from '../types'

export const projectService = {
  /**
   * Upload ZIP project file
   */
  async uploadProject(
    file: File,
    projectName: string,
    description?: string,
    visibility = 'PRIVATE'
  ): Promise<ApiResponse<ProjectUploadResponse>> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('projectName', projectName)
    if (description) formData.append('description', description)
    formData.append('visibility', visibility)
    const response = await api.post<ApiResponse<ProjectUploadResponse>>('/project/upload/zip', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
  },

  /**
   * Get project status
   */
  async getProjectStatus(projectId: number): Promise<ApiResponse<ProjectStatusDTO>> {
    const response = await api.get<ApiResponse<ProjectStatusDTO>>(`/project/${projectId}/status`)
    return response.data
  },

  /**
   * Get project files
   */
  async getProjectFiles(projectId: number, page = 0, size = 20): Promise<ApiResponse<ProjectFile[]>> {
    const response = await api.get<ApiResponse<ProjectFile[]>>(`/project/${projectId}/files`, {
      params: { page, size },
    })
    return response.data
  },

  /**
   * Get project report
   */
  async getProjectReport(projectId: number): Promise<ApiResponse<ProjectReport>> {
    const response = await api.get<ApiResponse<ProjectReport>>(`/project/${projectId}/report`)
    return response.data
  },

  /**
   * Delete project
   */
  async deleteProject(projectId: number): Promise<ApiResponse<string>> {
    const response = await api.delete<ApiResponse<string>>(`/project/${projectId}`)
    return response.data
  },

  /**
   * Get user's project list
   */
  async getProjectList(page = 0, size = 10): Promise<ApiResponse<ProjectInfo[]>> {
    const response = await api.get<ApiResponse<ProjectInfo[]>>('/project/list', {
      params: { page, size },
    })
    return response.data
  },

  /**
   * Generate project report
   */
  async generateReport(projectId: number): Promise<ApiResponse<string>> {
    const response = await api.post<ApiResponse<string>>(`/project/${projectId}/generate-report`)
    return response.data
  },

  /**
   * Get architecture analysis
   */
  async getArchitectureAnalysis(): Promise<ApiResponse<ArchitectureReport>> {
    const response = await api.get<ApiResponse<ArchitectureReport>>('/architecture/analyze')
    return response.data
  },

  /**
   * Get infrastructure usage
   */
  async getInfrastructureUsage(): Promise<ApiResponse<InfrastructureUsage>> {
    const response = await api.get<ApiResponse<InfrastructureUsage>>('/architecture/infrastructure')
    return response.data
  },

  /**
   * Get architecture recommendations
   */
  async getArchitectureRecommendations(): Promise<ApiResponse<ArchitectureRecommendations>> {
    const response = await api.get<ApiResponse<ArchitectureRecommendations>>('/architecture/recommendations')
    return response.data
  },
}
