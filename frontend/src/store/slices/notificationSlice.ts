import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import type { PayloadAction } from '@reduxjs/toolkit'
import type { RootState } from '../store'

export interface Notification {
  id: string
  type: 'success' | 'info' | 'warning' | 'error'
  title: string
  message: string
  link?: string
  read: boolean
  createdAt: string
}

interface NotificationState {
  notifications: Notification[]
  unreadCount: number
}

const initialState: NotificationState = {
  notifications: [],
  unreadCount: 0,
}

// 模拟添加通知
export const addNotification = createAsyncThunk(
  'notifications/add',
  async (notification: Omit<Notification, 'id' | 'read' | 'createdAt'>) => {
    return {
      ...notification,
      id: Date.now().toString(),
      read: false,
      createdAt: new Date().toISOString(),
    }
  }
)

// 标记通知为已读
export const markAsRead = createAsyncThunk(
  'notifications/markAsRead',
  async (id: string) => {
    return id
  }
)

// 标记所有通知为已读
export const markAllAsRead = createAsyncThunk(
  'notifications/markAllAsRead',
  async () => {
    return true
  }
)

// 删除通知
export const removeNotification = createAsyncThunk(
  'notifications/remove',
  async (id: string) => {
    return id
  }
)

const notificationSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    // 模拟从WebSocket接收到的通知
    reviewCompleted: (state, action: PayloadAction<{ reviewId: number; fileName: string }>) => {
      const notification: Notification = {
        id: Date.now().toString(),
        type: 'success',
        title: '代码审查完成',
        message: `文件 "${action.payload.fileName}" 的代码审查已完成`,
        link: `/review?id=${action.payload.reviewId}`,
        read: false,
        createdAt: new Date().toISOString(),
      }
      state.notifications.unshift(notification)
      state.unreadCount++
    },
    teachingReportGenerated: (state, action: PayloadAction<{ reviewId: number }>) => {
      const notification: Notification = {
        id: Date.now().toString(),
        type: 'info',
        title: '教学报告已生成',
        message: '您的个性化教学报告已生成完成',
        link: `/review?id=${action.payload.reviewId}`,
        read: false,
        createdAt: new Date().toISOString(),
      }
      state.notifications.unshift(notification)
      state.unreadCount++
    },
    clearNotifications: (state) => {
      state.notifications = []
      state.unreadCount = 0
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(addNotification.fulfilled, (state, action) => {
        state.notifications.unshift(action.payload)
        state.unreadCount++
      })
      .addCase(markAsRead.fulfilled, (state, action) => {
        const notification = state.notifications.find(n => n.id === action.payload)
        if (notification && !notification.read) {
          notification.read = true
          state.unreadCount = Math.max(0, state.unreadCount - 1)
        }
      })
      .addCase(markAllAsRead.fulfilled, (state) => {
        state.notifications.forEach(n => n.read = true)
        state.unreadCount = 0
      })
      .addCase(removeNotification.fulfilled, (state, action) => {
        const index = state.notifications.findIndex(n => n.id === action.payload)
        if (index !== -1) {
          const notification = state.notifications[index]
          if (!notification.read) {
            state.unreadCount = Math.max(0, state.unreadCount - 1)
          }
          state.notifications.splice(index, 1)
        }
      })
  },
})

export const {
  reviewCompleted,
  teachingReportGenerated,
  clearNotifications,
} = notificationSlice.actions

export const selectNotifications = (state: RootState) => state.notifications.notifications
export const selectUnreadCount = (state: RootState) => state.notifications.unreadCount

export default notificationSlice.reducer
