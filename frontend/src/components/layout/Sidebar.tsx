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
  description: string
  badge?: number | string
}

const sidebarItems: SidebarItem[] = [
  { title: '仪表板', path: '/dashboard', icon: LayoutDashboard, description: '系统总览' },
  { title: '知识库', path: '/knowledge/documents', icon: BookOpen, description: '文档索引' },
  { title: '智能问答', path: '/knowledge/qa', icon: MessageSquare, description: 'RAG 问答' },
  { title: '文档搜索', path: '/knowledge/search', icon: SearchIcon, description: '语义检索' },
  { title: '代码审查', path: '/review', icon: Code2, description: '5-Agent 协作' },
  { title: '项目管理', path: '/projects', icon: FolderKanban, description: '项目级分析' },
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
        'fixed left-0 top-0 z-40 h-screen border-r border-white/10 bg-slate-950/95 text-slate-200 shadow-[24px_0_80px_rgba(2,6,23,0.34)] backdrop-blur-2xl transition-all duration-300',
        collapsed ? '-translate-x-full lg:translate-x-0' : 'translate-x-0',
        collapsed ? 'w-16' : 'w-64'
      )}
    >
      {/* Logo */}
      <div className="flex h-16 items-center justify-between border-b border-white/10 px-4">
        {!collapsed && (
          <div className="flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg border border-emerald-300/30 bg-emerald-300/15 text-emerald-200">
              <Bot className="h-5 w-5" />
            </div>
            <div>
              <span className="block text-lg font-black leading-5 text-white">CodeView</span>
              <span className="text-xs text-slate-400">AI Review Lab</span>
            </div>
          </div>
        )}
        <button
          onClick={onToggle}
          className={cn(
            'rounded-lg border border-white/10 bg-white/5 p-1.5 text-slate-300 transition hover:bg-white/10',
            collapsed && 'mx-auto'
          )}
          aria-label={collapsed ? '展开导航' : '收起导航'}
        >
          {collapsed ? (
            <ChevronRight className="h-5 w-5" />
          ) : (
            <ChevronLeft className="h-5 w-5" />
          )}
        </button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto p-3">
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
                      'group flex items-center gap-3 rounded-lg px-3 py-3 text-sm font-medium transition-all',
                      'hover:bg-white/10 hover:text-white',
                      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-400',
                      isActive || navIsActive
                        ? 'bg-emerald-300/15 text-white shadow-[inset_3px_0_0_#34d399]'
                        : 'text-slate-400',
                      collapsed && 'justify-center px-2'
                    )
                  }
                  title={collapsed ? item.title : undefined}
                >
                  <Icon className="h-5 w-5 flex-shrink-0 text-current" />
                  {!collapsed && (
                    <>
                      <span className="min-w-0 flex-1">
                        <span className="block text-sm leading-5">{item.title}</span>
                        <span className="block truncate text-xs font-normal text-slate-500 group-hover:text-slate-400">
                          {item.description}
                        </span>
                      </span>
                      {item.badge && (
                        <span className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-400 text-xs text-slate-950">
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
      <div className="border-t border-white/10 p-4">
        {!collapsed && (
          <div className="mb-3 rounded-lg border border-white/10 bg-white/[0.04] p-3">
            <div className="flex items-center justify-between gap-2 text-xs text-slate-400">
              <span>Agent 编排</span>
              <span className="font-mono text-emerald-300">5 online</span>
            </div>
            <div className="mt-2 grid grid-cols-5 gap-1">
              {[0, 1, 2, 3, 4].map((item) => (
                <span key={item} className="h-1.5 rounded-full bg-emerald-300" />
              ))}
            </div>
          </div>
        )}
        <NavLink
          to="/profile"
          className={({ isActive }) =>
            cn(
              'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
              'hover:bg-white/10',
              isActive
                ? 'bg-emerald-300/15 text-white'
                : 'text-slate-400',
              collapsed && 'justify-center px-2'
            )
          }
          title={collapsed ? '个人设置' : undefined}
        >
          <User className="h-5 w-5 flex-shrink-0" />
          {!collapsed && <span>个人设置</span>}
        </NavLink>
      </div>
    </aside>
  )
}

export default Sidebar
