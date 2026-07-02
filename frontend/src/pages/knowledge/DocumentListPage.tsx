import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Archive,
  BookOpen,
  CheckCircle,
  Clock3,
  Database,
  FileText,
  Upload,
  Search,
  Trash2,
  RefreshCw,
  Image as ImageIcon,
  FileCode,
  FileSpreadsheet,
  Presentation,
  File as FileIcon,
} from 'lucide-react'
import Card, { CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Input from '../../components/ui/Input'
import WorkbenchHero from '../../components/ui/WorkbenchHero'
import { knowledgeService } from '../../services/knowledge.service'
import type { Document } from '../../types'

interface UploadModalProps {
  open: boolean
  onClose: () => void
  onUploadComplete: (document: Document) => void
}

const UploadModal: React.FC<UploadModalProps> = ({ open, onClose, onUploadComplete }) => {
  const [file, setFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState(0)
  const [dragOver, setDragOver] = useState(false)
  const fileInputRef = React.useRef<HTMLInputElement>(null)

  const handleFileSelect = (selectedFile: File) => {
    const maxSize = 100 * 1024 * 1024 // 100MB
    if (selectedFile.size > maxSize) {
      alert('文件大小超过100MB限制')
      return
    }
    setFile(selectedFile)
  }

  const handleUpload = async () => {
    if (!file) return

    try {
      setUploading(true)
      setProgress(0)

      const response = await knowledgeService.uploadDocument(file, (prog) => {
        setProgress(prog)
      })

      if (response.code === 200 && response.data) {
        onUploadComplete(response.data)
        handleClose()
      }
    } catch (error) {
      console.error('Upload failed:', error)
      alert('上传失败，请重试')
    } finally {
      setUploading(false)
      setProgress(0)
    }
  }

  const handleClose = () => {
    setFile(null)
    setProgress(0)
    setUploading(false)
    onClose()
  }

  const getFileIcon = (fileName: string) => {
    const ext = fileName.split('.').pop()?.toLowerCase()
    switch (ext) {
      case 'pdf':
        return <FileText className="h-8 w-8 text-red-500" />
      case 'png':
      case 'jpg':
      case 'jpeg':
      case 'gif':
        return <ImageIcon className="h-8 w-8 text-purple-500" />
      case 'doc':
      case 'docx':
        return <FileText className="h-8 w-8 text-blue-500" />
      case 'xls':
      case 'xlsx':
        return <FileSpreadsheet className="h-8 w-8 text-green-500" />
      case 'ppt':
      case 'pptx':
        return <Presentation className="h-8 w-8 text-orange-500" />
      case 'txt':
      case 'md':
      case 'js':
      case 'ts':
      case 'py':
      case 'java':
        return <FileCode className="h-8 w-8 text-gray-500" />
      default:
        return <FileIcon className="h-8 w-8 text-gray-400" />
    }
  }

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  }

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md overflow-hidden rounded-lg border border-white/10 bg-slate-950 text-slate-100 shadow-[0_30px_100px_rgba(2,6,23,0.65)]">
        <div className="border-b border-white/10 bg-white/[0.04] p-6">
          <div className="flex items-center gap-3">
            <div className="rounded-lg border border-emerald-300/25 bg-emerald-300/10 p-2 text-emerald-300">
              <Upload className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-xl font-semibold text-white">上传文档</h3>
              <p className="mt-1 text-sm text-slate-400">解析、切块、向量化后进入知识库索引</p>
            </div>
          </div>
        </div>

        <div className="p-6">
          {!file ? (
            <div
              className={`cursor-pointer rounded-lg border border-dashed p-8 text-center transition-colors ${
                dragOver
                  ? 'border-emerald-300 bg-emerald-300/10'
                  : 'border-white/15 bg-white/[0.04] hover:border-emerald-300/60 hover:bg-white/[0.07]'
              }`}
              onDragOver={(e) => {
                e.preventDefault()
                setDragOver(true)
              }}
              onDragLeave={() => setDragOver(false)}
              onDrop={(e) => {
                e.preventDefault()
                setDragOver(false)
                const droppedFile = e.dataTransfer.files[0]
                if (droppedFile) handleFileSelect(droppedFile)
              }}
              onClick={() => fileInputRef.current?.click()}
            >
              <input
                ref={fileInputRef}
                type="file"
                className="hidden"
                accept=".pdf,.doc,.docx,.txt,.md,.ppt,.pptx,.xls,.xlsx,.png,.jpg,.jpeg,.gif"
                onChange={(e) => {
                  const selectedFile = e.target.files?.[0]
                  if (selectedFile) handleFileSelect(selectedFile)
                }}
              />
              <Upload className="mx-auto mb-4 h-12 w-12 text-emerald-300" />
              <p className="mb-2 text-slate-200">
                拖放文件到此处，或点击选择文件
              </p>
              <p className="text-xs text-slate-500">PDF、Word、Markdown、PPT、Excel、图片，最大 100MB</p>
            </div>
          ) : (
            <div className="flex items-center gap-4 rounded-lg border border-white/10 bg-white/[0.05] p-4">
              <div className="flex-shrink-0">{getFileIcon(file.name)}</div>
              <div className="flex-1 min-w-0">
                <p className="truncate font-medium text-white">{file.name}</p>
                <p className="text-sm text-slate-500">{formatFileSize(file.size)}</p>
              </div>
              <button
                onClick={() => setFile(null)}
                className="rounded-lg p-2 text-slate-400 transition hover:bg-white/10 hover:text-rose-300"
                aria-label="移除已选择文件"
              >
                <Trash2 className="h-5 w-5" />
              </button>
            </div>
          )}

          {uploading && (
            <div className="mt-4">
              <div className="mb-2 flex justify-between text-sm text-slate-400">
                <span>上传中...</span>
                <span>{progress}%</span>
              </div>
              <div className="h-2 overflow-hidden rounded-full bg-white/10">
                <div
                  className="h-full bg-emerald-300 transition-all duration-300"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 border-t border-white/10 bg-white/[0.03] p-6">
          <Button variant="ghost" className="text-slate-300 hover:bg-white/10 hover:text-white" onClick={handleClose} disabled={uploading}>
            取消
          </Button>
          <Button className="bg-emerald-300 text-slate-950 hover:bg-emerald-200" onClick={handleUpload} disabled={!file || uploading}>
            {uploading ? '上传中...' : '上传'}
          </Button>
        </div>
      </div>
    </div>
  )
}

const DocumentListPage: React.FC = () => {
  const navigate = useNavigate()
  const [documents, setDocuments] = useState<Document[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [currentPage, setCurrentPage] = useState(0)
  const [pageSize] = useState(20)
  const [totalPages, setTotalPages] = useState(0)
  const [uploadModalOpen, setUploadModalOpen] = useState(false)

  const loadDocuments = async () => {
    try {
      setLoading(true)
      const response = await knowledgeService.getDocuments(currentPage, pageSize)
      if (response.code === 200 && response.data) {
        setDocuments(response.data.data || [])
        setTotalPages(response.data.totalPages || 0)
      }
    } catch (error) {
      console.error('Failed to load documents:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadDocuments()
  }, [currentPage, pageSize])

  const handleDelete = async (documentId: number) => {
    if (!confirm('确定要删除此文档吗？')) return

    try {
      await knowledgeService.deleteDocument(documentId)
      setDocuments((prev) => prev.filter((doc) => doc.id !== documentId))
    } catch (error) {
      console.error('Failed to delete document:', error)
      alert('删除文档失败')
    }
  }

  const handleReindex = async (documentId: number) => {
    try {
      await knowledgeService.reindexDocument(documentId)
      alert('重新索引已开始')
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
    return <Badge variant={variants[status]} className={badgeClasses[status]}>{labels[status]}</Badge>
  }

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  const filteredDocuments = documents.filter(
    (doc) =>
      doc.fileName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (doc.title && doc.title.toLowerCase().includes(searchTerm.toLowerCase()))
  )

  const indexedCount = documents.filter((doc) => doc.status === 'INDEXED').length
  const processingCount = documents.filter((doc) => doc.status === 'PROCESSING').length
  const failedCount = documents.filter((doc) => doc.status === 'FAILED').length
  const totalSize = documents.reduce((sum, doc) => sum + (doc.fileSize || 0), 0)

  return (
    <div className="mx-auto max-w-[1500px] p-4 text-slate-100 sm:p-6 lg:p-8">
      <WorkbenchHero
        kicker="knowledge ingestion"
        title="知识库"
        description="集中管理面试项目的参考资料、设计文档和技术说明。文档上传后会进入解析、切块、向量化和 Elasticsearch 索引流程，为搜索和问答提供同一份可信上下文。"
        metrics={[
          { icon: Archive, label: '文档总数', value: documents.length, tone: 'text-emerald-300' },
          { icon: CheckCircle, label: '已索引', value: indexedCount, tone: 'text-sky-300' },
          { icon: Clock3, label: '处理中', value: processingCount, tone: 'text-amber-300' },
          { icon: Database, label: '存储体量', value: formatFileSize(totalSize), tone: 'text-violet-300' },
          { icon: FileText, label: '异常文档', value: failedCount, tone: failedCount ? 'text-rose-300' : 'text-slate-400' },
        ]}
        action={
        <Button onClick={() => setUploadModalOpen(true)} className="gap-2 bg-emerald-300 text-slate-950 hover:bg-emerald-200">
          <Upload className="h-4 w-4" />
          上传文档
        </Button>
        }
      />

      {/* Search */}
      <div className="mb-6 rounded-lg border border-white/10 bg-white/[0.06] p-4 backdrop-blur">
        <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-center">
          <div className="relative">
            <Search className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-500" />
            <Input
              type="search"
              placeholder="按文件名或标题筛选知识库文档..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="h-11 border-white/10 bg-slate-950/45 pl-11 text-slate-100 placeholder:text-slate-600 focus-visible:border-emerald-300/60 focus-visible:ring-emerald-300/30"
            />
          </div>
          <div className="flex items-center gap-3 text-sm text-slate-400">
            <BookOpen className="h-4 w-4 text-emerald-300" />
            <span>当前显示 {filteredDocuments.length} / {documents.length} 个文档</span>
          </div>
        </div>
      </div>

      {/* Documents Table */}
      <Card variant="bordered" className="overflow-hidden border-white/10 bg-white/[0.06] text-slate-100 backdrop-blur">
        <CardContent className="p-0">
          {loading ? (
            <div className="p-10 text-center text-slate-400">加载文档中...</div>
          ) : filteredDocuments.length === 0 ? (
            <div className="p-12 text-center">
              <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-lg border border-white/10 bg-slate-950/45 text-slate-500">
                <FileText className="h-8 w-8" />
              </div>
              <p className="mb-2 font-semibold text-white">未找到文档</p>
              <p className="mb-4 text-sm text-slate-500">
                {searchTerm ? '尝试其他搜索词' : '上传您的第一个文档开始使用'}
              </p>
              {!searchTerm && (
                <Button onClick={() => setUploadModalOpen(true)} className="gap-2 bg-emerald-300 text-slate-950 hover:bg-emerald-200">
                  <Upload className="h-4 w-4" />
                  上传文档
                </Button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="border-b border-white/10 bg-slate-950/70">
                  <tr>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-slate-300">
                      名称
                    </th>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-slate-300">
                      大小
                    </th>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-slate-300">
                      上传日期
                    </th>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-slate-300">
                      状态
                    </th>
                    <th className="px-6 py-3 text-left text-sm font-semibold text-slate-300">
                      索引时间
                    </th>
                    <th className="px-6 py-3 text-right text-sm font-semibold text-slate-300">
                      操作
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/10">
                  {filteredDocuments.map((doc) => (
                    <tr
                      key={doc.id}
                      className="cursor-pointer transition hover:bg-white/[0.06]"
                      onClick={() => navigate(`/knowledge/documents/${doc.id}`)}
                    >
                      <td className="py-4 px-6">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg border border-emerald-300/20 bg-emerald-300/10 text-emerald-300">
                            <FileText className="h-5 w-5" />
                          </div>
                          <div className="min-w-0">
                            <p className="max-w-xs truncate font-medium text-white" title={doc.title || doc.fileName}>
                              {doc.title || doc.fileName}
                            </p>
                            <p className="text-xs text-slate-500">{doc.fileType}</p>
                          </div>
                        </div>
                      </td>
                      <td className="py-4 px-6 font-mono text-sm text-slate-400">
                        {formatFileSize(doc.fileSize)}
                      </td>
                      <td className="py-4 px-6 text-sm text-slate-400">
                        {formatDate(doc.createdAt)}
                      </td>
                      <td className="py-4 px-6">{getStatusBadge(doc.status)}</td>
                      <td className="py-4 px-6 text-sm text-slate-400">
                        {doc.indexedAt ? formatDate(doc.indexedAt) : '-'}
                      </td>
                      <td className="py-4 px-6">
                        <div
                          className="flex justify-end gap-2"
                          onClick={(e) => e.stopPropagation()}
                        >
                          {doc.status === 'INDEXED' && (
                            <button
                              onClick={() => handleReindex(doc.id)}
                              className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-emerald-300/10 hover:text-emerald-300"
                              title="重新索引"
                            >
                              <RefreshCw className="h-4 w-4" />
                            </button>
                          )}
                          <button
                            onClick={() => handleDelete(doc.id)}
                            className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-rose-300/10 hover:text-rose-300"
                            title="删除"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="mt-6 flex justify-center gap-2">
          <Button
            variant="ghost"
            className="text-slate-300 hover:bg-white/10 hover:text-white"
            onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
            disabled={currentPage === 0}
          >
            上一页
          </Button>
          <div className="flex items-center gap-2">
            {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
              let pageNum
              if (totalPages <= 5) {
                pageNum = i
              } else if (currentPage < 2) {
                pageNum = i
              } else if (currentPage > totalPages - 3) {
                pageNum = totalPages - 5 + i
              } else {
                pageNum = currentPage - 2 + i
              }

              return (
                <button
                  key={pageNum}
                  onClick={() => setCurrentPage(pageNum)}
                  className={`w-10 h-10 rounded-lg transition-colors ${
                    currentPage === pageNum
                      ? 'bg-emerald-300 text-slate-950'
                      : 'border border-white/10 bg-white/[0.05] text-slate-400 hover:bg-white/10 hover:text-white'
                  }`}
                >
                  {pageNum + 1}
                </button>
              )
            })}
          </div>
          <Button
            variant="ghost"
            className="text-slate-300 hover:bg-white/10 hover:text-white"
            onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={currentPage >= totalPages - 1}
          >
            下一页
          </Button>
        </div>
      )}

      {/* Upload Modal */}
      <UploadModal
        open={uploadModalOpen}
        onClose={() => setUploadModalOpen(false)}
        onUploadComplete={(doc) => {
          setDocuments((prev) => [doc, ...prev])
        }}
      />
    </div>
  )
}

export default DocumentListPage
