import api from './api'
import type {
  ApiResponse,
  ProjectUploadResponse,
  ProjectUploadSession,
  ProjectInfo,
  ProjectStatusDTO,
  ProjectFile,
  ProjectReport,
  ArchitectureReport,
  InfrastructureUsage,
  ArchitectureRecommendations,
} from '../types'

export const PROJECT_UPLOAD_CHUNK_SIZE = 1 * 1024 * 1024

export type ProjectUploadStage = 'initializing' | 'uploading' | 'completing'

export interface ProjectUploadProgress {
  stage: ProjectUploadStage
  percent: number
  uploadedChunks: number
  totalChunks: number
  projectId?: number
}

const uploadChunkWithRetry = async (
  formData: FormData,
  params: {
    projectId: number
    uploadId: string
    chunkIndex: number
    totalChunks: number
  },
  maxAttempts = 5
) => {
  let lastError: unknown

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      const response = await api.post<ApiResponse<string>>('/project/upload/chunk', formData, {
        params,
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 180000,
      })

      if (response.data.code !== 200) {
        throw new Error(response.data.message || '分片上传失败')
      }

      return
    } catch (error) {
      lastError = error
      if (attempt < maxAttempts) {
        await new Promise((resolve) => setTimeout(resolve, attempt * 1500))
      }
    }
  }

  const message = lastError instanceof Error ? lastError.message : '网络连接中断'
  throw new Error(`第 ${params.chunkIndex + 1}/${params.totalChunks} 个分片上传失败：${message}`)
}

export const projectService = {
  /**
   * Upload ZIP project file in chunks. The init step creates the project record first,
   * so the project list can show a pending item before all bytes finish uploading.
   */
  async uploadProject(
    file: File,
    projectName: string,
    description?: string,
    visibility = 'PRIVATE',
    onProgress?: (progress: ProjectUploadProgress) => void
  ): Promise<ApiResponse<ProjectUploadResponse>> {
    const totalChunks = Math.ceil(file.size / PROJECT_UPLOAD_CHUNK_SIZE)

    onProgress?.({
      stage: 'initializing',
      percent: 0,
      uploadedChunks: 0,
      totalChunks,
    })

    const initResponse = await api.post<ApiResponse<ProjectUploadSession>>('/project/upload/chunk/init', {
      projectName,
      description,
      visibility,
      fileName: file.name,
      fileSize: file.size,
      totalChunks,
      chunkSize: PROJECT_UPLOAD_CHUNK_SIZE,
    }, {
      timeout: 30000,
    })

    if (initResponse.data.code !== 200 || !initResponse.data.data) {
      throw new Error(initResponse.data.message || '初始化上传失败')
    }

    const session = initResponse.data.data
    onProgress?.({
      stage: 'uploading',
      percent: 0,
      uploadedChunks: 0,
      totalChunks,
      projectId: session.projectId,
    })

    for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex += 1) {
      const start = chunkIndex * PROJECT_UPLOAD_CHUNK_SIZE
      const end = Math.min(start + PROJECT_UPLOAD_CHUNK_SIZE, file.size)
      const chunk = file.slice(start, end)

      const formData = new FormData()
      formData.append('chunk', chunk, `${file.name}.part${chunkIndex}`)

      await uploadChunkWithRetry(formData, {
        projectId: session.projectId,
        uploadId: session.uploadId,
        chunkIndex,
        totalChunks,
      })

      onProgress?.({
        stage: 'uploading',
        percent: Math.round(((chunkIndex + 1) / totalChunks) * 100),
        uploadedChunks: chunkIndex + 1,
        totalChunks,
        projectId: session.projectId,
      })
    }

    onProgress?.({
      stage: 'completing',
      percent: 100,
      uploadedChunks: totalChunks,
      totalChunks,
      projectId: session.projectId,
    })

    const completeResponse = await api.post<ApiResponse<ProjectUploadResponse>>('/project/upload/chunk/complete', null, {
      params: {
        projectId: session.projectId,
        uploadId: session.uploadId,
        fileName: file.name,
        totalChunks,
      },
      timeout: 300000,
    })

    return completeResponse.data
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
