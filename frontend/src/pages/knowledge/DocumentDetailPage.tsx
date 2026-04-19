import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  Download,
  Trash2,
  RefreshCw,
  FileText,
  Calendar,
  File,
  Hash,
  AlertCircle,
} from 'lucide-react'
import Card, { CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import { knowledgeService } from '../../services/knowledge.service'
import type { Document, DocumentChunk } from '../../types'

const DocumentDetailPage: React.FC = () => {
  const { documentId } = useParams<{ documentId: string }>()
  const navigate = useNavigate()
  const [metadata, setMetadata] = useState<Document | null>(null)
  const [chunks, setChunks] = useState<DocumentChunk[]>([])
  const [loading, setLoading] = useState(true)
  const [chunksLoading, setChunksLoading] = useState(false)
  const [chunkPage, setChunkPage] = useState(0)
  const [totalChunkPages, setTotalChunkPages] = useState(0)

  // Convert string documentId to number for API calls
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
    if (!docId) return

    try {
      await knowledgeService.reindexDocument(docId)
      alert('重新索引已开始')
      loadMetadata()
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
    const labels: Record<Document['status'], string> = {
      UPLOADED: '已上传',
      PROCESSING: '处理中',
      INDEXED: '已索引',
      FAILED: '失败',
    }
    return <Badge variant={variants[status]}>{labels[status]}</Badge>
  }

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  }

  const formatDate = (dateString?: string) => {
    if (!dateString) return '-'
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  if (loading) {
    return (
      <div className="p-6 lg:p-8">
        <div className="animate-pulse">
          <div className="h-8 bg-slate-200 dark:bg-slate-700 rounded w-1/3 mb-4" />
          <div className="h-64 bg-slate-200 dark:bg-slate-700 rounded" />
        </div>
      </div>
    )
  }

  if (!metadata) {
    return (
      <div className="p-6 lg:p-8">
        <div className="text-center py-12">
          <AlertCircle className="h-12 w-12 text-slate-400 mx-auto mb-4" />
          <p className="text-slate-600 dark:text-slate-400">未找到文档</p>
          <Button variant="ghost" className="mt-4" onClick={() => navigate('/knowledge/documents')}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            返回文档列表
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="p-6 lg:p-8">
      {/* Header */}
      <div className="mb-6">
        <Button variant="ghost" className="mb-4" onClick={() => navigate('/knowledge/documents')}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          返回文档列表
        </Button>
        <div className="flex items-start justify-between">
          <div className="flex items-start gap-4">
            <div className="rounded-xl bg-primary-100 p-3 text-primary-600">
              <FileText className="h-8 w-8" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
                {metadata.title || metadata.fileName}
              </h1>
              <p className="text-slate-600 dark:text-slate-400 mt-1">{metadata.fileType}</p>
            </div>
          </div>
          <div className="flex gap-2">
            {metadata.status === 'INDEXED' && (
              <Button variant="outline" onClick={handleReindex}>
                <RefreshCw className="h-4 w-4 mr-2" />
                重新索引
              </Button>
            )}
            <Button variant="outline" onClick={handleDownload}>
              <Download className="h-4 w-4 mr-2" />
              下载
            </Button>
            <Button variant="outline" className="text-red-600 hover:bg-red-50" onClick={handleDelete}>
              <Trash2 className="h-4 w-4 mr-2" />
              删除
            </Button>
          </div>
        </div>
      </div>

      {/* Metadata */}
      <div className="grid gap-6 lg:grid-cols-3 mb-6">
        <Card variant="bordered">
          <CardHeader>
            <CardTitle>文档信息</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="space-y-3">
              <div className="flex items-center justify-between">
                <dt className="text-sm text-slate-500 flex items-center gap-2">
                  <File className="h-4 w-4" />
                  文件大小
                </dt>
                <dd className="text-sm font-medium text-slate-900 dark:text-slate-100">
                  {formatFileSize(metadata.fileSize)}
                </dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-sm text-slate-500 flex items-center gap-2">
                  <Calendar className="h-4 w-4" />
                  上传日期
                </dt>
                <dd className="text-sm font-medium text-slate-900 dark:text-slate-100">
                  {formatDate(metadata.createdAt)}
                </dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-sm text-slate-500">状态</dt>
                <dd>{getStatusBadge(metadata.status)}</dd>
              </div>
            </dl>
          </CardContent>
        </Card>

        <Card variant="bordered">
          <CardHeader>
            <CardTitle>处理信息</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="space-y-3">
              <div className="flex items-center justify-between">
                <dt className="text-sm text-slate-500 flex items-center gap-2">
                  <Hash className="h-4 w-4" />
                  索引时间
                </dt>
                <dd className="text-sm font-medium text-slate-900 dark:text-slate-100">
                  {formatDate(metadata.indexedAt)}
                </dd>
              </div>
            </dl>
          </CardContent>
        </Card>

        {metadata.errorMessage && (
          <Card variant="bordered" className="border-red-200">
            <CardHeader>
              <CardTitle className="text-red-600 flex items-center gap-2">
                <AlertCircle className="h-5 w-5" />
                错误
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-red-600">{metadata.errorMessage}</p>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Indexed Chunks */}
      <Card variant="bordered">
        <CardHeader>
          <CardTitle>文档分块</CardTitle>
          <CardDescription>从此文档中提取的内容块</CardDescription>
        </CardHeader>
        <CardContent>
          {chunks.length === 0 ? (
            <div className="text-center py-8 text-slate-500">
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
                  className="p-4 bg-slate-50 dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-700"
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium text-slate-600 dark:text-slate-400">
                      分块 #{chunk.chunkIndex + 1}
                    </span>
                    <span className="text-xs text-slate-500">
                      {chunk.content.length} 字符
                    </span>
                  </div>
                  <p className="text-sm text-slate-700 dark:text-slate-300 line-clamp-3">
                    {chunk.content}
                  </p>
                </div>
              ))}

              {chunksLoading && (
                <div className="text-center py-4 text-slate-500">加载更多分块中...</div>
              )}

              {chunkPage + 1 < totalChunkPages && !chunksLoading && (
                <div className="text-center">
                  <Button
                    variant="outline"
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
