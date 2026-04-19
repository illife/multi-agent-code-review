import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { BookOpen, Clock, Trophy, Target, ChevronRight } from 'lucide-react'
import Card, { CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import { learningService } from '../../services/learning.service'
import type { LearningPath, LearningProgress } from '../../types'

const LearningPage: React.FC = () => {
  const [paths, setPaths] = useState<LearningPath[]>([])
  const [progress, setProgress] = useState<LearningProgress | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadLearningData()
  }, [])

  const loadLearningData = async () => {
    try {
      const [pathsRes, progressRes] = await Promise.all([
        learningService.getLearningPaths(),
        learningService.getLearningProgress(),
      ])

      if (pathsRes.code === 200 && pathsRes.data) {
        setPaths(pathsRes.data)
      }

      if (progressRes.code === 200 && progressRes.data) {
        setProgress(progressRes.data)
      }
    } catch (error) {
      console.error('Failed to load learning data:', error)
    } finally {
      setLoading(false)
    }
  }

  const getDifficultyColor = (difficulty: string) => {
    const colors: Record<string, string> = {
      BEGINNER: 'bg-success-100 text-success-700 border-success-200',
      INTERMEDIATE: 'bg-warning-100 text-warning-700 border-warning-200',
      ADVANCED: 'bg-error-100 text-error-700 border-error-200',
    }
    return colors[difficulty] || 'bg-slate-100 text-slate-700 border-slate-200'
  }

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      NOT_STARTED: 'bg-slate-100 text-slate-700',
      IN_PROGRESS: 'bg-primary-100 text-primary-700',
      COMPLETED: 'bg-success-100 text-success-700',
    }
    return colors[status] || 'bg-slate-100 text-slate-700'
  }

  const getStatusText = (status: string) => {
    const texts: Record<string, string> = {
      NOT_STARTED: '未开始',
      IN_PROGRESS: '进行中',
      COMPLETED: '已完成',
    }
    return texts[status] || status
  }

  return (
    <div className="p-6 lg:p-8 max-w-7xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
          学习中心
        </h1>
        <p className="mt-1 text-slate-600 dark:text-slate-400">
          系统化的学习路径，提升你的编程技能
        </p>
      </div>

      {progress && (
        <div className="grid gap-4 md:grid-cols-4 mb-8">
          <Card variant="bordered">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-primary-100 p-2 text-primary-600">
                  <Target className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-sm text-slate-600">总经验值</p>
                  <p className="text-xl font-bold text-slate-900">{progress.totalXp}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card variant="bordered">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-success-100 p-2 text-success-600">
                  <Trophy className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-sm text-slate-600">当前等级</p>
                  <p className="text-xl font-bold text-slate-900">Lv.{progress.level}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card variant="bordered">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-warning-100 p-2 text-warning-600">
                  <BookOpen className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-sm text-slate-600">已完成路径</p>
                  <p className="text-xl font-bold text-slate-900">{progress.pathsCompleted}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card variant="bordered">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="rounded-lg bg-info-100 p-2 text-info-600">
                  <Clock className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-sm text-slate-600">连续打卡</p>
                  <p className="text-xl font-bold text-slate-900">{progress.streakDays}天</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      <Card variant="bordered">
        <CardHeader>
          <CardTitle>学习路径</CardTitle>
          <CardDescription>按顺序完成模块，系统化学习编程知识</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="py-12 text-center text-slate-500">加载中...</div>
          ) : (
            <div className="space-y-4">
              {paths.map((path) => (
                <Link
                  key={path.pathId}
                  to={`/learning/paths/${path.pathId}`}
                  className="block p-5 rounded-lg border border-slate-200 hover:border-primary-300 hover:shadow-md transition-all"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                          {path.title}
                        </h3>
                        <Badge size="sm" variant="default" className={getDifficultyColor(path.difficulty)}>
                          {path.difficulty === 'BEGINNER' ? '初级' : path.difficulty === 'INTERMEDIATE' ? '中级' : '高级'}
                        </Badge>
                        <Badge size="sm" variant="default" className={getStatusColor(path.status)}>
                          {getStatusText(path.status)}
                        </Badge>
                      </div>
                      <p className="text-sm text-slate-600 dark:text-slate-400 mb-3">
                        {path.description}
                      </p>
                      <div className="flex items-center gap-4 text-sm text-slate-500">
                        <span>⏱ {path.estimatedHours}小时</span>
                        <span>📚 {path.modules.length}个模块</span>
                        {path.progress > 0 && (
                          <div className="flex-1 max-w-xs">
                            <div className="flex items-center justify-between text-xs mb-1">
                              <span>进度</span>
                              <span>{path.progress}%</span>
                            </div>
                            <div className="h-2 bg-slate-200 rounded-full overflow-hidden">
                              <div
                                className="h-full bg-primary-600 transition-all"
                                style={{ width: `${path.progress}%` }}
                              />
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                    <ChevronRight className="h-5 w-5 text-slate-400" />
                  </div>
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default LearningPage
