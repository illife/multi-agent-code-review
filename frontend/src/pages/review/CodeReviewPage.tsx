import React, { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import {
  AlertCircle,
  AlertTriangle,
  Bug,
  CheckCircle,
  Clock,
  Code2,
  Download,
  ExternalLink,
  FileSearch,
  FolderKanban,
  Info,
  Loader2,
  Play,
  RefreshCw,
  Upload,
  Zap,
} from 'lucide-react'
import Card, { CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import { codeReviewService } from '../../services/code-review.service'
import { projectService } from '../../services/project.service'
import type {
  AgentExecution,
  CodeReviewRequest,
  CodeReviewResponse,
  ProjectFile,
  ProjectInfo,
  ReviewIssue,
} from '../../types'
import { reviewCompleted, teachingReportGenerated } from '../../store/slices/notificationSlice'

const CodeReviewPage: React.FC = () => {
  const dispatch = useDispatch()
  const [searchParams, setSearchParams] = useSearchParams()

  const [code, setCode] = useState('')
  const [language, setLanguage] = useState('javascript')
  const [reviewId, setReviewId] = useState<number | null>(null)
  const [reviewStatus, setReviewStatus] = useState<CodeReviewResponse['status'] | null>(null)
  const [reviewFileName, setReviewFileName] = useState('')
  const [issues, setIssues] = useState<ReviewIssue[]>([])
  const [agents, setAgents] = useState<AgentExecution[]>([])
  const [loading, setLoading] = useState(false)
  const [reviewLoading, setReviewLoading] = useState(false)
  const [teachingReport, setTeachingReport] = useState<any>(null)
  const [downloading, setDownloading] = useState(false)
  const [teachingReportLoading, setTeachingReportLoading] = useState(false)

  const [projects, setProjects] = useState<ProjectInfo[]>([])
  const [selectedProjectId, setSelectedProjectId] = useState<number | ''>('')
  const [projectFiles, setProjectFiles] = useState<ProjectFile[]>([])
  const [projectLoading, setProjectLoading] = useState(false)
  const [projectFilesLoading, setProjectFilesLoading] = useState(false)

  useEffect(() => {
    loadProjects()
  }, [])

  useEffect(() => {
    const id = Number(searchParams.get('reviewId'))
    if (Number.isFinite(id) && id > 0) {
      loadReviewResult(id)
    }
  }, [searchParams])

  const statistics = useMemo(() => {
    const bySeverity: Record<string, number> = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0, INFO: 0 }
    const byCategory: Record<string, number> = {}
    const byAgent: Record<string, number> = {}

    issues.forEach((issue) => {
      if (issue.severity) {
        bySeverity[issue.severity] = (bySeverity[issue.severity] || 0) + 1
      }
      if (issue.category) {
        byCategory[issue.category] = (byCategory[issue.category] || 0) + 1
      }
      if (issue.agentType) {
        byAgent[issue.agentType] = (byAgent[issue.agentType] || 0) + 1
      }
    })

    return { bySeverity, byCategory, byAgent }
  }, [issues])

  const selectedProject = projects.find((project) => project.id === selectedProjectId)

  const loadProjects = async () => {
    setProjectLoading(true)
    try {
      const response = await projectService.getProjectList(0, 20)
      if (response.code === 200) {
        const list = response.data || []
        setProjects(list)

        if (list.length > 0) {
          const preferred = list.find((project) => project.status === 'COMPLETED') || list[0]
          setSelectedProjectId(preferred.id)
          await loadProjectFiles(preferred.id)
        }
      }
    } catch (error) {
      console.error('Failed to load projects:', error)
    } finally {
      setProjectLoading(false)
    }
  }

  const loadProjectFiles = async (projectId: number) => {
    setProjectFilesLoading(true)
    try {
      const response = await projectService.getProjectFiles(projectId, 0, 100)
      if (response.code === 200) {
        setProjectFiles(response.data || [])
      }
    } catch (error) {
      console.error('Failed to load project files:', error)
      setProjectFiles([])
    } finally {
      setProjectFilesLoading(false)
    }
  }

  const handleProjectChange = async (projectId: number) => {
    setSelectedProjectId(projectId)
    await loadProjectFiles(projectId)
  }

  const loadReviewResult = async (id: number, showLoading = true) => {
    if (showLoading) setReviewLoading(true)
    try {
      const [detailRes, issuesRes, agentsRes] = await Promise.all([
        codeReviewService.getReviewDetail(id),
        codeReviewService.getReviewIssues(id),
        codeReviewService.getAgentExecutions(id),
      ])

      if (detailRes.code === 200 && detailRes.data) {
        setReviewId(id)
        setReviewStatus(detailRes.data.status)
        setReviewFileName(detailRes.data.fileName || `审查 #${id}`)
        setCode(detailRes.data.codeContent || '')
        setLanguage(detailRes.data.language || 'javascript')
        setTeachingReport(detailRes.data.teachingReport || null)
      }

      if (issuesRes.code === 200) {
        setIssues(issuesRes.data || [])
      }

      if (agentsRes.code === 200) {
        setAgents(agentsRes.data || [])
      }

      return detailRes.data?.status
    } catch (error) {
      console.error('Failed to load review result:', error)
      return null
    } finally {
      if (showLoading) setReviewLoading(false)
    }
  }

  const openReview = async (id: number) => {
    setSearchParams({ reviewId: String(id) })
    await loadReviewResult(id)
  }

  const getSeverityIcon = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
      case 'HIGH':
        return <AlertTriangle className="h-5 w-5" />
      case 'MEDIUM':
        return <AlertCircle className="h-5 w-5" />
      case 'LOW':
      case 'INFO':
        return <Info className="h-5 w-5" />
      default:
        return <Bug className="h-5 w-5" />
    }
  }

  const handleSubmit = async () => {
    if (!code.trim()) return

    setLoading(true)
    setIssues([])
    setAgents([])
    setTeachingReport(null)
    try {
      const request: CodeReviewRequest = {
        code,
        language,
        fileName: `code.${getFileExtension(language)}`,
        description: '代码审查请求',
      }

      const response = await codeReviewService.submitReview(request)
      if (response.code === 200 && response.data) {
        setReviewId(response.data)
        setReviewStatus('PENDING')
        setReviewFileName(request.fileName || `审查 #${response.data}`)
        setSearchParams({ reviewId: String(response.data) })
        pollResults(response.data)
      }
    } catch (error) {
      console.error('Failed to submit review:', error)
      alert('提交审查失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  const pollResults = (id: number) => {
    const interval = window.setInterval(async () => {
      try {
        const status = await loadReviewResult(id, false)

        if (status === 'COMPLETED' || status === 'FAILED') {
          window.clearInterval(interval)

          if (status === 'COMPLETED') {
            dispatch(reviewCompleted({
              reviewId: id,
              fileName: reviewFileName || `审查 #${id}`,
            }))
            pollTeachingReport(id)
          }
        }
      } catch (error) {
        console.error('Failed to poll results:', error)
        window.clearInterval(interval)
      }
    }, 2000)
  }

  const pollTeachingReport = async (id: number, maxAttempts = 15) => {
    setTeachingReportLoading(true)
    let attempts = 0

    const checkReport = async (): Promise<boolean> => {
      try {
        attempts++
        const response = await codeReviewService.getReviewDetail(id)

        if (response.code === 200 && response.data?.teachingReport) {
          setTeachingReport(response.data.teachingReport)
          setTeachingReportLoading(false)
          dispatch(teachingReportGenerated({ reviewId: id }))
          return true
        }

        if (attempts >= maxAttempts) {
          setTeachingReportLoading(false)
          return false
        }

        await new Promise((resolve) => setTimeout(resolve, 2000))
        return checkReport()
      } catch (error) {
        console.error('Error fetching teaching report:', error)
        if (attempts >= maxAttempts) {
          setTeachingReportLoading(false)
          return false
        }
        await new Promise((resolve) => setTimeout(resolve, 2000))
        return checkReport()
      }
    }

    checkReport()
  }

  const handleDownloadTeachingReport = async () => {
    if (!reviewId) return

    setDownloading(true)
    try {
      const blob = await codeReviewService.downloadTeachingReport(reviewId)
      downloadBlob(blob, `teaching-report-${reviewId}.md`)
    } catch (error) {
      console.error('Failed to download teaching report:', error)
      alert('下载教学报告失败')
    } finally {
      setDownloading(false)
    }
  }

  const handleDownloadFullReport = async () => {
    if (!reviewId) return

    setDownloading(true)
    try {
      const blob = await codeReviewService.downloadReviewReport(reviewId)
      downloadBlob(blob, `code-review-${reviewId}-report.md`)
    } catch (error) {
      console.error('Failed to download full report:', error)
      const reportContent = generateFullReport()
      downloadBlob(new Blob([reportContent], { type: 'text/markdown' }), `code-review-${reviewId}-report.md`)
    } finally {
      setDownloading(false)
    }
  }

  const downloadBlob = (blob: Blob, fileName: string) => {
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
  }

  const generateFullReport = (): string => {
    if (!reviewId) return ''

    let report = `# 代码审查报告\n\n`
    report += `**审查ID**: ${reviewId}\n`
    report += `**文件**: ${reviewFileName || '-'}\n`
    report += `**编程语言**: ${language}\n`
    report += `**状态**: ${reviewStatus || '-'}\n`
    report += `**生成时间**: ${new Date().toLocaleString()}\n\n`
    report += `## 审查统计\n\n`
    report += `- 总问题数: ${issues.length}\n`
    report += `- 严重问题: ${statistics.bySeverity.CRITICAL || 0}\n`
    report += `- 高危问题: ${statistics.bySeverity.HIGH || 0}\n`
    report += `- 中等问题: ${statistics.bySeverity.MEDIUM || 0}\n`
    report += `- 轻微问题: ${statistics.bySeverity.LOW || 0}\n\n`

    report += `## 问题详情\n\n`
    issues.forEach((issue, index) => {
      report += `### ${index + 1}. ${issue.title}\n\n`
      report += `- 严重程度: ${issue.severity}\n`
      report += `- 分类: ${issue.category}\n`
      if (issue.lineNumber) report += `- 行号: ${issue.lineNumber}\n`
      report += `\n${issue.description || ''}\n\n`
      if (issue.suggestion) report += `建议: ${issue.suggestion}\n\n`
      if (issue.codeSnippet) report += `\`\`\`\n${issue.codeSnippet}\n\`\`\`\n\n`
    })

    return report
  }

  const getFileExtension = (lang: string): string => {
    const extensions: Record<string, string> = {
      javascript: 'js',
      typescript: 'ts',
      python: 'py',
      java: 'java',
      go: 'go',
      rust: 'rs',
      cpp: 'cpp',
    }
    return extensions[lang] || 'txt'
  }

  const getAgentIcon = (agentType: string) => {
    const icons: Record<string, React.ReactNode> = {
      CODE_STANDARDS_INSPECTOR: <CheckCircle className="h-5 w-5" />,
      ARCHITECTURE_GUARDIAN: <Zap className="h-5 w-5" />,
      SECURITY_AUDITOR: <Bug className="h-5 w-5" />,
      PERFORMANCE_OPTIMIZER: <Clock className="h-5 w-5" />,
    }
    return icons[agentType] || <Code2 className="h-5 w-5" />
  }

  const getSeverityColor = (severity: string) => {
    const colors: Record<string, string> = {
      CRITICAL: 'bg-error-100 text-error-800 border-error-200',
      HIGH: 'bg-error-100 text-error-700 border-error-200',
      MEDIUM: 'bg-warning-100 text-warning-700 border-warning-200',
      LOW: 'bg-info-100 text-info-700 border-info-200',
      INFO: 'bg-slate-100 text-slate-700 border-slate-200',
    }
    return colors[severity] || 'bg-slate-100 text-slate-700 border-slate-200'
  }

  const getSeverityVariant = (severity: string) => {
    if (severity === 'CRITICAL' || severity === 'HIGH') return 'error'
    if (severity === 'MEDIUM') return 'warning'
    if (severity === 'LOW' || severity === 'INFO') return 'info'
    return 'default'
  }

  const getProjectStatusText = (status: ProjectInfo['status']) => {
    const text: Record<ProjectInfo['status'], string> = {
      PENDING: '等待中',
      ANALYZING: '分析中',
      COMPLETED: '已完成',
      FAILED: '失败',
    }
    return text[status]
  }

  return (
    <div className="p-6 lg:p-8 max-w-7xl mx-auto">
      <div className="mb-8 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
            代码审查
          </h1>
          <p className="mt-1 text-slate-600 dark:text-slate-400">
            从已上传项目查看文件级审查，也可临时提交代码片段。
          </p>
        </div>
        <Link to="/projects">
          <Button variant="outline">
            <FolderKanban className="h-4 w-4" />
            项目管理
          </Button>
        </Link>
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.05fr)_minmax(360px,0.95fr)]">
        <div className="space-y-6">
          <Card variant="bordered">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <FolderKanban className="h-5 w-5 text-primary-600" />
                项目文件审查
              </CardTitle>
              <CardDescription>选择已上传项目中的文件，查看项目分析生成的审查结果</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {projectLoading ? (
                <div className="py-10 text-center text-sm text-slate-500">项目加载中...</div>
              ) : projects.length === 0 ? (
                <div className="rounded-lg border border-dashed border-slate-300 p-6 text-center dark:border-slate-700">
                  <FolderKanban className="mx-auto mb-3 h-10 w-10 text-slate-400" />
                  <p className="text-sm text-slate-600 dark:text-slate-400">还没有可审查的项目</p>
                  <Link to="/projects" className="mt-4 inline-flex">
                    <Button size="sm">
                      <Upload className="h-4 w-4" />
                      上传项目
                    </Button>
                  </Link>
                </div>
              ) : (
                <>
                  <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_auto] md:items-center">
                    <select
                      value={selectedProjectId}
                      onChange={(event) => handleProjectChange(Number(event.target.value))}
                      className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-200 dark:border-slate-700 dark:bg-slate-800"
                    >
                      {projects.map((project) => (
                        <option key={project.id} value={project.id}>
                          {project.projectName} - {getProjectStatusText(project.status)}{project.visibility === 'PUBLIC' ? ' - 公开' : ''}
                        </option>
                      ))}
                    </select>
                    {selectedProject && (
                      <Badge
                        size="md"
                        variant={selectedProject.status === 'COMPLETED' ? 'success' : selectedProject.status === 'FAILED' ? 'error' : 'info'}
                      >
                        {selectedProject.totalIssues || 0} 个问题
                      </Badge>
                    )}
                  </div>

                  {projectFilesLoading ? (
                    <div className="py-8 text-center text-sm text-slate-500">文件加载中...</div>
                  ) : projectFiles.length === 0 ? (
                    <div className="rounded-lg bg-slate-50 p-6 text-center text-sm text-slate-500 dark:bg-slate-800">
                      项目文件正在扫描或尚未生成
                    </div>
                  ) : (
                    <div className="max-h-[360px] overflow-y-auto rounded-lg border border-slate-200 dark:border-slate-800">
                      <table className="w-full text-sm">
                        <thead className="sticky top-0 bg-slate-50 dark:bg-slate-800">
                          <tr>
                            <th className="px-4 py-2 text-left font-medium text-slate-700 dark:text-slate-300">文件</th>
                            <th className="px-4 py-2 text-left font-medium text-slate-700 dark:text-slate-300">语言</th>
                            <th className="px-4 py-2 text-left font-medium text-slate-700 dark:text-slate-300">状态</th>
                            <th className="px-4 py-2 text-right font-medium text-slate-700 dark:text-slate-300">审查</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                          {projectFiles.map((file) => (
                            <tr key={file.fileId} className="hover:bg-slate-50 dark:hover:bg-slate-800/70">
                              <td className="px-4 py-3">
                                <div className="max-w-[360px]">
                                  <p className="truncate font-medium text-slate-900 dark:text-slate-100" title={file.fileName}>
                                    {file.fileName}
                                  </p>
                                  <p className="truncate text-xs text-slate-500" title={file.filePath}>
                                    {file.filePath}
                                  </p>
                                </div>
                              </td>
                              <td className="px-4 py-3 text-slate-600 dark:text-slate-400">{file.language || '-'}</td>
                              <td className="px-4 py-3">
                                <Badge size="sm" variant={file.isAnalyzed ? 'success' : 'default'}>
                                  {file.isAnalyzed ? '已分析' : '待分析'}
                                </Badge>
                              </td>
                              <td className="px-4 py-3 text-right">
                                {file.reviewId ? (
                                  <Button size="sm" variant="outline" onClick={() => openReview(file.reviewId!)}>
                                    <FileSearch className="h-4 w-4" />
                                    查看
                                  </Button>
                                ) : (
                                  <span className="text-xs text-slate-400">
                                    {file.isAnalyzed ? '未生成' : '等待'}
                                  </span>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>

          <Card variant="bordered">
            <CardHeader>
              <CardTitle>临时代码片段</CardTitle>
              <CardDescription>用于快速验证单个函数或文件片段</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  编程语言
                </label>
                <select
                  value={language}
                  onChange={(event) => setLanguage(event.target.value)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-primary-500 focus:ring-2 focus:ring-primary-200 dark:border-slate-700 dark:bg-slate-800"
                >
                  <option value="javascript">JavaScript</option>
                  <option value="typescript">TypeScript</option>
                  <option value="python">Python</option>
                  <option value="java">Java</option>
                  <option value="go">Go</option>
                  <option value="rust">Rust</option>
                  <option value="cpp">C++</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  代码
                </label>
                <textarea
                  value={code}
                  onChange={(event) => setCode(event.target.value)}
                  placeholder="// 粘贴你的代码到这里..."
                  className="h-56 w-full resize-none rounded-lg border border-slate-300 px-4 py-3 font-mono text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-200 dark:border-slate-700 dark:bg-slate-800"
                />
              </div>

              <Button onClick={handleSubmit} loading={loading} disabled={!code.trim()} className="w-full">
                <Play className="h-4 w-4" />
                开始审查
              </Button>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card variant="bordered">
            <CardHeader>
              <div className="flex items-start justify-between gap-4">
                <div>
                  <CardTitle>审查结果</CardTitle>
                  <CardDescription>
                    {reviewId ? `${reviewFileName || `审查 #${reviewId}`}` : '等待选择项目文件或提交代码'}
                  </CardDescription>
                </div>
                {reviewId && (
                  <Badge variant={reviewStatus === 'COMPLETED' ? 'success' : reviewStatus === 'FAILED' ? 'error' : 'info'}>
                    {reviewStatus || '加载中'}
                  </Badge>
                )}
              </div>
            </CardHeader>
            <CardContent>
              {reviewLoading ? (
                <div className="flex items-center justify-center gap-2 py-16 text-sm text-slate-500">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  正在加载审查结果
                </div>
              ) : !reviewId ? (
                <div className="py-16 text-center">
                  <FileSearch className="mx-auto mb-4 h-14 w-14 text-slate-300" />
                  <p className="text-sm text-slate-500">选择一个已分析文件，或提交临时代码片段</p>
                </div>
              ) : (
                <div className="space-y-5">
                  <div className="grid grid-cols-4 gap-3">
                    <div className="rounded-lg bg-slate-50 p-3 dark:bg-slate-800">
                      <p className="text-xs text-slate-500">总问题</p>
                      <p className="mt-1 text-xl font-bold text-slate-900 dark:text-slate-100">{issues.length}</p>
                    </div>
                    <div className="rounded-lg bg-error-50 p-3 dark:bg-error-900/20">
                      <p className="text-xs text-slate-500">高危</p>
                      <p className="mt-1 text-xl font-bold text-error-600">
                        {(statistics.bySeverity.CRITICAL || 0) + (statistics.bySeverity.HIGH || 0)}
                      </p>
                    </div>
                    <div className="rounded-lg bg-warning-50 p-3 dark:bg-warning-900/20">
                      <p className="text-xs text-slate-500">中等</p>
                      <p className="mt-1 text-xl font-bold text-warning-600">{statistics.bySeverity.MEDIUM || 0}</p>
                    </div>
                    <div className="rounded-lg bg-primary-50 p-3 dark:bg-primary-900/20">
                      <p className="text-xs text-slate-500">低危</p>
                      <p className="mt-1 text-xl font-bold text-primary-600">
                        {(statistics.bySeverity.LOW || 0) + (statistics.bySeverity.INFO || 0)}
                      </p>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    {teachingReport ? (
                      <Button size="sm" variant="outline" onClick={handleDownloadTeachingReport} disabled={downloading}>
                        <Download className="h-4 w-4" />
                        教学报告
                      </Button>
                    ) : teachingReportLoading ? (
                      <Button size="sm" variant="outline" disabled>
                        <Loader2 className="h-4 w-4 animate-spin" />
                        报告生成中
                      </Button>
                    ) : (
                      <Button size="sm" variant="outline" onClick={() => reviewId && pollTeachingReport(reviewId)}>
                        <RefreshCw className="h-4 w-4" />
                        刷新报告
                      </Button>
                    )}
                    <Button size="sm" variant="outline" onClick={handleDownloadFullReport} disabled={downloading}>
                      <Download className="h-4 w-4" />
                      完整报告
                    </Button>
                    {reviewId && (
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => navigator.clipboard.writeText(`${window.location.origin}/review?reviewId=${reviewId}`)}
                      >
                        <ExternalLink className="h-4 w-4" />
                        复制链接
                      </Button>
                    )}
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {agents.length > 0 && (
            <Card variant="bordered">
              <CardHeader>
                <CardTitle>智能体执行状态</CardTitle>
                <CardDescription>{agents.length} 个检查角色</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid gap-3 sm:grid-cols-2">
                  {agents.map((agent) => (
                    <div
                      key={agent.agentType}
                      className="flex items-center gap-3 rounded-lg border border-slate-200 p-3 dark:border-slate-800"
                    >
                      <div className="rounded-full bg-slate-100 p-2 text-slate-700 dark:bg-slate-800 dark:text-slate-200">
                        {getAgentIcon(agent.agentType)}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium text-slate-900 dark:text-slate-100">
                          {agent.agentName}
                        </p>
                        <p className="text-xs text-slate-500">
                          {agent.status === 'COMPLETED' ? '已完成' : agent.status === 'RUNNING' ? '分析中' : agent.status}
                        </p>
                      </div>
                      <Badge size="sm" variant={agent.issuesFound > 0 ? 'warning' : 'success'}>
                        {agent.issuesFound || 0}
                      </Badge>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>

      {issues.length > 0 && (
        <Card variant="bordered" className="mt-6">
          <CardHeader>
            <CardTitle>问题列表</CardTitle>
            <CardDescription>发现 {issues.length} 个问题</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {issues.map((issue) => (
                <div key={issue.id} className={`rounded-lg border p-4 ${getSeverityColor(issue.severity)}`}>
                  <div className="mb-2 flex items-start justify-between gap-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="text-current">
                        {getSeverityIcon(issue.severity)}
                      </span>
                      <Badge size="sm" variant={getSeverityVariant(issue.severity) as any}>
                        {issue.severity}
                      </Badge>
                      <Badge size="sm" variant="default">
                        {issue.category}
                      </Badge>
                      {issue.agentType && (
                        <span className="text-xs text-slate-500">{issue.agentType}</span>
                      )}
                    </div>
                    {issue.lineNumber && (
                      <span className="shrink-0 text-sm text-slate-600 dark:text-slate-400">行 {issue.lineNumber}</span>
                    )}
                  </div>

                  <p className="mb-1 font-medium text-slate-900 dark:text-slate-100">{issue.title}</p>

                  {issue.description && (
                    <p className="mb-3 text-sm text-slate-700 dark:text-slate-300">{issue.description}</p>
                  )}

                  {issue.codeSnippet && (
                    <pre className="mt-2 overflow-x-auto rounded-lg bg-slate-900 p-3 text-sm">
                      <code className="text-slate-100">{issue.codeSnippet}</code>
                    </pre>
                  )}

                  {issue.suggestion && (
                    <div className="mt-3 rounded-lg bg-white p-3 dark:bg-slate-900">
                      <p className="mb-1 text-sm font-medium text-slate-700 dark:text-slate-300">建议</p>
                      <p className="text-sm text-slate-600 dark:text-slate-400">{issue.suggestion}</p>
                    </div>
                  )}

                  {issue.teachingExplanation && (
                    <div className="mt-3 rounded-lg bg-primary-50 p-3 dark:bg-primary-900/20">
                      <p className="mb-1 text-sm font-medium text-primary-700 dark:text-primary-300">学习要点</p>
                      <p className="text-sm text-primary-700 dark:text-primary-300">{issue.teachingExplanation}</p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

export default CodeReviewPage
