import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Activity,
  AlertCircle,
  ArrowRight,
  BookOpen,
  CheckCircle,
  Clock,
  Code2,
  Database,
  FileSearch,
  FolderKanban,
  GraduationCap,
  MessageSquare,
  Search,
  ShieldCheck,
  Sparkles,
  Trophy,
  Zap,
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
  icon: React.ComponentType<{ className?: string }>
  tone: string
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

const demoModules = [
  {
    title: '项目级代码审查',
    description: '从项目管理里的公开项目直接进入文件级审查，适合面试现场演示。',
    path: '/review',
    icon: Code2,
    tone: 'border-emerald-300/25 bg-emerald-300/10 text-emerald-200',
  },
  {
    title: '知识库文档',
    description: '上传文档后完成解析、切块、向量化、检索索引与问答联动。',
    path: '/knowledge/documents',
    icon: BookOpen,
    tone: 'border-amber-300/25 bg-amber-300/10 text-amber-200',
  },
  {
    title: '智能问答',
    description: '基于已索引文档进行上下文问答，展示 RAG 检索来源。',
    path: '/knowledge/qa',
    icon: MessageSquare,
    tone: 'border-sky-300/25 bg-sky-300/10 text-sky-200',
  },
  {
    title: '项目管理',
    description: '管理 ZIP 上传、分片进度、公开项目、分析报告和文件审查入口。',
    path: '/projects',
    icon: FolderKanban,
    tone: 'border-rose-300/25 bg-rose-300/10 text-rose-200',
  },
]

const architectureSteps = [
  { label: 'React 工作台', detail: 'Vite + Tailwind' },
  { label: 'API Gateway', detail: 'JWT 鉴权与路由' },
  { label: '微服务处理', detail: '审查 / 知识库 / 用户' },
  { label: 'Kafka 异步任务', detail: '上传与 AI 分析解耦' },
  { label: 'Postgres / ES / MinIO', detail: '结构化、检索与对象存储' },
]

const agentCards = [
  { name: '代码规范', icon: CheckCircle, color: 'text-emerald-300' },
  { name: '架构守护', icon: Zap, color: 'text-sky-300' },
  { name: '安全审计', icon: ShieldCheck, color: 'text-rose-300' },
  { name: '性能优化', icon: Activity, color: 'text-amber-300' },
  { name: '教学报告', icon: GraduationCap, color: 'text-violet-300' },
]

