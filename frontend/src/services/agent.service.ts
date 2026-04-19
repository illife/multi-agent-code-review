import api from './api'
import type {
  ApiResponse,
  AgentTask,
  CreateTaskRequest,
  ScheduledTaskRequest,
  AgentTasksResponse,
  AgentTaskStats,
} from '../types'

export const agentService = {
  /**
   * 创建新任务
   */
  async createTask(request: CreateTaskRequest): Promise<ApiResponse<AgentTask>> {
    const response = await api.post<ApiResponse<AgentTask>>('/agent-tasks/create', request)
    return response.data
  },

  /**
   * 创建定时任务
   */
  async createScheduledTask(request: ScheduledTaskRequest): Promise<ApiResponse<AgentTask>> {
    const response = await api.post<ApiResponse<AgentTask>>('/agent-tasks/scheduled', request)
    return response.data
  },

  /**
   * 手动执行任务
   */
  async executeTask(taskId: number): Promise<ApiResponse<string>> {
    const response = await api.post<ApiResponse<string>>(`/agent-tasks/${taskId}/execute`)
    return response.data
  },

  /**
   * 重试失败任务
   */
  async retryTask(taskId: number): Promise<ApiResponse<AgentTask>> {
    const response = await api.post<ApiResponse<AgentTask>>(`/agent-tasks/${taskId}/retry`)
    return response.data
  },

  /**
   * 取消任务
   */
  async cancelTask(taskId: number): Promise<ApiResponse<string>> {
    const response = await api.post<ApiResponse<string>>(`/agent-tasks/${taskId}/cancel`)
    return response.data
  },

  /**
   * 获取用户任务列表（分页）
   */
  async getTasks(page = 0, size = 10): Promise<ApiResponse<AgentTasksResponse>> {
    const response = await api.get<ApiResponse<AgentTasksResponse>>('/agent-tasks', {
      params: { page, size },
    })
    return response.data
  },

  /**
   * 获取任务详情
   */
  async getTask(taskId: number): Promise<ApiResponse<AgentTask>> {
    const response = await api.get<ApiResponse<AgentTask>>(`/agent-tasks/${taskId}`)
    return response.data
  },

  /**
   * 搜索任务
   */
  async searchTasks(keyword: string, page = 0, size = 10): Promise<ApiResponse<AgentTasksResponse>> {
    const response = await api.get<ApiResponse<AgentTasksResponse>>('/agent-tasks/search', {
      params: { keyword, page, size },
    })
    return response.data
  },

  /**
   * 获取任务统计信息
   */
  async getStats(): Promise<ApiResponse<AgentTaskStats>> {
    const response = await api.get<ApiResponse<AgentTaskStats>>('/agent-tasks/stats')
    return response.data
  },

  /**
   * 获取待执行任务列表（管理员）
   */
  async getPendingTasks(): Promise<ApiResponse<AgentTask[]>> {
    const response = await api.get<ApiResponse<AgentTask[]>>('/agent-tasks/pending')
    return response.data
  },

  /**
   * 删除任务
   */
  async deleteTask(taskId: number): Promise<ApiResponse<string>> {
    const response = await api.delete<ApiResponse<string>>(`/agent-tasks/${taskId}`)
    return response.data
  },
}
