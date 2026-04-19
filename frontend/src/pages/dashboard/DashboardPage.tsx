import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Code2,
  FolderKanban,
  GraduationCap,
  Trophy,
  TrendingUp,
  Clock,
  AlertCircle,
  ArrowRight,
} from 'lucide-react'
import Card, { CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import { reviewService } from '../../services/review.service'
import { teachingService } from '../../services/teaching.service'
import { learningService } from '../../services/learning.service'
import { projectService } from '../../services/project.service'

interface StatItem {
  title: string
  value: string | number
  change: string
  icon: any
  color: string
}

interface ActivityItem {
  id: number
  type: 'review' | 'learning' | 'exercise' | 'project'
  title: string
  time: string
  status: 'success' | 'warning' | 'info' | 'error'
}

interface PathItem {
  id: number
  title: string
  difficulty: string
  progress: number
}

const DashboardPage: React.FC = () => {
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState<StatItem[]>([
    { title: '本周审查', value: '-', change: '-', icon: Code2, color: 'text-primary-600 bg-primary-50' },
    { title: '学习进度', value: '-', change: '-', icon: GraduationCap, color: 'text-success-600 bg-success-50' },
    { title: '待处理问题', value: '-', change: '-', icon: AlertCircle, color: 'text-warning-600 bg-warning-50' },
    { title: '成就徽章', value: '-', change: '-', icon: Trophy, color: 'text-error-600 bg-error-50' },
  ])
  const [recentActivity, setRecentActivity] = useState<ActivityItem[]>([])
  const [recommendedPaths, setRecommendedPaths] = useState<PathItem[]>([])

  useEffect(() => {
    loadDashboardData()
  }, [])

  const loadDashboardData = async () => {
    try {
      setLoading(true)

      // Load data in parallel
      const [
        reviewsRes,
        progressRes,
        pathsRes,
        projectsRes,
        teachingStatsRes
      ] = await Promise.allSettled([
        reviewService.getReviewList(0, 10),
        learningService.getLearningProgress(),
        learningService.getLearningPaths(),
        projectService.getProjectList(0, 5),
        teachingService.getStats()
      ])

      // Process reviews
      let completedReviews = 0
      let failedReviews = 0
      const activities: ActivityItem[] = []

      if (reviewsRes.status === 'fulfilled' && reviewsRes.value.code === 200) {
        // The API returns a Page object with content array
        const reviewsData = reviewsRes.value.data as any
        const reviews = reviewsData?.content || reviewsData || []
        completedReviews = reviews.filter((r: any) => r.status === 'COMPLETED').length
        failedReviews = reviews.filter((r: any) => r.status === 'FAILED').length

        // Add recent review activities
        reviews.slice(0, 3).forEach((review: any, index: number) => {
          activities.push({
            id: index + 1,
            type: 'review',
            title: `代码审查 ${review.status === 'COMPLETED' ? '完成' : review.status === 'FAILED' ? '失败' : '进行中'}`,
            time: formatTimeAgo(review.createdAt),
            status: review.status === 'COMPLETED' ? 'success' : review.status === 'FAILED' ? 'error' : 'info'
          })
        })
      }

      // Process progress
      let currentLevel = 0
      let currentXp = 0
      if (progressRes.status === 'fulfilled' && progressRes.value.code === 200) {
        const progress = progressRes.value.data
        currentLevel = progress?.level || 0
        currentXp = progress?.totalXp || 0
      }

      // Process teaching stats as achievement indicator
      let publishedDocs = 0
      if (teachingStatsRes.status === 'fulfilled' && teachingStatsRes.value.code === 200) {
        const stats = teachingStatsRes.value.data
        publishedDocs = stats?.published || 0
      }

      // Process learning paths
      if (pathsRes.status === 'fulfilled' && pathsRes.value.code === 200) {
        const paths = pathsRes.value.data || []
        setRecommendedPaths(
          paths.slice(0, 3).map((path: any) => ({
            id: parseInt(path.pathId) || Math.random(),
            title: path.title,
            difficulty: path.difficulty === 'BEGINNER' ? '初级' : path.difficulty === 'INTERMEDIATE' ? '中级' : '高级',
            progress: path.progress || 0
          }))
        )
      }

      // Process projects
      if (projectsRes.status === 'fulfilled' && projectsRes.value.code === 200) {
        const projects = projectsRes.value.data || []
        projects.slice(0, 2).forEach((project: any, index: number) => {
          activities.push({
            id: activities.length + index + 1,
            type: 'project',
            title: `项目 "${project.projectName}" ${project.status === 'COMPLETED' ? '分析完成' : project.status === 'ANALYZING' ? '分析中' : '已上传'}`,
            time: formatTimeAgo(project.createdAt),
            status: project.status === 'COMPLETED' ? 'success' : project.status === 'FAILED' ? 'error' : 'info'
          })
        })
      }

      // Update stats
      setStats([
        {
          title: '本周审查',
          value: completedReviews.toString(),
          change: completedReviews > 0 ? `+${completedReviews}` : '-',
          icon: Code2,
          color: 'text-primary-600 bg-primary-50'
        },
        {
          title: '学习进度',
          value: currentLevel.toString(),
          change: `Lv.${currentLevel} (${currentXp} XP)`,
          icon: GraduationCap,
          color: 'text-success-600 bg-success-50'
        },
        {
          title: '待处理问题',
          value: failedReviews.toString(),
          change: failedReviews > 0 ? '需处理' : '无',
          icon: AlertCircle,
          color: 'text-warning-600 bg-warning-50'
        },
        {
          title: '教学文档',
          value: publishedDocs.toString(),
          change: `已发布 ${publishedDocs} 个`,
          icon: Trophy,
          color: 'text-error-600 bg-error-50'
        }
      ])

      // Sort activities by time and take top 4
      setRecentActivity(activities.slice(0, 4))

    } catch (error) {
      console.error('Failed to load dashboard data:', error)
    } finally {
      setLoading(false)
    }
  }

  const formatTimeAgo = (dateString?: string) => {
    if (!dateString) return '刚刚'
    const date = new Date(dateString)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 1) return '刚刚'
    if (diffMins < 60) return `${diffMins}分钟前`
    if (diffHours < 24) return `${diffHours}小时前`
    return `${diffDays}天前`
  }

  return (
    <div className="p-6 lg:p-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
          欢迎回来, {localStorage.getItem('username') || '开发者'}!
        </h1>
        <p className="mt-1 text-slate-600 dark:text-slate-400">
          这是你的学习概览和最新动态
        </p>
      </div>

      {loading ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4 mb-8">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-32 bg-slate-100 dark:bg-slate-800 rounded-lg animate-pulse" />
          ))}
        </div>
      ) : (
        <>
        {/* Stats Grid - Bento Box Layout */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4 mb-8">
        {stats.map((stat) => {
          const Icon = stat.icon
          return (
            <Card key={stat.title} variant="bordered" className="hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-slate-600 dark:text-slate-400">{stat.title}</p>
                    <p className="mt-2 text-3xl font-bold text-slate-900 dark:text-slate-100">{stat.value}</p>
                  </div>
                  <div className={`rounded-xl p-3 ${stat.color}`}>
                    <Icon className="h-6 w-6" />
                  </div>
                </div>
                <div className="mt-4 flex items-center gap-1 text-sm">
                  <TrendingUp className="h-4 w-4" />
                  <span className={stat.change.startsWith('+') ? 'text-success-600' : 'text-slate-600'}>
                    {stat.change}
                  </span>
                  <span className="text-slate-500">vs 上周</span>
                </div>
              </CardContent>
            </Card>
          )
        })}
      </div>

      {/* Main Content Grid */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Recent Activity - Spans 2 columns */}
        <div className="lg:col-span-2">
          <Card variant="bordered">
            <CardHeader>
              <CardTitle>最近活动</CardTitle>
              <CardDescription>你的最新学习进度和审查记录</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {recentActivity.length === 0 ? (
                  <div className="text-center py-8 text-slate-500">
                    暂无活动记录，开始你的第一个代码审查吧！
                  </div>
                ) : (
                  recentActivity.map((activity) => (
                    <div
                      key={activity.id}
                      className="flex items-start gap-4 p-4 rounded-lg border border-slate-100 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
                    >
                      <div
                        className={`mt-0.5 rounded-full p-2 ${
                          activity.status === 'success'
                            ? 'bg-success-100 text-success-600'
                            : activity.status === 'warning'
                            ? 'bg-warning-100 text-warning-600'
                            : activity.status === 'error'
                            ? 'bg-error-100 text-error-600'
                            : 'bg-primary-100 text-primary-600'
                        }`}
                      >
                        {activity.type === 'review' && <Code2 className="h-4 w-4" />}
                        {activity.type === 'learning' && <GraduationCap className="h-4 w-4" />}
                        {activity.type === 'exercise' && <Clock className="h-4 w-4" />}
                        {activity.type === 'project' && <FolderKanban className="h-4 w-4" />}
                      </div>
                      <div className="flex-1">
                        <p className="font-medium text-slate-900 dark:text-slate-100">{activity.title}</p>
                        <p className="text-sm text-slate-500">{activity.time}</p>
                      </div>
                      <Badge variant={activity.status === 'success' ? 'success' : activity.status === 'warning' ? 'warning' : activity.status === 'error' ? 'error' : 'info'}>
                        {activity.status === 'success' ? '完成' : activity.status === 'warning' ? '需处理' : activity.status === 'error' ? '失败' : '进行中'}
                      </Badge>
                    </div>
                  ))
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Recommended Learning */}
        <div>
          <Card variant="bordered">
            <CardHeader>
              <CardTitle>推荐学习</CardTitle>
              <CardDescription>基于你的技能水平推荐</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {recommendedPaths.length === 0 ? (
                  <div className="text-center py-8 text-slate-500">
                    暂无推荐学习路径
                  </div>
                ) : (
                  recommendedPaths.map((path) => (
                    <Link
                      key={path.id}
                      to={`/learning/paths/${path.id}`}
                      className="block p-4 rounded-lg border border-slate-100 dark:border-slate-800 hover:border-primary-200 hover:shadow-sm transition-all cursor-pointer"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <h4 className="font-medium text-slate-900 dark:text-slate-100">{path.title}</h4>
                          <div className="mt-2 flex items-center gap-2">
                            <Badge size="sm" variant="default">
                              {path.difficulty}
                            </Badge>
                            {path.progress > 0 && (
                              <span className="text-xs text-slate-500">{path.progress}% 完成</span>
                            )}
                          </div>
                        </div>
                        <ArrowRight className="h-5 w-5 text-slate-400" />
                      </div>
                    </Link>
                  ))
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="mt-6">
        <Card variant="bordered">
          <CardHeader>
            <CardTitle>快速开始</CardTitle>
            <CardDescription>选择一个操作开始你的学习之旅</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <Link
                to="/review"
                className="flex flex-col items-center gap-3 p-4 rounded-lg border border-slate-200 hover:border-primary-300 hover:bg-primary-50 transition-all cursor-pointer group"
              >
                <div className="rounded-xl bg-primary-100 p-3 text-primary-600 group-hover:bg-primary-600 group-hover:text-white transition-colors">
                  <Code2 className="h-6 w-6" />
                </div>
                <div className="text-center">
                  <p className="font-medium text-slate-900 dark:text-slate-100">提交代码审查</p>
                  <p className="text-sm text-slate-500">AI 分析代码质量</p>
                </div>
              </Link>

              <Link
                to="/learning"
                className="flex flex-col items-center gap-3 p-4 rounded-lg border border-slate-200 hover:border-success-300 hover:bg-success-50 transition-all cursor-pointer group"
              >
                <div className="rounded-xl bg-success-100 p-3 text-success-600 group-hover:bg-success-600 group-hover:text-white transition-colors">
                  <GraduationCap className="h-6 w-6" />
                </div>
                <div className="text-center">
                  <p className="font-medium text-slate-900 dark:text-slate-100">开始学习路径</p>
                  <p className="text-sm text-slate-500">系统化学习计划</p>
                </div>
              </Link>

              <Link
                to="/teaching/exercises"
                className="flex flex-col items-center gap-3 p-4 rounded-lg border border-slate-200 hover:border-warning-300 hover:bg-warning-50 transition-all cursor-pointer group"
              >
                <div className="rounded-xl bg-warning-100 p-3 text-warning-600 group-hover:bg-warning-600 group-hover:text-white transition-colors">
                  <Clock className="h-6 w-6" />
                </div>
                <div className="text-center">
                  <p className="font-medium text-slate-900 dark:text-slate-100">练习题挑战</p>
                  <p className="text-sm text-slate-500">巩固编程技能</p>
                </div>
              </Link>

              <Link
                to="/agents"
                className="flex flex-col items-center gap-3 p-4 rounded-lg border border-slate-200 hover:border-error-300 hover:bg-error-50 transition-all cursor-pointer group"
              >
                <div className="rounded-xl bg-error-100 p-3 text-error-600 group-hover:bg-error-600 group-hover:text-white transition-colors">
                  <FolderKanban className="h-6 w-6" />
                </div>
                <div className="text-center">
                  <p className="font-medium text-slate-900 dark:text-slate-100">AI 智能体</p>
                  <p className="text-sm text-slate-500">8个Agent协作</p>
                </div>
              </Link>
            </div>
          </CardContent>
        </Card>
      </div>
        </>)
      }
    </div>
  )
}

export default DashboardPage
