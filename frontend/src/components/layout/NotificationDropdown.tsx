import React, { useState, useRef, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { useNavigate } from 'react-router-dom'
import type { AppDispatch } from '../../store/store'
import { Bell, Check, CheckCheck, Trash2, X } from 'lucide-react'
import { markAsRead, markAllAsRead, removeNotification, selectNotifications, selectUnreadCount } from '../../store/slices/notificationSlice'
import type { Notification } from '../../store/slices/notificationSlice'
import { cn } from '../../lib/utils'

const NotificationDropdown: React.FC = () => {
  const dispatch = useDispatch<AppDispatch>()
  const navigate = useNavigate()
  const notifications = useSelector(selectNotifications)
  const unreadCount = useSelector(selectUnreadCount)
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)

  // 点击外部关闭下拉菜单
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleNotificationClick = (notification: Notification) => {
    if (!notification.read) {
      dispatch(markAsRead(notification.id))
    }
    if (notification.link) {
      navigate(notification.link)
    }
    setIsOpen(false)
  }

  const handleMarkAllAsRead = () => {
    dispatch(markAllAsRead())
  }

  const handleClearAll = () => {
    notifications.forEach(n => dispatch(removeNotification(n.id)))
  }

  const getNotificationIcon = (type: Notification['type']) => {
    switch (type) {
      case 'success':
        return <div className="h-8 w-8 rounded-full bg-green-100 text-green-600 flex items-center justify-center">
          <Check className="h-4 w-4" />
        </div>
      case 'error':
        return <div className="h-8 w-8 rounded-full bg-red-100 text-red-600 flex items-center justify-center">
          <X className="h-4 w-4" />
        </div>
      case 'warning':
        return <div className="h-8 w-8 rounded-full bg-yellow-100 text-yellow-600 flex items-center justify-center">
          ⚠
        </div>
      default:
        return <div className="h-8 w-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center">
          <Bell className="h-4 w-4" />
        </div>
    }
  }

  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 1) return '刚刚'
    if (diffMins < 60) return `${diffMins}分钟前`
    if (diffHours < 24) return `${diffHours}小时前`
    if (diffDays < 7) return `${diffDays}天前`
    return date.toLocaleDateString('zh-CN')
  }

  return (
    <div className="relative" ref={dropdownRef}>
      {/* Bell Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={cn(
          'relative rounded-lg p-2 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors',
          isOpen && 'bg-slate-100 dark:bg-slate-800'
        )}
      >
        <Bell className="h-5 w-5" />
        {unreadCount > 0 && (
          <span className="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-error-500 text-xs text-white">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {/* Dropdown */}
      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 rounded-lg bg-white dark:bg-slate-900 shadow-lg border border-slate-200 dark:border-slate-700 z-50">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-slate-200 dark:border-slate-700">
            <h3 className="font-semibold text-slate-900 dark:text-slate-100">
              通知
              {unreadCount > 0 && (
                <span className="ml-2 text-sm font-normal text-slate-500">
                  ({unreadCount} 条未读)
                </span>
              )}
            </h3>
            <div className="flex items-center gap-1">
              {unreadCount > 0 && (
                <button
                  onClick={handleMarkAllAsRead}
                  className="rounded p-1 hover:bg-slate-100 dark:hover:bg-slate-800"
                  title="全部标记为已读"
                >
                  <CheckCheck className="h-4 w-4 text-slate-500" />
                </button>
              )}
              {notifications.length > 0 && (
                <button
                  onClick={handleClearAll}
                  className="rounded p-1 hover:bg-slate-100 dark:hover:bg-slate-800"
                  title="清空所有"
                >
                  <Trash2 className="h-4 w-4 text-slate-500" />
                </button>
              )}
            </div>
          </div>

          {/* Notifications List */}
          <div className="max-h-96 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-slate-500">
                <Bell className="h-12 w-12 mb-2 opacity-50" />
                <p className="text-sm">暂无通知</p>
              </div>
            ) : (
              <ul className="divide-y divide-slate-200 dark:divide-slate-700">
                {notifications.map((notification) => (
                  <li
                    key={notification.id}
                    className={cn(
                      'cursor-pointer transition-colors hover:bg-slate-50 dark:hover:bg-slate-800',
                      !notification.read && 'bg-blue-50 dark:bg-blue-900/20'
                    )}
                    onClick={() => handleNotificationClick(notification)}
                  >
                    <div className="flex gap-3 p-4">
                      {getNotificationIcon(notification.type)}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between gap-2">
                          <p className={cn(
                            'text-sm font-medium',
                            !notification.read ? 'text-slate-900 dark:text-slate-100' : 'text-slate-600 dark:text-slate-400'
                          )}>
                            {notification.title}
                          </p>
                          {!notification.read && (
                            <span className="h-2 w-2 rounded-full bg-blue-500 flex-shrink-0 mt-1" />
                          )}
                        </div>
                        <p className="text-sm text-slate-600 dark:text-slate-400 mt-1 line-clamp-2">
                          {notification.message}
                        </p>
                        <p className="text-xs text-slate-400 mt-1">
                          {formatTime(notification.createdAt)}
                        </p>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Footer */}
          {notifications.length > 0 && (
            <div className="px-4 py-2 border-t border-slate-200 dark:border-slate-700">
              <button
                onClick={() => {
                  // 可以跳转到通知中心页面
                  setIsOpen(false)
                }}
                className="w-full text-center text-sm text-primary-600 hover:text-primary-700"
              >
                查看所有通知
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default NotificationDropdown
