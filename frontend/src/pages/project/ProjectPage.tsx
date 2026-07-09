import React, { useState, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useSelector } from 'react-redux'
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
  FileSearch,
  BrainCircuit,
  Globe2,
  ShieldCheck,
  Sparkles,
} from 'lucide-react'
import Card, { CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import MarkdownReport from '../../components/MarkdownReport'
import { PROJECT_UPLOAD_CHUNK_SIZE, projectService } from '../../services/project.service'
import { selectUser } from '../../store/slices/authSlice'
import type { ProjectUploadProgress } from '../../services/project.service'
import type {
  ProjectInfo,
  ProjectStatusDTO,
  ProjectFile,
  ProjectReport,
} from '../../types'

const ProjectPage: React.FC = () => {
  const currentUser = useSelector(selectUser)
  const [projects, setProjects] = useState<ProjectInfo[]>([])
  const [selectedProject, setSelectedProject] = useState<ProjectInfo | null>(null)
  const [projectStatus, setProjectStatus] = useState<ProjectStatusDTO | null>(null)
  const [projectFiles, setProjectFiles] = useState<ProjectFile[]>([])
  const [projectReport, setProjectReport] = useState<ProjectReport | null>(null)
  const [loading, setLoading] = useState(true)
  const [projectLoadError, setProjectLoadError] = useState('')
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState<ProjectUploadProgress | null>(null)
  const [showUploadModal, setShowUploadModal] = useState(false)
  const [showDetailModal, setShowDetailModal] = useState(false)

  // Upload form state
  const [uploadFile, setUploadFile] = useState<File | null>(null)
  const [projectName, setProjectName] = useState('')
  const [description, setDescription] = useState('')
  const [visibility, setVisibility] = useState<'PRIVATE' | 'PUBLIC' | 'TEAM'>('PUBLIC')

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
      setProjectLoadError('')
      const response = await projectService.getProjectList(0, 20)
      if (response.code === 200 && response.data) {
        setProjects(response.data)
      } else {
        setProjects([])
        setProjectLoadError(response.message || '项目列表加载失败')
      }
    } catch (error: any) {
      console.error('Failed to load projects:', error)
      setProjects([])
      setProjectLoadError(error.response?.data?.message || error.message || '项目列表加载失败，请重新登录后再试')
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
    setUploadProgress({
      stage: 'initializing',
      percent: 0,
      uploadedChunks: 0,
      totalChunks: Math.ceil(uploadFile.size / PROJECT_UPLOAD_CHUNK_SIZE),
    })
    try {
      const response = await projectService.uploadProject(
        uploadFile,
        projectName,
        description,
        visibility,
        async (progress) => {
          setUploadProgress(progress)
          if (progress.projectId && progress.stage === 'uploading' && progress.uploadedChunks === 0) {
            await loadProjects()
          }
        }
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
      alert(error.response?.data?.message || error.message || '上传失败，请重试')
    } finally {
      setUploading(false)
      setUploadProgress(null)
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
      } else {
        alert(response.message || '删除失败：只有项目创建者可以删除项目')
      }
    } catch (error: any) {
      console.error('Delete failed:', error)
      alert(error.response?.data?.message || error.message || '删除失败：只有项目创建者可以删除项目')
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
    setVisibility('PUBLIC')
    setUploadProgress(null)
  }

  const getUploadStageText = () => {
    if (!uploadProgress) return ''
    if (uploadProgress.stage === 'initializing') return '正在创建项目记录...'
    if (uploadProgress.stage === 'completing') return '正在合并分片并启动分析...'
    return `正在上传分片 ${uploadProgress.uploadedChunks}/${uploadProgress.totalChunks}`
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

  const formatProgress = (progress?: number) => {
    if (!progress) return 0
    return progress <= 1 ? progress * 100 : progress
  }

  const getStatusVariant = (status: string): 'default' | 'success' | 'warning' | 'error' | 'info' => {
    const variants: Record<string, 'default' | 'success' | 'warning' | 'error' | 'info'> = {
      PENDING: 'default',
      ANALYZING: 'info',
      COMPLETED: 'success',
      FAILED: 'error',
    }
    return variants[status] || 'default'
  }

  const currentUserId = useMemo(() => {
    if (currentUser?.id) return String(currentUser.id)
    try {
      const storedUser = JSON.parse(localStorage.getItem('user') || '{}')
      return String(storedUser?.id || localStorage.getItem('userId') || '')
    } catch {
      return String(localStorage.getItem('userId') || '')
    }
  }, [currentUser?.id])

  const canDeleteProject = (project: ProjectInfo) => String(project.userId) === currentUserId

  const dashboardStats = useMemo(() => {
    return {
      totalProjects: projects.length,
      completedProjects: projects.filter((project) => project.status === 'COMPLETED').length,
      analyzingProjects: projects.filter((project) => project.status === 'ANALYZING' || project.status === 'PENDING').length,
      totalIssues: projects.reduce((sum, project) => sum + (project.totalIssues || 0), 0),
      publicProjects: projects.filter((project) => project.visibility === 'PUBLIC').length,
    }
  }, [projects])

  return (
    <div className="mx-auto max-w-[1500px] p-4 text-slate-100 sm:p-6 lg:p-8">
      <section className="relative mb-6 overflow-hidden rounded-lg border border-white/10 bg-white/[0.06] p-6 shadow-[0_30px_100px_rgba(2,6,23,0.3)] backdrop-blur-xl lg:p-8">
        <div className="absolute inset-0 cv-grid opacity-30" />
        <div className="relative flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            <div className="cv-kicker">
              <Sparkles className="h-4 w-4 text-amber-300" />
              project intelligence hub
            </div>
            <h1 className="mt-5 text-4xl font-black tracking-normal text-white">项目管理</h1>
            <p className="mt-3 text-base leading-7 text-slate-300">
              上传 ZIP 项目包后自动分片入库、后台异步分析。公开项目可被所有账号查看与审查，适合面试时预置演示项目；删除权限只保留给项目创建者。
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Link to="/review">
              <Button variant="outline" className="border-white/15 text-slate-100 hover:bg-white/10">
                <FileSearch className="h-4 w-4" />
                进入代码审查
              </Button>
            </Link>
            <Button onClick={() => setShowUploadModal(true)} className="bg-emerald-300 text-slate-950 hover:bg-emerald-200">
              <Plus className="h-4 w-4" />
              上传项目
            </Button>
          </div>
        </div>

        <div className="relative mt-6 grid gap-3 md:grid-cols-3">
          {[
            { icon: BrainCircuit, label: '后台异步分析', value: '队列处理中' },
            { icon: Globe2, label: '公开项目复用', value: `${dashboardStats.publicProjects} 个` },
            { icon: ShieldCheck, label: '删除权限保护', value: '仅创建者' },
          ].map((item) => {
            const Icon = item.icon
            return (
              <div key={item.label} className="rounded-lg border border-white/10 bg-slate-950/35 p-4">
                <div className="flex items-center gap-3">
                  <Icon className="h-5 w-5 text-emerald-300" />
                  <span className="text-sm text-slate-400">{item.label}</span>
                </div>
                <p className="mt-2 font-mono text-lg font-bold text-white">{item.value}</p>
              </div>
            )
          })}
        </div>
      </section>

      <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-lg border border-white/10 bg-white/[0.06] p-4 backdrop-blur">
          <p className="text-sm text-slate-400">项目总数</p>
          <p className="mt-1 text-2xl font-black text-white">{dashboardStats.totalProjects}</p>
        </div>
        <div className="rounded-lg border border-emerald-300/25 bg-emerald-300/10 p-4">
          <p className="text-sm text-emerald-100/80">已完成</p>
          <p className="mt-1 text-2xl font-black text-emerald-200">{dashboardStats.completedProjects}</p>
        </div>
        <div className="rounded-lg border border-sky-300/25 bg-sky-300/10 p-4">
          <p className="text-sm text-sky-100/80">进行中</p>
          <p className="mt-1 text-2xl font-black text-sky-200">{dashboardStats.analyzingProjects}</p>
        </div>
        <div className="rounded-lg border border-amber-300/25 bg-amber-300/10 p-4">
          <p className="text-sm text-amber-100/80">累计问题</p>
          <p className="mt-1 text-2xl font-black text-amber-200">{dashboardStats.totalIssues}</p>
        </div>
      </div>

      <Card variant="bordered" className="border-white/10 bg-white/[0.06] text-slate-100 backdrop-blur">
        <CardHeader>
          <CardTitle className="text-white">可访问项目</CardTitle>
          <CardDescription className="text-slate-400">查看自己的项目和公开演示项目，分析完成后可直接进入文件级审查</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="py-12 text-center text-slate-400">加载中...</div>
          ) : projectLoadError ? (
            <div className="rounded-lg border border-amber-300/25 bg-amber-300/10 p-6 text-center">
              <AlertCircle className="mx-auto mb-3 h-10 w-10 text-amber-300" />
              <h3 className="mb-2 text-lg font-semibold text-white">项目列表加载失败</h3>
              <p className="mx-auto mb-4 max-w-xl text-sm text-amber-100/80">
                {projectLoadError}
              </p>
              <Button
                variant="outline"
                className="border-white/15 text-slate-100 hover:bg-white/10"
                onClick={loadProjects}
              >
                <RefreshCw className="h-4 w-4" />
                重新加载
              </Button>
            </div>
          ) : projects.length === 0 ? (
            <div className="text-center py-12">
              <FolderKanban className="mx-auto mb-4 h-16 w-16 text-slate-600" />
              <h3 className="mb-2 text-lg font-medium text-white">
                还没有项目
              </h3>
              <p className="mb-4 text-slate-400">
                上传你的第一个ZIP项目文件开始分析
              </p>
              <Button onClick={() => setShowUploadModal(true)} className="bg-emerald-300 text-slate-950 hover:bg-emerald-200">
                <Upload className="h-4 w-4 mr-2" />
                上传项目
              </Button>
            </div>
          ) : (
            <div className="space-y-3">
              {projects.map((project) => (
                <div
                  key={project.id}
                  className="flex flex-col gap-4 rounded-lg border border-white/10 bg-slate-950/35 p-4 transition hover:border-emerald-300/30 hover:bg-white/[0.07] lg:flex-row lg:items-center"
                >
                  <div className={`rounded-full p-2 ${
                    project.status === 'COMPLETED'
                      ? 'bg-emerald-300/15 text-emerald-300'
                      : project.status === 'ANALYZING'
                      ? 'bg-sky-300/15 text-sky-300'
                      : project.status === 'FAILED'
                      ? 'bg-rose-300/15 text-rose-300'
                      : 'bg-white/10 text-slate-300'
                  }`}>
                    {getStatusIcon(project.status)}
                  </div>

                  <div className="flex-1 min-w-0">
                    <h4 className="truncate font-semibold text-white">
                      {project.projectName}
                    </h4>
                    <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-slate-400">
                      <span>{project.totalFiles} 个文件</span>
                      <span>•</span>
                      <span>{project.totalSize ? formatFileSize(project.totalSize) : '大小未知'}</span>
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
                    <Badge size="sm" variant={getStatusVariant(project.status)}>
                      {project.status === 'PENDING' && '等待中'}
                      {project.status === 'ANALYZING' && '分析中'}
                      {project.status === 'COMPLETED' && '已完成'}
                      {project.status === 'FAILED' && '失败'}
                    </Badge>

                    {project.visibility === 'PUBLIC' && (
                      <Badge size="sm" variant="info">
                        公开
                      </Badge>
                    )}

                    {project.totalIssues > 0 && (
                      <Badge size="sm" variant="warning">
                        {project.totalIssues} 个问题
                      </Badge>
                    )}

                    <Button
                      size="sm"
                      variant="outline"
                      className="border-white/15 text-slate-100 hover:bg-white/10"
                      onClick={() => openProjectDetail(project)}
                    >
                      <Eye className="h-4 w-4" />
                      详情
                    </Button>

                    {canDeleteProject(project) && (
                      <Button
                        size="sm"
                        variant="ghost"
                        className="text-slate-300 hover:bg-white/10"
                        onClick={() => handleDelete(project.id, project.projectName)}
                      >
                        <Trash2 className="h-4 w-4 text-error-600" />
                      </Button>
                    )}
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
                  if (uploading) return
                  setShowUploadModal(false)
                  resetUploadForm()
                }}
                disabled={uploading}
                className="text-slate-400 hover:text-slate-600 disabled:cursor-not-allowed disabled:opacity-40"
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
                  disabled={uploading}
                  onChange={(e) => {
                    const file = e.target.files?.[0] || null
                    setUploadFile(file)
                    if (file && !projectName.trim()) {
                      setProjectName(file.name.replace(/\.zip$/i, ''))
                    }
                  }}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-2 focus:ring-primary-200 dark:border-slate-700 dark:bg-slate-800"
                />
                {uploadFile && (
                  <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
                    已选择: {uploadFile.name} ({formatFileSize(uploadFile.size)})
                  </p>
                )}
                <p className="mt-1 text-xs text-slate-500">
                  支持 100MB 以内 ZIP，上传后后台会异步分析，不需要停在当前页面等待。
                </p>
              </div>

              {uploadProgress && (
                <div className="rounded-lg border border-primary-200 bg-primary-50 p-3 dark:border-primary-900/40 dark:bg-primary-900/20">
                  <div className="mb-2 flex items-center justify-between text-sm">
                    <span className="font-medium text-primary-700 dark:text-primary-300">
                      {getUploadStageText()}
                    </span>
                    <span className="tabular-nums text-primary-700 dark:text-primary-300">
                      {uploadProgress.percent}%
                    </span>
                  </div>
                  <div className="h-2 overflow-hidden rounded-full bg-white dark:bg-slate-800">
                    <div
                      className="h-full rounded-full bg-primary-600 transition-all duration-300"
                      style={{ width: `${uploadProgress.percent}%` }}
                    />
                  </div>
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                  项目名称 *
                </label>
                <input
                  type="text"
                  value={projectName}
                  disabled={uploading}
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
                  disabled={uploading}
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
                  disabled={uploading}
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
                    if (uploading) return
                    setShowUploadModal(false)
                    resetUploadForm()
                  }}
                  disabled={uploading}
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
                  {uploading ? '上传中' : '上传并分析'}
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
                        {formatProgress(projectStatus.progress).toFixed(0)}%
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
                          <th className="px-4 py-2 text-right font-medium text-slate-700 dark:text-slate-300">审查</th>
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
                            <td className="px-4 py-2 text-right">
                              {file.reviewId ? (
                                <Link to={`/review?reviewId=${file.reviewId}`}>
                                  <Button size="sm" variant="outline">
                                    <FileSearch className="h-4 w-4" />
                                    查看
                                  </Button>
                                </Link>
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
