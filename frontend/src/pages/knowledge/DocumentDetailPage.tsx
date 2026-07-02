import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  AlertCircle,
  ArrowLeft,
  Calendar,
  Clock3,
  Database,
  Download,
  File,
  FileText,
  Hash,
  Layers,
  RefreshCw,
  Trash2,
} from 'lucide-react'
import Card, { CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import { knowledgeService } from '../../services/knowledge.service'
import type { Document, DocumentChunk } from '../../types'

const darkCardClass = 'border-white/10 bg-white/[0.06] text-slate-100 backdrop-blur'
const mutedPanelClass = 'rounded-lg border border-white/10 bg-slate-950/35'

const DocumentDetailPage: React.FC = () => {
  const { documentId } = useParams<{ documentId: string }>()
  const navigate = useNavigate()
  const [metadata, setMetadata] = useState<Document | null>(null)
  const [chunks, setChunks] = useState<DocumentChunk[]>([])
  const [loading, setLoading] = useState(true)
  const [chunksLoading, setChunksLoading] = useState(false)
  const [chunkPage, setChunkPage] = useState(0)
  const [totalChunkPages, setTotalChunkPages] = useState(0)

  const docId = documentId ? parseInt(documentId, 10) : undefined

  const loadMetadata = async () => {
    if (!docId) return

    try {
      setLoading(true)
      const response = await knowledgeService.getDocument(docId)
      if (response.code === 200 && response.data) {
        setMetadata(response.data)
      }
    } catch (error) {
      console.error('Failed to load document metadata:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadChunks = async (page: number = 0) => {
    if (!docId) return

    try {
      setChunksLoading(true)
      const response = await knowledgeService.getDocumentChunks(docId, page, 10)
      if (response.code === 200 && response.data) {
        if (page === 0) {
          setChunks(response.data.data || [])
        } else {
          setChunks((prev) => [...prev, ...(response.data.data || [])])
        }
        setTotalChunkPages(response.data.totalPages || 0)
      }
    } catch (error) {
      console.error('Failed to load chunks:', error)
    } finally {
      setChunksLoading(false)
    }
  }

  useEffect(() => {
    setChunkPage(0)
    loadMetadata()
    loadChunks(0)
  }, [docId])

  const handleDownload = async () => {
    if (!docId) return

    try {
      const blob = await knowledgeService.downloadDocument(docId)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = metadata?.fileName || 'document'
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      console.error('Failed to download document:', error)
      alert('下载文档失败')
    }
  }

  const handleDelete = async () => {
    if (!docId) return
    if (!confirm('确定要删除此文档吗？')) return

    try {
      await knowledgeService.deleteDocument(docId)
      navigate('/knowledge/documents')
    } catch (error) {
      console.error('Failed to delete document:', error)
      alert('删除文档失败')
    }
  }

  const handleReindex = async () => {
    if (!docId || metadata?.status === 'PROCESSING') return

    try {
      await knowledgeService.reindexDocument(docId)
      alert('重新索引已开始')
      setChunkPage(0)
      loadMetadata()
      loadChunks(0)
    } catch (error) {
      console.error('Failed to re-index document:', error)
      alert('重新索引失败')
    }
  }

  const getStatusBadge = (status: Document['status']) => {
    const variants: Record<Document['status'], 'success' | 'warning' | 'error' | 'info' | 'default'> = {
      UPLOADED: 'info',
      PROCESSING: 'warning',
      INDEXED: 'success',
      FAILED: 'error',
    }
    const badgeClasses: Record<Document['status'], string> = {
      UPLOADED: 'border border-sky-300/25 bg-sky-300/10 text-sky-200',
      PROCESSING: 'border border-amber-300/25 bg-amber-300/10 text-amber-200',
      INDEXED: 'border border-emerald-300/25 bg-emerald-300/10 text-emerald-200',
      FAILED: 'border border-rose-300/25 bg-rose-300/10 text-rose-200',
    }
    const labels: Record<Document['status'], string> = {
      UPLOADED: '已上传',
      PROCESSING: '处理中',
      INDEXED: '已索引',
      FAILED: '失败',
    }
    return (
      <Badge variant={variants[status]} className={badgeClasses[status]}>
        {labels[status]}
      </Badge>
    )
  }

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return '0 B'
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  }

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-'
    return new Date(dateString).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-[1500px] p-4 text-slate-100 sm:p-6 lg:p-8">
        <div className="animate-pulse space-y-4">
          <div className="h-10 w-1/3 rounded-lg bg-white/10" />
          <div className="h-48 rounded-lg border border-white/10 bg-white/[0.06]" />
          <div className="h-64 rounded-lg border border-white/10 bg-white/[0.06]" />
        </div>
      </div>
    )
  }

  if (!metadata) {
    return (
      <div className="mx-auto max-w-[1500px] p-4 text-slate-100 sm:p-6 lg:p-8">
        <div className="rounded-lg border border-white/10 bg-white/[0.06] py-16 text-center backdrop-blur">
          <AlertCircle className="mx-auto mb-4 h-12 w-12 text-slate-500" />
          <p className="text-slate-400">未找到文档</p>
          <Button
            variant="ghost"
            className="mt-4 text-slate-300 hover:bg-white/10 hover:text-white"
            onClick={() => navigate('/knowledge/documents')}
          >
            <ArrowLeft className="h-4 w-4" />
            返回文档列表
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-[1500px] p-4 text-slate-100 sm:p-6 lg:p-8">
      <div className="mb-5">
        <Button
          variant="ghost"
          className="text-slate-300 hover:bg-white/10 hover:text-white"
          onClick={() => navigate('/knowledge/documents')}
        >
          <ArrowLeft className="h-4 w-4" />
          返回文档列表
        </Button>
      </div>

      <section className="relative mb-6 overflow-hidden rounded-lg border border-white/10 bg-white/[0.06] p-6 shadow-[0_30px_100px_rgba(2,6,23,0.3)] backdrop-blur-xl lg:p-8">
        <div className="absolute inset-0 cv-grid opacity-30" />
        <div className="absolute inset-x-0 top-0 h-56 cv-scanline opacity-45" />
        <div className="relative flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
          <div className="flex min-w-0 items-start gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-lg border border-sky-300/25 bg-sky-300/10 text-sky-300">
              <FileText className="h-8 w-8" />
            </div>
            <div className="min-w-0">
              <div className="mb-3 flex flex-wrap items-center gap-2">
                <span className="cv-kicker">document detail</span>
                {getStatusBadge(metadata.status)}
              </div>
              <h1 className="truncate text-3xl font-black tracking-normal text-white">
                {metadata.title || metadata.fileName}
              </h1>
              <p className="mt-2 text-sm text-slate-400">{metadata.fileType}</p>
            </div>
          </div>

          <div className="flex flex-wrap gap-2">
            <Button
              variant="outline"
              className="whitespace-nowrap border-white/15 text-slate-100 hover:bg-white/10"
              onClick={handleReindex}
              disabled={metadata.status === 'PROCESSING'}
              title={metadata.status === 'PROCESSING' ? '文档处理中，完成后可重新索引' : '重新索引文档'}
            >
              <RefreshCw className={`h-4 w-4 ${metadata.status === 'PROCESSING' ? 'animate-spin' : ''}`} />
              重新索引
            </Button>
            <Button
              variant="outline"
              className="whitespace-nowrap border-white/15 text-slate-100 hover:bg-white/10"
              onClick={handleDownload}
            >
              <Download className="h-4 w-4" />
              下载
            </Button>
            <Button
              variant="outline"
              className="whitespace-nowrap border-rose-300/35 text-rose-300 hover:bg-rose-300/10"
              onClick={handleDelete}
            >
              <Trash2 className="h-4 w-4" />
              删除
            </Button>
          </div>
        </div>
      </section>

      <div className="mb-6 grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <Card variant="bordered" className={darkCardClass}>
          <CardHeader>
            <CardTitle className="text-white">文档信息</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="space-y-4">
              <div className="flex items-center justify-between gap-4">
                <dt className="flex items-center gap-2 text-sm text-slate-400">
                  <File className="h-4 w-4 text-emerald-300" />
                  文件大小
                </dt>
                <dd className="font-mono text-sm font-medium text-slate-100">
                  {formatFileSize(metadata.fileSize)}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="flex items-center gap-2 text-sm text-slate-400">
                  <Calendar className="h-4 w-4 text-sky-300" />
                  上传日期
                </dt>
                <dd className="text-sm font-medium text-slate-100">
                  {formatDate(metadata.createdAt)}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="text-sm text-slate-400">状态</dt>
                <dd>{getStatusBadge(metadata.status)}</dd>
              </div>
            </dl>
          </CardContent>
        </Card>

        <Card variant="bordered" className={darkCardClass}>
          <CardHeader>
            <CardTitle className="text-white">处理信息</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="space-y-4">
              <div className="flex items-center justify-between gap-4">
                <dt className="flex items-center gap-2 text-sm text-slate-400">
                  <Hash className="h-4 w-4 text-violet-300" />
                  索引时间
                </dt>
                <dd className="text-sm font-medium text-slate-100">
                  {formatDate(metadata.indexedAt)}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="flex items-center gap-2 text-sm text-slate-400">
                  <Layers className="h-4 w-4 text-amber-300" />
                  已加载分块
                </dt>
                <dd className="font-mono text-sm font-medium text-slate-100">
                  {chunks.length}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="flex items-center gap-2 text-sm text-slate-400">
                  <Clock3 className="h-4 w-4 text-emerald-300" />
                  处理状态
                </dt>
                <dd className="text-sm font-medium text-slate-100">
                  {metadata.status === 'PROCESSING' ? '处理中' : '可查看'}
                </dd>
              </div>
            </dl>
          </CardContent>
        </Card>

        {metadata.errorMessage && (
          <Card variant="bordered" className="border-rose-300/25 bg-rose-300/10 text-rose-100 backdrop-blur xl:col-span-2">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-rose-200">
                <AlertCircle className="h-5 w-5" />
                错误
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-rose-100/90">{metadata.errorMessage}</p>
            </CardContent>
          </Card>
        )}
      </div>

      <Card variant="bordered" className={darkCardClass}>
        <CardHeader>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <CardTitle className="text-white">文档分块</CardTitle>
              <CardDescription className="text-slate-400">从此文档中提取的内容块</CardDescription>
            </div>
            <div className="flex items-center gap-2 rounded-lg border border-white/10 bg-slate-950/35 px-3 py-2 text-xs text-slate-400">
              <Database className="h-4 w-4 text-emerald-300" />
              RAG Context
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {chunks.length === 0 ? (
            <div className={`${mutedPanelClass} py-12 text-center text-slate-400`}>
              {metadata.status === 'PROCESSING' ? (
                <p>文档正在处理中...</p>
              ) : metadata.status === 'FAILED' ? (
                <p>处理失败，请重新上传文档。</p>
              ) : (
                <p>暂无分块内容</p>
              )}
            </div>
          ) : (
            <div className="space-y-4">
              {chunks.map((chunk) => (
                <div
                  key={chunk.id}
                  className="rounded-lg border border-white/10 bg-slate-950/35 p-4"
                >
                  <div className="mb-2 flex items-center justify-between gap-3">
                    <span className="text-sm font-medium text-slate-300">
                      分块 #{chunk.chunkIndex + 1}
                    </span>
                    <span className="shrink-0 font-mono text-xs text-slate-500">
                      {chunk.content.length} 字符
                    </span>
                  </div>
                  <p className="line-clamp-3 text-sm leading-6 text-slate-300">
                    {chunk.content}
                  </p>
                </div>
              ))}

              {chunksLoading && (
                <div className="py-4 text-center text-slate-500">加载更多分块中...</div>
              )}

              {chunkPage + 1 < totalChunkPages && !chunksLoading && (
                <div className="text-center">
                  <Button
                    variant="outline"
                    className="border-white/15 text-slate-100 hover:bg-white/10"
                    onClick={() => {
                      const nextPage = chunkPage + 1
                      setChunkPage(nextPage)
                      loadChunks(nextPage)
                    }}
                  >
                    加载更多
                  </Button>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

export default DocumentDetailPage
