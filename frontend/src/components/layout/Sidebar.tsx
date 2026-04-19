import React from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { cn } from '../../lib/utils'
import {
  LayoutDashboard,
  Code2,
  FolderKanban,
  User,
  ChevronLeft,
  ChevronRight,
  BookOpen,
  MessageSquare,
  Search as SearchIcon,
  Bot,
} from 'lucide-react'

export interface SidebarItem {
  title: string
  path: string
  icon: React.ComponentType<{ className?: string }>
  badge?: number | string
}

const sidebarItems: SidebarItem[] = [
  { title: '仪表板', path: '/dashboard', icon: LayoutDashboard },
  { title: '知识库', path: '/knowledge/documents', icon: BookOpen },
  { title: '智能问答', path: '/knowledge/qa', icon: MessageSquare },
  { title: '文档搜索', path: '/knowledge/search', icon: SearchIcon },
  { title: '代码审查', path: '/review', icon: Code2 },
  { title: '项目管理', path: '/projects', icon: FolderKanban },
]

interface SidebarProps {
  collapsed: boolean
  onToggle: () => void
}

const Sidebar: React.FC<SidebarProps> = ({ collapsed, onToggle }) => {
  const location = useLocation()

  return (
    <aside
      className={cn(
        'fixed left-0 top-0 z-40 h-screen bg-white dark:bg-slate-900 border-r border-slate-200 dark:border-slate-700 transition-all duration-300',
        collapsed ? 'w-16' : 'w-64'
      )}
    >
      {/* Logo */}
      <div className="flex h-16 items-center justify-between px-4 border-b border-slate-200 dark:border-slate-700">
        {!collapsed && (
          <div className="flex items-center gap-2">
            <Bot className="h-6 w-6 text-primary-600" />
            <span className="text-lg font-semibold">CodeReview AI</span>
          </div>
        )}
        <button
          onClick={onToggle}
          className={cn(
            'rounded-lg p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors',
            collapsed && 'mx-auto'
          )}
        >
          {collapsed ? (
            <ChevronRight className="h-5 w-5" />
          ) : (
            <ChevronLeft className="h-5 w-5" />
          )}
        </button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto p-4">
        <ul className="space-y-1">
          {sidebarItems.map((item) => {
            const Icon = item.icon
            const isActive = location.pathname === item.path || location.pathname.startsWith(item.path + '/')

            return (
              <li key={item.path}>
                <NavLink
                  to={item.path}
                  className={({ isActive: navIsActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                      'hover:bg-slate-100 dark:hover:bg-slate-800',
                      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500',
                      isActive || navIsActive
                        ? 'bg-primary-50 text-primary-700 dark:bg-primary-900 dark:text-primary-300'
                        : 'text-slate-700 dark:text-slate-300',
                      collapsed && 'justify-center px-2'
                    )
                  }
                >
                  <Icon className="h-5 w-5 flex-shrink-0" />
                  {!collapsed && (
                    <>
                      <span>{item.title}</span>
                      {item.badge && (
                        <span className="ml-auto flex h-5 w-5 items-center justify-center rounded-full bg-primary-600 text-xs text-white">
                          {item.badge}
                        </span>
                      )}
                    </>
                  )}
                </NavLink>
              </li>
            )
          })}
        </ul>
      </nav>

      {/* User Section */}
      <div className="border-t border-slate-200 dark:border-slate-700 p-4">
        <NavLink
          to="/profile"
          className={({ isActive }) =>
            cn(
              'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
              'hover:bg-slate-100 dark:hover:bg-slate-800',
              isActive
                ? 'bg-primary-50 text-primary-700 dark:bg-primary-900 dark:text-primary-300'
                : 'text-slate-700 dark:text-slate-300',
              collapsed && 'justify-center px-2'
            )
          }
        >
          <User className="h-5 w-5 flex-shrink-0" />
          {!collapsed && <span>个人设置</span>}
        </NavLink>
      </div>
    </aside>
  )
}

export default Sidebar
