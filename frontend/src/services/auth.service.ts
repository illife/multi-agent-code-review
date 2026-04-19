import api from './api'
import type {
  ApiResponse,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  UserInfo,
} from '../types'

export const authService = {
  /**
   * User login
   */
  async login(data: LoginRequest): Promise<ApiResponse<AuthResponse>> {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', data)
    return response.data
  },

  /**
   * User registration
   */
  async register(data: RegisterRequest): Promise<ApiResponse<AuthResponse>> {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/register', data)
    return response.data
  },

  /**
   * Validate token
   */
  async validateToken(): Promise<ApiResponse<boolean>> {
    const response = await api.get<ApiResponse<boolean>>('/auth/validate')
    return response.data
  },

  /**
   * Refresh token
   */
  async refreshToken(refreshToken: string): Promise<ApiResponse<AuthResponse>> {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken })
    return response.data
  },

  /**
   * Get current user info
   */
  async getCurrentUser(): Promise<ApiResponse<UserInfo>> {
    const response = await api.get<ApiResponse<UserInfo>>('/auth/me')
    return response.data
  },

  /**
   * Update profile
   */
  async updateProfile(fullName: string): Promise<ApiResponse<UserInfo>> {
    const response = await api.put<ApiResponse<UserInfo>>('/auth/profile', { fullName })
    return response.data
  },

  /**
   * Change password
   */
  async changePassword(currentPassword: string, newPassword: string): Promise<ApiResponse<void>> {
    const response = await api.post<ApiResponse<void>>('/auth/change-password', {
      currentPassword,
      newPassword,
    })
    return response.data
  },

  /**
   * Logout
   */
  async logout(): Promise<ApiResponse<void>> {
    const response = await api.post<ApiResponse<void>>('/auth/logout')
    return response.data
  },
}
