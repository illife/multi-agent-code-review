import React, { useState, useEffect } from 'react'
import {
  FolderKanban,
  Upload,
  CheckCircle,
  Clock,
  AlertCircle,
  Trash2,
  Eye,
  Download,
  RefreshCw,
  Plus,
  X,
  Network,
  FileText,
} from 'lucide-react'
import Card, { CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import MarkdownReport from '../../components/MarkdownReport'
import { projectService } from '../../services/project.service'
import type {
  ProjectInfo,
  ProjectStatusDTO,
  ProjectFile,
  ProjectReport,
} from '../../types'

const ProjectPage: React.FC = () => {
  const [projects, setProjects] = useState<ProjectInfo[]>([])
  const [selectedProject, setSelectedProject] = useState<ProjectInfo | null>(null)
  const [projectStatus, setProjectStatus] = useState<ProjectStatusDTO | null>(null)
  const [projectFiles, setProjectFiles] = useState<ProjectFile[]>([])
  const [projectReport, setProjectReport] = useState<ProjectReport | null>(null)
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [showUploadModal, setShowUploadModal] = useState(false)
  const [showDetailModal, setShowDetailModal] = useState(false)

  // Upload form state
  const [uploadFile, setUploadFile] = useState<File | null>(null)
  const [projectName, setProjectName] = useState('')
  const [description, setDescription] = useState('')
  const [visibility, setVisibility] = useState<'PRIVATE' | 'PUBLIC' | 'TEAM'>('PRIVATE')

  useEffect(() => {
    loadProjects()
  }, [])

  useEffect(() => {
    if (selectedProject && selectedProject.status === 'ANALYZING') {
      const interval = setInterval(async () => {
        await loadProjectStatus(selectedProject.id)
        await loadProjects()
      }, 3000)
      return () => clearInterval(interval)
    }
  }, [selectedProject])

  const loadProjects = async () => {
    try {
      const response = await projectService.getProjectList(0, 20)
      if (response.code === 200 && response.data) {
        setProjects(response.data)
      }
    } catch (error) {
      console.error('Failed to load projects:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadProjectStatus = async (projectId: number) => {
    try {
      const response = await projectService.getProjectStatus(projectId)
      if (response.code === 200 && response.data) {
        setProjectStatus(response.data)
        if (response.data.status === 'COMPLETED') {
          await loadProjects()
        }
      }
    } catch (error) {
      console.error('Failed to load project status:', error)
    }
  }

  const loadProjectFiles = async (projectId: number) => {
    try {
      const response = await projectService.getProjectFiles(projectId, 0, 100)
      if (response.code === 200 && response.data) {
        setProjectFiles(response.data)
      }
    } catch (error) {
      console.error('Failed to load project files:', error)
    }
  }

  const loadProjectReport = async (projectId: number) => {
    try {
      const response = await projectService.getProjectReport(projectId)
      if (response.code === 200 && response.data) {
        setProjectReport(response.data)
      }
    } catch (error) {
      console.error('Failed to load project report:', error)
    }
  }

  const handleUpload = async () => {
    if (!uploadFile || !projectName.trim()) {
      alert('请选择文件并输入项目名称')
      return
    }

    setUploading(true)
    try {
      const response = await projectService.uploadProject(
        uploadFile,
        projectName,
        description,
        visibility
      )
      if (response.code === 200 && response.data) {
        await loadProjects()
        setShowUploadModal(false)
        resetUploadForm()
        alert('项目上传成功！分析即将开始...')
      } else {
        alert(response.message || '上传失败')
      }
    } catch (error: any) {
      console.error('Upload failed:', error)
      alert(error.response?.data?.message || '上传失败，请重试')
    } finally {
      setUploading(false)
    }
  }

  const handleDelete = async (projectId: number, projectName: string) => {
    if (!confirm(`确定要删除项目 "${projectName}" 吗？`)) return

    try {
      const response = await projectService.deleteProject(projectId)
      if (response.code === 200) {
        await loadProjects()
        if (selectedProject?.id === projectId) {
          setSelectedProject(null)
          setShowDetailModal(false)
        }
      }
    } catch (error) {
      console.error('Delete failed:', error)
      alert('删除失败，请重试')
    }
  }

  const handleGenerateReport = async (projectId: number) => {
    try {
      const response = await projectService.generateReport(projectId)
      if (response.code === 200) {
        alert('报告生成已开始，请稍后查看')
        await loadProjectReport(projectId)
      }
    } catch (error) {
      console.error('Generate report failed:', error)
      alert('生成报告失败')
    }
  }

  const openProjectDetail = async (project: ProjectInfo) => {
    setSelectedProject(project)
    setShowDetailModal(true)
    await Promise.all([
      loadProjectStatus(project.id),
      loadProjectFiles(project.id),
      loadProjectReport(project.id),
    ])
  }

  const resetUploadForm = () => {
    setUploadFile(null)
    setProjectName('')
    setDescription('')
    setVisibility('PRIVATE')
  }

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      PENDING: 'bg-slate-100 text-slate-700 border-slate-200',
      ANALYZING: 'bg-primary-100 text-primary-700 border-primary-200',
      COMPLETED: 'bg-success-100 text-success-700 border-success-200',
      FAILED: 'bg-error-100 text-error-700 border-error-200',
    }
    return colors[status] || 'bg-slate-100 text-slate-700'
  }

  const getStatusIcon = (status: string) => {
    const icons: Record<string, React.ReactNode> = {
      PENDING: <Clock className="h-4 w-4" />,
      ANALYZING: <RefreshCw className="h-4 w-4 animate-spin" />,
      COMPLETED: <CheckCircle className="h-4 w-4" />,
      FAILED: <AlertCircle className="h-4 w-4" />,
    }
    return icons[status] || <Clock className="h-4 w-4" />
  }

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  return (
    <div className="p-6 lg:p-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
            项目管理
          </h1>
          <p className="mt-1 text-slate-600 dark:text-slate-400">
            上传ZIP项目文件，进行全面的代码分析
          </p>
        </div>
        <Button onClick={() => setShowUploadModal(true)}>
          <Plus className="h-4 w-4 mr-2" />
          上传项目
        </Button>
      </div>

      {/* Project List */}
      <Card variant="bordered">
        <CardHeader>
          <CardTitle>我的项目</CardTitle>
          <CardDescription>已上传 {projects.length} 个项目</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="text-center py-12 text-slate-500">加载中...</div>
          ) : projects.length === 0 ? (
            <div className="text-center py-12">
              <FolderKanban className="h-16 w-16 mx-auto text-slate-300 mb-4" />
              <h3 className="text-lg font-medium text-slate-900 dark:text-slate-100 mb-2">
                还没有项目
              </h3>
              <p className="text-slate-600 dark:text-slate-400 mb-4">
                上传你的第一个ZIP项目文件开始分析
              </p>
              <Button onClick={() => setShowUploadModal(true)}>
                <Upload className="h-4 w-4 mr-2" />
                上传项目
              </Button>
            </div>
          ) : (
            <div className="space-y-3">
              {projects.map((project) => (
                <div
                  key={project.id}
                  className="flex items-center gap-4 p-4 rounded-lg border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
                >
                  <div className={`rounded-full p-2 ${
                    project.status === 'COMPLETED'
                      ? 'bg-success-100 text-success-600'
                      : project.status === 'ANALYZING'
                      ? 'bg-primary-100 text-primary-600'
                      : project.status === 'FAILED'
                      ? 'bg-error-100 text-error-600'
                      : 'bg-slate-100 text-slate-600'
                  }`}>
                    {getStatusIcon(project.status)}
                  </div>

                  <div className="flex-1 min-w-0">
                    <h4 className="font-medium text-slate-900 dark:text-slate-100 truncate">
                      {project.projectName}
                    </h4>
                    <div className="flex items-center gap-3 mt-1 text-sm text-slate-600 dark:text-slate-400">
                      <span>{project.totalFiles} 个文件</span>
                      <span>•</span>
                      <span>{formatDate(project.createdAt)}</span>
                      {project.language && (
                        <>
                          <span>•</span>
                          <span>{project.language}</span>
                        </>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <Badge size="sm" variant={getStatusColor(project.status) as any}>
                      {project.status === 'PENDING' && '等待中'}
                      {project.status === 'ANALYZING' && '分析中'}
                      {project.status === 'COMPLETED' && '已完成'}
                      {project.status === 'FAILED' && '失败'}
                    </Badge>

                    {project.totalIssues > 0 && (
                      <Badge size="sm" variant="warning">
                        {project.totalIssues} 个问题
                      </Badge>
                    )}

                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => openProjectDetail(project)}
                    >
                      <Eye className="h-4 w-4" />
                    </Button>

                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => handleDelete(project.id, project.projectName)}
                    >
                      <Trash2 className="h-4 w-4 text-error-600" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Upload Modal */}
      {showUploadModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white dark:bg-slate-900 rounded-xl shadow-xl max-w-md w-full p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">
                上传项目
              </h2>
              <button
                onClick={() => {
                  setShowUploadModal(false)
                  resetUploadForm()
                }}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  ZIP文件 *
                </label>
                <input
                  type="file"
                  accept=".zip"
                  onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-200 dark:border-slate-700 dark:bg-slate-800"
                />
                {uploadFile && (
                  <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
                    已选择: {uploadFile.name} ({formatFileSize(uploadFile.size)})
                  </p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  项目名称 *
                </label>
                <input
                  type="text"
                  value={projectName}
                  onChange={(e) => setProjectName(e.target.value)}
                  placeholder="例如: my-web-app"
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-200 dark:border-slate-700 dark:bg-slate-800"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  项目描述
                </label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="简要描述这个项目..."
                  rows={3}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-200 dark:border-slate-700 dark:bg-slate-800 resize-none"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  可见性
                </label>
                <select
                  value={visibility}
                  onChange={(e) => setVisibility(e.target.value as any)}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-200 dark:border-slate-700 dark:bg-slate-800"
                >
                  <option value="PRIVATE">私有</option>
                  <option value="PUBLIC">公开</option>
                  <option value="TEAM">团队</option>
                </select>
              </div>

              <div className="flex gap-3 pt-4">
                <Button
                  variant="outline"
                  className="flex-1"
                  onClick={() => {
                    setShowUploadModal(false)
                    resetUploadForm()
                  }}
                >
                  取消
                </Button>
                <Button
                  className="flex-1"
                  onClick={handleUpload}
                  loading={uploading}
                  disabled={!uploadFile || !projectName.trim()}
                >
                  <Upload className="h-4 w-4 mr-2" />
                  上传
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Project Detail Modal */}
      {showDetailModal && selectedProject && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 overflow-y-auto">
          <div className="bg-white dark:bg-slate-900 rounded-xl shadow-xl max-w-4xl w-full my-8 max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 p-6 flex items-center justify-between">
              <div>
                <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">
                  {selectedProject.projectName}
                </h2>
                <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
                  {selectedProject.description || '无描述'}
                </p>
              </div>
              <button
                onClick={() => {
                  setShowDetailModal(false)
                  setSelectedProject(null)
                  setProjectStatus(null)
                  setProjectFiles([])
                  setProjectReport(null)
                }}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="h-6 w-6" />
              </button>
            </div>

            <div className="p-6 space-y-6">
              {/* Status Section */}
              {projectStatus && (
                <div>
                  <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100 mb-4">
                    分析状态
                  </h3>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <div className="p-4 bg-slate-50 dark:bg-slate-800 rounded-lg">
                      <p className="text-sm text-slate-600 dark:text-slate-400">状态</p>
                      <p className="text-lg font-semibold text-slate-900 dark:text-slate-100 mt-1">
                        {projectStatus.status === 'PENDING' && '等待中'}
                        {projectStatus.status === 'ANALYZING' && '分析中'}
                        {projectStatus.status === 'COMPLETED' && '已完成'}
                        {projectStatus.status === 'FAILED' && '失败'}
                      </p>
                    </div>
                    <div className="p-4 bg-slate-50 dark:bg-slate-800 rounded-lg">
                      <p className="text-sm text-slate-600 dark:text-slate-400">文件数</p>
                      <p className="text-lg font-semibold text-slate-900 dark:text-slate-100 mt-1">
                        {projectStatus.analyzedFiles} / {projectStatus.totalFiles}
                      </p>
                    </div>
                    <div className="p-4 bg-slate-50 dark:bg-slate-800 rounded-lg">
                      <p className="text-sm text-slate-600 dark:text-slate-400">发现问题</p>
                      <p className="text-lg font-semibold text-slate-900 dark:text-slate-100 mt-1">
                        {projectStatus.totalIssues}
                      </p>
                    </div>
                    <div className="p-4 bg-slate-50 dark:bg-slate-800 rounded-lg">
                      <p className="text-sm text-slate-600 dark:text-slate-400">进度</p>
                      <p className="text-lg font-semibold text-slate-900 dark:text-slate-100 mt-1">
                        {projectStatus.progress.toFixed(0)}%
                      </p>
                    </div>
                  </div>

                  {projectStatus.status === 'COMPLETED' && !projectReport && (
                    <Button className="mt-4" onClick={() => handleGenerateReport(selectedProject.id)}>
                      <Download className="h-4 w-4 mr-2" />
                      生成分析报告
                    </Button>
                  )}
                </div>
              )}

              {/* Files Section */}
              {projectFiles.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100 mb-4">
                    项目文件 ({projectFiles.length})
                  </h3>
                  <div className="max-h-64 overflow-y-auto border border-slate-200 dark:border-slate-800 rounded-lg">
                    <table className="w-full text-sm">
                      <thead className="bg-slate-50 dark:bg-slate-800 sticky top-0">
                        <tr>
                          <th className="px-4 py-2 text-left font-medium text-slate-700 dark:text-slate-300">文件名</th>
                          <th className="px-4 py-2 text-left font-medium text-slate-700 dark:text-slate-300">语言</th>
                          <th className="px-4 py-2 text-left font-medium text-slate-700 dark:text-slate-300">大小</th>
                          <th className="px-4 py-2 text-left font-medium text-slate-700 dark:text-slate-300">状态</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                        {projectFiles.map((file) => (
                          <tr key={file.fileId} className="hover:bg-slate-50 dark:hover:bg-slate-800">
                            <td className="px-4 py-2 text-slate-900 dark:text-slate-100">{file.fileName}</td>
                            <td className="px-4 py-2 text-slate-600 dark:text-slate-400">
                              {file.language || '-'}
                            </td>
                            <td className="px-4 py-2 text-slate-600 dark:text-slate-400">
                              {formatFileSize(file.fileSize)}
                            </td>
                            <td className="px-4 py-2">
                              {file.isAnalyzed ? (
                                <Badge size="sm" variant="success">已分析</Badge>
                              ) : (
                                <Badge size="sm" variant="default">待分析</Badge>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* Report Section */}
              {projectReport && (
                <div>
                  <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100 mb-4 flex items-center gap-2">
                    <FileText className="h-5 w-5" />
                    分析报告
                  </h3>

                  {/* Quick Stats Cards */}
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
                    <div className="p-3 bg-gradient-to-br from-primary-50 to-primary-100 dark:from-primary-900/20 dark:to-primary-800/20 rounded-lg border border-primary-200 dark:border-primary-800">
                      <p className="text-xs text-slate-600 dark:text-slate-400">综合评分</p>
                      <p className="text-2xl font-bold text-primary-600 dark:text-primary-400 mt-1">
                        {projectReport.overallScore}
                      </p>
                    </div>

                    <div className={`p-3 rounded-lg border ${
                      projectReport.riskLevel === 'LOW'
                        ? 'bg-gradient-to-br from-success-50 to-success-100 dark:from-success-900/20 dark:to-success-800/20 border-success-200 dark:border-success-800'
                        : projectReport.riskLevel === 'MEDIUM'
                        ? 'bg-gradient-to-br from-warning-50 to-warning-100 dark:from-warning-900/20 dark:to-warning-800/20 border-warning-200 dark:border-warning-800'
                        : 'bg-gradient-to-br from-error-50 to-error-100 dark:from-error-900/20 dark:to-error-800/20 border-error-200 dark:border-error-800'
                    }`}>
                      <p className="text-xs text-slate-600 dark:text-slate-400">风险等级</p>
                      <p className={`text-lg font-bold mt-1 ${
                        projectReport.riskLevel === 'LOW'
                          ? 'text-success-600 dark:text-success-400'
                          : projectReport.riskLevel === 'MEDIUM'
                          ? 'text-warning-600 dark:text-warning-400'
                          : 'text-error-600 dark:text-error-400'
                      }`}>
                        {projectReport.riskLevel === 'LOW' && '低风险'}
                        {projectReport.riskLevel === 'MEDIUM' && '中风险'}
                        {projectReport.riskLevel === 'HIGH' && '高风险'}
                        {projectReport.riskLevel === 'CRITICAL' && '严重风险'}
                      </p>
                    </div>

                    {projectReport.metrics && (() => {
                      const metrics = projectReport.metrics as any
                      const totalIssues = metrics.totalIssues || 0
                      return (
                        <div className="p-3 bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-800 dark:to-slate-900 rounded-lg border border-slate-200 dark:border-slate-700">
                          <p className="text-xs text-slate-600 dark:text-slate-400">发现问题</p>
                          <p className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-1">
                            {totalIssues}
                          </p>
                        </div>
                      )
                    })()}

                    {projectReport.metrics && (() => {
                      const metrics = projectReport.metrics as any
                      const analyzedFiles = metrics.analyzedFiles || metrics.filesAnalyzed || 0
                      return (
                        <div className="p-3 bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-800 dark:to-slate-900 rounded-lg border border-slate-200 dark:border-slate-700">
                          <p className="text-xs text-slate-600 dark:text-slate-400">分析文件</p>
                          <p className="text-2xl font-bold text-slate-900 dark:text-slate-100 mt-1">
                            {analyzedFiles}
                          </p>
                        </div>
                      )
                    })()}
                  </div>

                  {/* Full Markdown Report */}
                  {projectReport.fullMarkdownReport ? (
                    <div className="border border-slate-200 dark:border-slate-800 rounded-lg overflow-hidden">
                      <div className="bg-slate-50 dark:bg-slate-900 px-4 py-3 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <Network className="h-4 w-4 text-primary-600" />
                          <span className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                            AI 多智能体详细分析报告
                          </span>
                        </div>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => {
                            const blob = new Blob([projectReport.fullMarkdownReport!], { type: 'text/markdown' })
                            const url = URL.createObjectURL(blob)
                            const a = document.createElement('a')
                            a.href = url
                            a.download = `${selectedProject.projectName}-分析报告.md`
                            a.click()
                            URL.revokeObjectURL(url)
                          }}
                        >
                          <Download className="h-4 w-4 mr-1" />
                          导出
                        </Button>
                      </div>
                      <div className="p-4 max-h-[600px] overflow-y-auto bg-white dark:bg-slate-900">
                        <MarkdownReport markdown={projectReport.fullMarkdownReport} />
                      </div>
                    </div>
                  ) : (
                    /* Fallback to old display if no markdown report */
                    <div className="p-4 bg-slate-50 dark:bg-slate-800 rounded-lg space-y-4">
                      <div>
                        <p className="text-sm text-slate-600 dark:text-slate-400 mb-1">总结</p>
                        <div className="text-slate-900 dark:text-slate-100 whitespace-pre-wrap text-sm">
                          {projectReport.summary}
                        </div>
                      </div>

                      {projectReport.recommendations && (
                        <div>
                          <p className="text-sm text-slate-600 dark:text-slate-400 mb-1">建议</p>
                          <div className="text-slate-900 dark:text-slate-100 whitespace-pre-wrap text-sm">
                            {projectReport.recommendations}
                          </div>
                        </div>
                      )}

                      {projectReport.metrics && (() => {
                        const metrics = projectReport.metrics as any
                        const simpleMetrics = Object.entries(metrics).filter(([, value]) =>
                          typeof value !== 'object' || value === null
                        )

                        return simpleMetrics.length > 0 ? (
                          <div>
                            <p className="text-sm text-slate-600 dark:text-slate-400 mb-2">详细指标</p>
                            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                              {simpleMetrics.map(([key, value]) => (
                                <div
                                  key={key}
                                  className="p-3 bg-white dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-700"
                                >
                                  <p className="text-xs text-slate-600 dark:text-slate-400">{key}</p>
                                  <p className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                                    {String(value)}
                                  </p>
                                </div>
                              ))}
                            </div>
                          </div>
                        ) : null
                      })()}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default ProjectPage