const DashboardPage: React.FC = () => {
  const [loading, setLoading] = useState(true)
  const [stats, setStats] = useState<StatItem[]>([
    { title: '已完成审查', value: '-', change: '-', icon: Code2, tone: 'text-emerald-300 bg-emerald-300/10' },
    { title: '学习等级', value: '-', change: '-', icon: GraduationCap, tone: 'text-sky-300 bg-sky-300/10' },
    { title: '待处理问题', value: '-', change: '-', icon: AlertCircle, tone: 'text-amber-300 bg-amber-300/10' },
    { title: '教学文档', value: '-', change: '-', icon: Trophy, tone: 'text-rose-300 bg-rose-300/10' },
  ])
  const [recentActivity, setRecentActivity] = useState<ActivityItem[]>([])
  const [recommendedPaths, setRecommendedPaths] = useState<PathItem[]>([])

  useEffect(() => {
    loadDashboardData()
  }, [])

  const loadDashboardData = async () => {
    try {
      setLoading(true)

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

      let completedReviews = 0
      let failedReviews = 0
      const activities: ActivityItem[] = []

      if (reviewsRes.status === 'fulfilled' && reviewsRes.value.code === 200) {
        const reviewsData = reviewsRes.value.data as any
        const reviews = reviewsData?.content || reviewsData || []
        completedReviews = reviews.filter((r: any) => r.status === 'COMPLETED').length
        failedReviews = reviews.filter((r: any) => r.status === 'FAILED').length

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

      let currentLevel = 0
      let currentXp = 0
      if (progressRes.status === 'fulfilled' && progressRes.value.code === 200) {
        const progress = progressRes.value.data
        currentLevel = progress?.level || 0
        currentXp = progress?.totalXp || 0
      }

      let publishedDocs = 0
      if (teachingStatsRes.status === 'fulfilled' && teachingStatsRes.value.code === 200) {
        const teachingStats = teachingStatsRes.value.data
        publishedDocs = teachingStats?.published || 0
      }

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

      setStats([
        {
          title: '已完成审查',
          value: completedReviews.toString(),
          change: completedReviews > 0 ? `+${completedReviews}` : '暂无新增',
          icon: Code2,
          tone: 'text-emerald-300 bg-emerald-300/10'
        },
        {
          title: '学习等级',
          value: currentLevel.toString(),
          change: `Lv.${currentLevel} · ${currentXp} XP`,
          icon: GraduationCap,
          tone: 'text-sky-300 bg-sky-300/10'
        },
        {
          title: '待处理问题',
          value: failedReviews.toString(),
          change: failedReviews > 0 ? '需要处理' : '状态健康',
          icon: AlertCircle,
          tone: 'text-amber-300 bg-amber-300/10'
        },
        {
          title: '教学文档',
          value: publishedDocs.toString(),
          change: `已发布 ${publishedDocs} 个`,
          icon: Trophy,
          tone: 'text-rose-300 bg-rose-300/10'
        }
      ])

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

  const userName = useMemo(() => localStorage.getItem('username') || '开发者', [])

  return (
    <div className="mx-auto max-w-[1500px] p-4 text-slate-100 sm:p-6 lg:p-8">
      <section className="relative mb-6 overflow-hidden rounded-lg border border-white/10 bg-white/[0.06] shadow-[0_30px_100px_rgba(2,6,23,0.3)] backdrop-blur-xl">
        <div className="absolute inset-0 cv-grid opacity-35" />
        <div className="absolute inset-x-0 top-0 h-64 cv-scanline opacity-60" />

        <div className="relative grid gap-8 p-6 lg:grid-cols-[minmax(0,1.12fr)_minmax(360px,0.88fr)] lg:p-8 xl:p-10">
          <div>
            <div className="cv-kicker">
              <Sparkles className="h-4 w-4 text-amber-300" />
              codeview.top online
            </div>
            <h1 className="mt-6 max-w-4xl text-4xl font-black leading-tight tracking-normal text-white lg:text-5xl">
              欢迎回来，{userName}
            </h1>
            <p className="mt-4 max-w-3xl text-base leading-8 text-slate-300 lg:text-lg">
              这里是 CodeView AI 工作台。它把项目级代码审查、知识库检索、RAG 问答、教学报告和 Docker 部署串成一个可以现场讲清楚的全栈 AI 工程闭环。
            </p>

            <div className="mt-7 flex flex-wrap gap-3">
              <Link
                to="/projects"
                className="inline-flex h-11 items-center justify-center gap-2 rounded-lg bg-emerald-300 px-5 text-sm font-bold text-slate-950 transition hover:bg-emerald-200"
              >
                查看公开项目
                <ArrowRight className="h-4 w-4" />
              </Link>
              <Link
                to="/review"
                className="inline-flex h-11 items-center justify-center gap-2 rounded-lg border border-white/15 bg-white/10 px-5 text-sm font-semibold text-white transition hover:bg-white/15"
              >
                进入代码审查
                <FileSearch className="h-4 w-4" />
              </Link>
            </div>

            <div className="mt-8 grid gap-3 sm:grid-cols-3">
              {[
                ['5', '协作智能体'],
                ['1MB', '分片上传'],
                ['ON', 'Token 限流保护'],
              ].map(([value, label]) => (
                <div key={label} className="rounded-lg border border-white/10 bg-black/20 p-4">
                  <p className="font-mono text-2xl font-bold text-emerald-300">{value}</p>
                  <p className="mt-1 text-sm text-slate-400">{label}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-lg border border-white/10 bg-slate-950/50 p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm uppercase text-slate-400">Architecture</p>
                <p className="mt-2 text-lg font-bold text-white">系统链路</p>
              </div>
              <div className="rounded-lg bg-emerald-300 p-2 text-slate-950">
                <Search className="h-5 w-5" />
              </div>
            </div>
            <div className="mt-5 space-y-3">
              {architectureSteps.map((step, index) => (
                <div key={step.label} className="grid grid-cols-[28px_1fr] gap-3">
                  <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-white/10 text-xs font-bold text-emerald-300">
                    {index + 1}
                  </span>
                  <div className="rounded-lg border border-white/10 bg-white/[0.04] px-3 py-2">
                    <p className="text-sm font-semibold text-slate-100">{step.label}</p>
                    <p className="text-xs text-slate-500">{step.detail}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="mb-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {demoModules.map((module) => {
          const Icon = module.icon
          return (
            <Link
              key={module.title}
              to={module.path}
              className="group rounded-lg border border-white/10 bg-white/[0.06] p-5 shadow-sm backdrop-blur transition hover:-translate-y-0.5 hover:border-emerald-300/35 hover:bg-white/[0.09]"
            >
              <div className={`mb-4 inline-flex rounded-lg border p-3 ${module.tone}`}>
                <Icon className="h-5 w-5" />
              </div>
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-bold text-white">{module.title}</p>
                  <p className="mt-2 text-sm leading-6 text-slate-400">{module.description}</p>
                </div>
                <ArrowRight className="mt-1 h-4 w-4 shrink-0 text-slate-500 transition group-hover:translate-x-1 group-hover:text-emerald-300" />
              </div>
            </Link>
          )
        })}
      </section>

      {loading ? (
        <div className="mb-8 grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-32 animate-pulse rounded-lg border border-white/10 bg-white/[0.05]" />
          ))}
        </div>
      ) : (
        <>
          <section className="mb-8 grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            {stats.map((stat) => {
              const Icon = stat.icon
              return (
                <div key={stat.title} className="rounded-lg border border-white/10 bg-white/[0.06] p-5 backdrop-blur transition hover:border-white/20">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-slate-400">{stat.title}</p>
                      <p className="mt-2 text-3xl font-black text-white">{stat.value}</p>
                    </div>
                    <div className={`rounded-lg p-3 ${stat.tone}`}>
                      <Icon className="h-6 w-6" />
                    </div>
                  </div>
                  <div className="mt-4 flex items-center gap-2 text-sm text-slate-400">
                    <Activity className="h-4 w-4 text-emerald-300" />
                    <span>{stat.change}</span>
                  </div>
                </div>
              )
            })}
          </section>

          <section className="grid gap-6 xl:grid-cols-[minmax(0,1.25fr)_minmax(360px,0.75fr)]">
            <Card variant="bordered" className="border-white/10 bg-white/[0.06] text-slate-100 backdrop-blur">
              <CardHeader>
                <CardTitle className="text-white">最近活动</CardTitle>
                <CardDescription className="text-slate-400">最新审查、项目分析和学习动作</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {recentActivity.length === 0 ? (
                    <div className="rounded-lg border border-dashed border-white/15 py-10 text-center text-slate-400">
                      暂无活动记录，可以从公开项目开始一次代码审查。
                    </div>
                  ) : (
                    recentActivity.map((activity) => (
                      <div
                        key={activity.id}
                        className="flex items-start gap-4 rounded-lg border border-white/10 bg-slate-950/35 p-4 transition hover:bg-white/[0.06]"
                      >
                        <div
                          className={`mt-0.5 rounded-lg p-2 ${
                            activity.status === 'success'
                              ? 'bg-emerald-300/15 text-emerald-300'
                              : activity.status === 'warning'
                                ? 'bg-amber-300/15 text-amber-300'
                                : activity.status === 'error'
                                  ? 'bg-rose-300/15 text-rose-300'
                                  : 'bg-sky-300/15 text-sky-300'
                          }`}
                        >
                          {activity.type === 'review' && <Code2 className="h-4 w-4" />}
                          {activity.type === 'learning' && <GraduationCap className="h-4 w-4" />}
                          {activity.type === 'exercise' && <Clock className="h-4 w-4" />}
                          {activity.type === 'project' && <FolderKanban className="h-4 w-4" />}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="font-medium text-white">{activity.title}</p>
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

            <div className="space-y-6">
              <Card variant="bordered" className="border-white/10 bg-white/[0.06] text-slate-100 backdrop-blur">
                <CardHeader>
                  <CardTitle className="text-white">智能体编排</CardTitle>
                  <CardDescription className="text-slate-400">面试时可以按这 5 个角色讲审查结果来源</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="grid gap-3">
                    {agentCards.map((agent) => {
                      const Icon = agent.icon
                      return (
                        <div key={agent.name} className="flex items-center gap-3 rounded-lg border border-white/10 bg-slate-950/35 p-3">
                          <Icon className={`h-5 w-5 ${agent.color}`} />
                          <span className="text-sm font-medium text-slate-200">{agent.name}</span>
                          <span className="ml-auto h-2 w-2 rounded-full bg-emerald-300" />
                        </div>
                      )
                    })}
                  </div>
                </CardContent>
              </Card>

              <Card variant="bordered" className="border-white/10 bg-white/[0.06] text-slate-100 backdrop-blur">
                <CardHeader>
                  <CardTitle className="text-white">推荐学习</CardTitle>
                  <CardDescription className="text-slate-400">根据审查问题沉淀学习路径</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {recommendedPaths.length === 0 ? (
                      <div className="rounded-lg border border-dashed border-white/15 py-8 text-center text-slate-400">
                        暂无推荐学习路径
                      </div>
                    ) : (
                      recommendedPaths.map((path) => (
                        <Link
                          key={path.id}
                          to={`/learning/paths/${path.id}`}
                          className="block rounded-lg border border-white/10 bg-slate-950/35 p-4 transition hover:border-emerald-300/35"
                        >
                          <div className="flex items-start justify-between gap-3">
                            <div className="min-w-0 flex-1">
                              <h4 className="font-medium text-white">{path.title}</h4>
                              <div className="mt-2 flex items-center gap-2">
                                <Badge size="sm" variant="default">
                                  {path.difficulty}
                                </Badge>
                                {path.progress > 0 && (
                                  <span className="text-xs text-slate-500">{path.progress}% 完成</span>
                                )}
                              </div>
                            </div>
                            <ArrowRight className="h-5 w-5 text-slate-500" />
                          </div>
                        </Link>
                      ))
                    )}
                  </div>
                </CardContent>
              </Card>
            </div>
          </section>

          <section className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {[
              { to: '/review', icon: Code2, title: '提交代码审查', desc: 'AI 分析代码质量' },
              { to: '/knowledge/documents', icon: Database, title: '上传知识文档', desc: '解析并建立检索索引' },
              { to: '/knowledge/qa', icon: MessageSquare, title: '知识库问答', desc: '检索并生成回答' },
              { to: '/projects', icon: FolderKanban, title: '项目管理', desc: '查看项目分析结果' },
            ].map((action) => {
              const Icon = action.icon
              return (
                <Link
                  key={action.title}
                  to={action.to}
                  className="group flex items-center gap-3 rounded-lg border border-white/10 bg-white/[0.05] p-4 transition hover:border-emerald-300/35 hover:bg-white/[0.08]"
                >
                  <div className="rounded-lg bg-emerald-300/10 p-3 text-emerald-300 transition group-hover:bg-emerald-300 group-hover:text-slate-950">
                    <Icon className="h-5 w-5" />
                  </div>
                  <div>
                    <p className="font-medium text-white">{action.title}</p>
                    <p className="text-sm text-slate-500">{action.desc}</p>
                  </div>
                </Link>
              )
            })}
          </section>
        </>
      )}
    </div>
  )
}

export default DashboardPage
