import React, { useState, useMemo } from 'react'
import { useDispatch } from 'react-redux'
import { Code2, Upload, Play, CheckCircle, Clock, Bug, Zap, AlertTriangle, AlertCircle, Info, Download, Loader2, RefreshCw } from 'lucide-react'
import Card, { CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import { codeReviewService } from '../../services/code-review.service'
import type { CodeReviewRequest, ReviewIssue, AgentExecution } from '../../types'
import { reviewCompleted, teachingReportGenerated } from '../../store/slices/notificationSlice'

const CodeReviewPage: React.FC = () => {
  const dispatch = useDispatch()
  const [code, setCode] = useState('')
  const [language, setLanguage] = useState('javascript')
  const [reviewId, setReviewId] = useState<number | null>(null)
  const [issues, setIssues] = useState<ReviewIssue[]>([])
  const [agents, setAgents] = useState<AgentExecution[]>([])
  const [loading, setLoading] = useState(false)
  const [teachingReport, setTeachingReport] = useState<any>(null)
  const [downloading, setDownloading] = useState(false)
  const [teachingReportLoading, setTeachingReportLoading] = useState(false)

  // Calculate statistics from issues
  const statistics = useMemo(() => {
    const bySeverity = { HIGH: 0, MEDIUM: 0, LOW: 0 }
    const byCategory: Record<string, number> = {}
    const byAgent: Record<string, number> = {}

    issues.forEach(issue => {
      // Count by severity
      if (issue.severity && bySeverity[issue.severity] !== undefined) {
        bySeverity[issue.severity]++
      }

      // Count by category
      if (issue.category) {
        byCategory[issue.category] = (byCategory[issue.category] || 0) + 1
      }

      // Count by agent
      if (issue.agentType) {
        byAgent[issue.agentType] = (byAgent[issue.agentType] || 0) + 1
      }
    })

    return { bySeverity, byCategory, byAgent }
  }, [issues])

  const getSeverityIcon = (severity: string) => {
    switch (severity) {
      case 'HIGH': return <AlertTriangle className="h-5 w-5" />
      case 'MEDIUM': return <AlertCircle className="h-5 w-5" />
      case 'LOW': return <Info className="h-5 w-5" />
      default: return <Bug className="h-5 w-5" />
    }
  }

  const handleSubmit = async () => {
    if (!code.trim()) return

    setLoading(true)
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
        // Start polling for results
        pollResults(response.data)
      }
    } catch (error) {
      console.error('Failed to submit review:', error)
    } finally {
      setLoading(false)
    }
  }

  const pollResults = async (id: number) => {
    const interval = setInterval(async () => {
      try {
        const [statusRes, issuesRes, agentsRes] = await Promise.all([
          codeReviewService.getReviewStatus(id),
          codeReviewService.getReviewIssues(id),
          codeReviewService.getAgentExecutions(id),
        ])

        if (issuesRes.code === 200 && issuesRes.data) {
          setIssues(issuesRes.data)
        }

        if (agentsRes.code === 200 && agentsRes.data) {
          setAgents(agentsRes.data)
        }

        // Stop polling when completed
        if (statusRes.data?.status === 'COMPLETED') {
          clearInterval(interval)

          // 触发审查完成通知
          const fileName = code.split('\n')[0].substring(0, 50) || '代码文件'
          dispatch(reviewCompleted({
            reviewId: id,
            fileName: fileName.length > 40 ? fileName.substring(0, 40) + '...' : fileName,
          }))

          // Fetch teaching report after completion (will poll until ready)
          pollTeachingReport(id)
        }
      } catch (error) {
        console.error('Failed to poll results:', error)
        clearInterval(interval)
      }
    }, 2000)
  }

  const pollTeachingReport = async (reviewId: number, maxAttempts = 15) => {
    setTeachingReportLoading(true)
    let attempts = 0

    const checkReport = async () => {
      try {
        attempts++
        const response = await codeReviewService.getReviewDetail(reviewId)

        if (response.code === 200 && response.data?.teachingReport) {
          setTeachingReport(response.data.teachingReport)
          setTeachingReportLoading(false)

          // 触发教学报告生成完成通知
          dispatch(teachingReportGenerated({ reviewId }))

          console.log('Teaching report loaded successfully')
          return true
        }

        if (attempts >= maxAttempts) {
          setTeachingReportLoading(false)
          console.warn('Teaching report not available after maximum attempts')
          return false
        }

        // Wait 2 seconds before next attempt
        await new Promise(resolve => setTimeout(resolve, 2000))
        return await checkReport()
      } catch (error) {
        console.error('Error fetching teaching report:', error)
        if (attempts >= maxAttempts) {
          setTeachingReportLoading(false)
          return false
        }
        await new Promise(resolve => setTimeout(resolve, 2000))
        return await checkReport()
      }
    }

    checkReport()
  }

  const handleDownloadTeachingReport = async () => {
    if (!reviewId) return

    setDownloading(true)
    try {
      const blob = await codeReviewService.downloadTeachingReport(reviewId)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `teaching-report-${reviewId}.md`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
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
      // Generate full report as markdown
      const reportContent = generateFullReport()
      const blob = new Blob([reportContent], { type: 'text/markdown' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `code-review-${reviewId}-report.md`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      console.error('Failed to download full report:', error)
      alert('下载完整报告失败')
    } finally {
      setDownloading(false)
    }
  }

  const generateFullReport = (): string => {
    if (!reviewId || !agents.length) return ''

    let report = `# 代码审查报告\n\n`
    report += `**审查ID**: ${reviewId}\n`
    report += `**编程语言**: ${language}\n`
    report += `**生成时间**: ${new Date().toLocaleString()}\n\n`
    report += `---\n\n`

    // Statistics
    report += `## 📊 审查统计\n\n`
    report += `- **总问题数**: ${issues.length}\n`
    report += `- **高危问题**: ${statistics.bySeverity.HIGH}\n`
    report += `- **中等问题**: ${statistics.bySeverity.MEDIUM}\n`
    report += `- **轻微问题**: ${statistics.bySeverity.LOW}\n\n`

    // Teaching Report
    if (teachingReport) {
      report += `## 📚 教学报告\n\n`
      report += `### 总结\n${teachingReport.summary || '无'}\n\n`

      if (teachingReport.knowledgeGaps && teachingReport.knowledgeGaps.length > 0) {
        report += `### 知识缺口\n`
        teachingReport.knowledgeGaps.forEach((gap: string, i: number) => {
          report += `${i + 1}. ${gap}\n`
        })
        report += `\n`
      }

      if (teachingReport.keyFindings && teachingReport.keyFindings.length > 0) {
        report += `### 关键发现\n`
        teachingReport.keyFindings.forEach((finding: any) => {
          report += `- **${finding.category}**: ${finding.issue}\n`
          report += `  - 说明: ${finding.explanation}\n`
          report += `  - 改进: ${finding.improvement}\n\n`
        })
      }

      if (teachingReport.encouragement) {
        report += `### 💡 鼓励\n${teachingReport.encouragement}\n\n`
      }
    }

    // Agent executions
    report += `## 🤖 智能体执行情况\n\n`
    agents.forEach(agent => {
      report += `### ${agent.agentName}\n`
      report += `- 状态: ${agent.status}\n`
      report += `- 发现问题: ${agent.issuesFound || 0}\n`
      if (agent.duration) {
        report += `- 耗时: ${(agent.duration / 1000).toFixed(2)}s\n`
      }
      report += `\n`
    })

    // Issues
    report += `## 🔍 问题详情\n\n`
    issues.forEach((issue, index) => {
      report += `### ${index + 1}. ${issue.title}\n\n`
      report += `- **严重程度**: ${issue.severity}\n`
      report += `- **分类**: ${issue.category}\n`
      report += `- **描述**: ${issue.description}\n`
      if (issue.suggestion) {
        report += `- **建议**: ${issue.suggestion}\n`
      }
      if (issue.teachingExplanation) {
        report += `- **教学说明**: ${issue.teachingExplanation}\n`
      }
      if (issue.lineNumber) {
        report += `- **行号**: ${issue.lineNumber}\n`
      }
      if (issue.codeSnippet) {
        report += `- **代码**:\n\`\`\n${issue.codeSnippet.substring(0, 100)}...\n\`\`\n\n`
      }
      report += `\n`
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
      HIGH: 'bg-error-100 text-error-700 border-error-200',
      MEDIUM: 'bg-warning-100 text-warning-700 border-warning-200',
      LOW: 'bg-info-100 text-info-700 border-info-200',
    }
    return colors[severity] || 'bg-slate-100 text-slate-700 border-slate-200'
  }

  return (
    <div className="p-6 lg:p-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
          代码审查
        </h1>
        <p className="mt-1 text-slate-600 dark:text-slate-400">
          {agents.length > 0 ? `${agents.length}个` : '多个'}AI智能体协作分析你的代码，提供专业的改进建议
        </p>
      </div>

      {/* Input Section */}
      <Card variant="bordered" className="mb-6">
        <CardHeader>
          <CardTitle>提交代码</CardTitle>
          <CardDescription>粘贴你的代码，选择编程语言，开始智能审查</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
              编程语言
            </label>
            <select
              value={language}
              onChange={(e) => setLanguage(e.target.value)}
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
              onChange={(e) => setCode(e.target.value)}
              placeholder="// 粘贴你的代码到这里..."
              className="w-full h-64 rounded-lg border border-slate-300 px-4 py-3 font-mono text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-200 dark:border-slate-700 dark:bg-slate-800 resize-none"
            />
          </div>

          <Button
            onClick={handleSubmit}
            loading={loading}
            disabled={!code.trim()}
            className="w-full"
          >
            <Play className="h-4 w-4 mr-2" />
            开始审查
          </Button>
        </CardContent>
      </Card>

      {/* Agent Execution Status */}
      {agents.length > 0 && (
        <Card variant="bordered" className="mb-6">
          <CardHeader>
            <CardTitle>智能体执行状态</CardTitle>
            <CardDescription>{agents.length}个专业AI智能体协作分析</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
              {agents.map((agent) => (
                <div
                  key={agent.agentType}
                  className={`flex items-center gap-3 p-4 rounded-lg border ${
                    agent.status === 'COMPLETED'
                      ? 'bg-success-50 border-success-200'
                      : agent.status === 'RUNNING'
                      ? 'bg-primary-50 border-primary-200'
                      : agent.status === 'FAILED'
                      ? 'bg-error-50 border-error-200'
                      : 'bg-slate-50 border-slate-200'
                  }`}
                >
                  <div className={`rounded-full p-2 ${
                    agent.status === 'COMPLETED'
                      ? 'bg-success-500 text-white'
                      : agent.status === 'RUNNING'
                      ? 'bg-primary-500 text-white'
                      : agent.status === 'FAILED'
                      ? 'bg-error-500 text-white'
                      : 'bg-slate-400 text-white'
                  }`}>
                    {getAgentIcon(agent.agentType)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-slate-900 dark:text-slate-100 truncate">
                      {agent.agentName}
                    </p>
                    <p className="text-xs text-slate-500">{agent.status === 'COMPLETED' ? '已完成' : agent.status === 'RUNNING' ? '分析中...' : '待处理'}</p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Statistics Summary */}
      {issues.length > 0 && (
        <Card variant="bordered" className="mb-6">
          <CardHeader>
            <CardTitle>问题统计</CardTitle>
            <CardDescription>按严重程度、分类和智能体分组统计</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-6 md:grid-cols-3">
              {/* By Severity */}
              <div>
                <h4 className="text-sm font-medium text-slate-700 dark:text-slate-300 mb-3">按严重程度</h4>
                <div className="space-y-2">
                  {Object.entries(statistics.bySeverity).map(([severity, count]) => (
                    count > 0 && (
                      <div key={severity} className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className={`rounded-full p-1 ${
                            severity === 'HIGH' ? 'bg-error-500 text-white' :
                            severity === 'MEDIUM' ? 'bg-warning-500 text-white' :
                            'bg-info-500 text-white'
                          }`}>
                            {getSeverityIcon(severity)}
                          </span>
                          <span className="text-sm text-slate-600 dark:text-slate-400">{severity}</span>
                        </div>
                        <Badge size="sm" variant={severity === 'HIGH' ? 'error' : severity === 'MEDIUM' ? 'warning' : 'info'}>
                          {count}
                        </Badge>
                      </div>
                    )
                  ))}
                  {Object.values(statistics.bySeverity).every(v => v === 0) && (
                    <p className="text-sm text-slate-500">无问题</p>
                  )}
                </div>
              </div>

              {/* By Category */}
              <div>
                <h4 className="text-sm font-medium text-slate-700 dark:text-slate-300 mb-3">按分类</h4>
                <div className="space-y-2 max-h-40 overflow-y-auto">
                  {Object.entries(statistics.byCategory)
                    .sort(([, a], [, b]) => b - a)
                    .map(([category, count]) => (
                      <div key={category} className="flex items-center justify-between">
                        <span className="text-sm text-slate-600 dark:text-slate-400 truncate" title={category}>
                          {category}
                        </span>
                        <Badge size="sm" variant="default">{count}</Badge>
                      </div>
                    ))}
                  {Object.keys(statistics.byCategory).length === 0 && (
                    <p className="text-sm text-slate-500">无分类数据</p>
                  )}
                </div>
              </div>

              {/* By Agent */}
              <div>
                <h4 className="text-sm font-medium text-slate-700 dark:text-slate-300 mb-3">按智能体</h4>
                <div className="space-y-2 max-h-40 overflow-y-auto">
                  {Object.entries(statistics.byAgent)
                    .sort(([, a], [, b]) => b - a)
                    .map(([agentType, count]) => {
                      const agent = agents.find(a => a.agentType === agentType)
                      const agentName = agent?.agentName || agentType
                      return (
                        <div key={agentType} className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span className="text-slate-600">
                              {getAgentIcon(agentType)}
                            </span>
                            <span className="text-sm text-slate-600 dark:text-slate-400 truncate" title={agentName}>
                              {agentName}
                            </span>
                          </div>
                          <Badge size="sm" variant="default">{count}</Badge>
                        </div>
                      )
                    })}
                  {Object.keys(statistics.byAgent).length === 0 && (
                    <p className="text-sm text-slate-500">无智能体数据</p>
                  )}
                </div>
              </div>
            </div>

            {/* Total Summary Bar */}
            <div className="mt-6 pt-6 border-t border-slate-200 dark:border-slate-700">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-8">
                  <div className="text-center">
                    <p className="text-2xl font-bold text-slate-900 dark:text-slate-100">{issues.length}</p>
                    <p className="text-xs text-slate-500">总问题数</p>
                  </div>
                  <div className="h-10 w-px bg-slate-200 dark:bg-slate-700"></div>
                  <div className="text-center">
                    <p className="text-2xl font-bold text-error-600">{statistics.bySeverity.HIGH}</p>
                    <p className="text-xs text-slate-500">高危</p>
                  </div>
                  <div className="text-center">
                    <p className="text-2xl font-bold text-warning-600">{statistics.bySeverity.MEDIUM}</p>
                    <p className="text-xs text-slate-500">中等</p>
                  </div>
                  <div className="text-center">
                    <p className="text-2xl font-bold text-info-600">{statistics.bySeverity.LOW}</p>
                    <p className="text-xs text-slate-500">轻微</p>
                  </div>
                </div>

                {/* Download buttons */}
                <div className="flex items-center gap-2">
                  {teachingReport ? (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={handleDownloadTeachingReport}
                      disabled={downloading}
                    >
                      <Download className="h-4 w-4 mr-2" />
                      教学报告
                    </Button>
                  ) : teachingReportLoading ? (
                    <Button size="sm" variant="outline" disabled>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      生成中...
                    </Button>
                  ) : (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => reviewId && pollTeachingReport(reviewId)}
                    >
                      <RefreshCw className="h-4 w-4 mr-2" />
                      刷新报告
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={handleDownloadFullReport}
                    disabled={downloading}
                  >
                    <Download className="h-4 w-4 mr-2" />
                    完整报告
                  </Button>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Review Issues */}
      {issues.length > 0 && (
        <Card variant="bordered">
          <CardHeader>
            <CardTitle>审查结果</CardTitle>
            <CardDescription>发现 {issues.length} 个问题</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {issues.map((issue) => (
                <div
                  key={issue.id}
                  className={`p-4 rounded-lg border ${getSeverityColor(issue.severity)}`}
                >
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <Badge size="sm" variant={issue.severity === 'HIGH' ? 'error' : issue.severity === 'MEDIUM' ? 'warning' : 'info'}>
                        {issue.severity}
                      </Badge>
                      <Badge size="sm" variant="default">
                        {issue.category}
                      </Badge>
                    </div>
                    {issue.lineNumber && (
                      <span className="text-sm text-slate-600 dark:text-slate-400">
                        行 {issue.lineNumber}
                      </span>
                    )}
                  </div>

                  <p className="font-medium text-slate-900 dark:text-slate-100 mb-1">
                    {issue.title}
                  </p>

                  {issue.description && (
                    <p className="text-sm text-slate-600 dark:text-slate-400 mb-2">
                      {issue.description}
                    </p>
                  )}

                  {issue.codeSnippet && (
                    <pre className="mt-2 p-3 bg-slate-800 rounded-lg text-sm overflow-x-auto">
                      <code className="text-slate-200">{issue.codeSnippet}</code>
                    </pre>
                  )}

                  {issue.suggestion && (
                    <div className="mt-3 p-3 bg-white dark:bg-slate-800 rounded-lg">
                      <p className="text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                        💡 建议：
                      </p>
                      <p className="text-sm text-slate-600 dark:text-slate-400">{issue.suggestion}</p>
                    </div>
                  )}

                  {issue.teachingExplanation && (
                    <div className="mt-3 p-3 bg-primary-50 dark:bg-primary-900/20 rounded-lg">
                      <p className="text-sm font-medium text-primary-700 dark:text-primary-300 mb-1">
                        📚 学习要点：
                      </p>
                      <p className="text-sm text-primary-600 dark:text-primary-400">{issue.teachingExplanation}</p>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Empty State */}
      {!reviewId && !code && (
        <Card variant="bordered">
          <CardContent className="py-12 text-center">
            <Upload className="h-16 w-16 mx-auto text-slate-400 mb-4" />
            <h3 className="text-lg font-medium text-slate-900 dark:text-slate-100 mb-2">
              开始你的第一次代码审查
            </h3>
            <p className="text-slate-600 dark:text-slate-400">
              粘贴代码并点击"开始审查"按钮
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

export default CodeReviewPage
