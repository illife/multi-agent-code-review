import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, LogOut, Menu } from 'lucide-react'
import { cn } from '../../lib/utils'
import Button from '../ui/Button'
import NotificationDropdown from './NotificationDropdown'
import { useSelector } from 'react-redux'
import { selectUser } from '../../store/slices/authSlice'

interface TopbarProps {
  sidebarCollapsed: boolean
  onMenuClick: () => void
}

const Topbar: React.FC<TopbarProps> = ({ sidebarCollapsed, onMenuClick }) => {
  const navigate = useNavigate()
  const user = useSelector(selectUser)

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
    navigate('/login')
  }

  const handleProfileClick = () => {
    navigate('/profile')
  }

  // 获取用户显示名称
  const getDisplayName = () => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      const parsedUser = JSON.parse(storedUser)
      return parsedUser.fullName || parsedUser.username || '用户'
    }
    return user?.username || '用户'
  }

  // 获取用户首字母作为头像
  const getAvatarLetter = () => {
    const name = getDisplayName()
    return name.charAt(0).toUpperCase()
  }

  return (
    <header
      className={cn(
        'fixed top-0 right-0 z-30 h-16 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-700 transition-all duration-300',
        sidebarCollapsed ? 'left-16' : 'left-64'
      )}
    >
      <div className="flex h-full items-center justify-between px-4">
        {/* Left: Mobile menu & Search */}
        <div className="flex items-center gap-4">
          <button
            onClick={onMenuClick}
            className="lg:hidden rounded-lg p-2 hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <Menu className="h-5 w-5" />
          </button>

          <div className="hidden md:flex items-center">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="search"
                placeholder="搜索..."
                className="h-9 w-64 rounded-lg border border-slate-300 bg-slate-50 pl-10 pr-4 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500 dark:border-slate-600 dark:bg-slate-800"
              />
            </div>
          </div>
        </div>

        {/* Right: Notifications, User, Logout */}
        <div className="flex items-center gap-2">
          {/* Notifications */}
          <NotificationDropdown />

          {/* User Menu */}
          <button
            onClick={handleProfileClick}
            className="hidden md:flex items-center gap-3 pl-4 border-l border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 rounded-lg px-3 py-2 transition-colors"
          >
            {/* Avatar - 与个人设置页面一致 */}
            <div className="h-9 w-9 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white text-sm font-bold shadow-sm">
              {getAvatarLetter()}
            </div>
            <div className="text-right">
              <p className="text-sm font-medium text-slate-900 dark:text-slate-100">
                {getDisplayName()}
              </p>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {user?.role === 'ADMIN' ? '管理员' : '普通用户'}
              </p>
            </div>
          </button>

          {/* Mobile Avatar */}
          <button
            onClick={handleProfileClick}
            className="md:hidden h-9 w-9 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white text-sm font-bold"
          >
            {getAvatarLetter()}
          </button>

          {/* Logout Button */}
          <Button
            variant="ghost"
            size="sm"
            onClick={handleLogout}
            className="hidden md:flex"
            title="退出登录"
          >
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </header>
  )
}

export default Topbar
