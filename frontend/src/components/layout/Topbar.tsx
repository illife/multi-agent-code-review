import React from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, LogOut, Menu, ShieldCheck, Server } from 'lucide-react'
import { cn } from '../../lib/utils'
import Button from '../ui/Button'
import NotificationDropdown from './NotificationDropdown'
import { useDispatch, useSelector } from 'react-redux'
import { logout, selectUser } from '../../store/slices/authSlice'

interface TopbarProps {
  sidebarCollapsed: boolean
  onMenuClick: () => void
}

const Topbar: React.FC<TopbarProps> = ({ sidebarCollapsed, onMenuClick }) => {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const user = useSelector(selectUser)

  const handleLogout = () => {
    dispatch(logout())
    navigate('/login')
  }

  const handleProfileClick = () => {
    navigate('/profile')
  }

  // 获取用户显示名称
  const getDisplayName = () => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser)
        return parsedUser.fullName || parsedUser.username || '用户'
      } catch {
        return user?.username || '用户'
      }
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
        'fixed right-0 top-0 z-30 h-16 border-b border-white/10 bg-slate-950/82 text-slate-100 shadow-[0_18px_50px_rgba(2,6,23,0.2)] backdrop-blur-2xl transition-all duration-300',
        sidebarCollapsed ? 'left-0 lg:left-16' : 'left-0 lg:left-64'
      )}
    >
      <div className="flex h-full items-center justify-between px-4 lg:px-6">
        {/* Left: Mobile menu & Search */}
        <div className="flex items-center gap-4">
          <button
            onClick={onMenuClick}
            className="rounded-lg border border-white/10 bg-white/5 p-2 text-slate-300 transition hover:bg-white/10 lg:hidden"
            aria-label="打开导航"
          >
            <Menu className="h-5 w-5" />
          </button>

          <div className="hidden md:flex items-center">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-emerald-300" />
              <input
                type="search"
                placeholder="搜索文档、项目或审查结果"
                className="h-10 w-72 rounded-lg border border-white/10 bg-white/[0.06] pl-10 pr-4 text-sm text-slate-100 placeholder:text-slate-500 transition focus:border-emerald-300/60 focus:bg-white/[0.09] focus:outline-none focus:ring-2 focus:ring-emerald-300/30 xl:w-96"
              />
            </div>
          </div>

          <div className="hidden items-center gap-2 rounded-lg border border-emerald-300/20 bg-emerald-300/10 px-3 py-2 text-xs font-medium text-emerald-200 xl:flex">
            <ShieldCheck className="h-4 w-4" />
            Token 限流已启用
          </div>

          <div className="hidden items-center gap-2 rounded-lg border border-white/10 bg-white/[0.05] px-3 py-2 text-xs font-medium text-slate-300 xl:flex">
            <Server className="h-4 w-4 text-sky-300" />
            codeview.top
          </div>
        </div>

        {/* Right: Notifications, User, Logout */}
        <div className="flex items-center gap-2">
          {/* Notifications */}
          <NotificationDropdown />

          {/* User Menu */}
          <button
            onClick={handleProfileClick}
            className="hidden items-center gap-3 rounded-lg border border-white/10 bg-white/[0.05] px-3 py-2 transition hover:bg-white/10 md:flex"
          >
            {/* Avatar - 与个人设置页面一致 */}
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-emerald-300 to-sky-400 text-sm font-black text-slate-950 shadow-sm">
              {getAvatarLetter()}
            </div>
            <div className="text-right">
              <p className="text-sm font-semibold text-white">
                {getDisplayName()}
              </p>
              <p className="text-xs text-slate-400">
                {user?.role === 'ADMIN' ? '管理员' : '普通用户'}
              </p>
            </div>
          </button>

          {/* Mobile Avatar */}
          <button
            onClick={handleProfileClick}
            className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-emerald-300 to-sky-400 text-sm font-black text-slate-950 md:hidden"
          >
            {getAvatarLetter()}
          </button>

          {/* Logout Button */}
          <Button
            variant="ghost"
            size="sm"
            onClick={handleLogout}
            className="hidden border border-white/10 text-slate-300 hover:bg-white/10 hover:text-white md:flex"
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
