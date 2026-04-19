import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
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
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white dark:bg-slate-800 rounded-xl shadow-xl w-full max-w-md">
        <div className="p-6 border-b border-slate-200 dark:border-slate-700">
          <h3 className="text-xl font-semibold text-slate-900 dark:text-slate-100">上传文档</h3>
          <p className="text-sm text-slate-500 mt-1">支持的格式：PDF、DOC、DOCX、TXT、MD、PPT、PPTX、XLS、XLSX（最大100MB）</p>
        </div>

        <div className="p-6">
          {!file ? (
            <div
              className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                dragOver
                  ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                  : 'border-slate-300 dark:border-slate-600 hover:border-primary-400'
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
              <Upload className="h-12 w-12 mx-auto text-slate-400 mb-4" />
              <p className="text-slate-600 dark:text-slate-400 mb-2">
                拖放文件到此处，或点击选择文件
              </p>
              <p className="text-xs text-slate-500">最大文件大小：100MB</p>
            </div>
          ) : (
            <div className="flex items-center gap-4 p-4 bg-slate-50 dark:bg-slate-900 rounded-lg">
              <div className="flex-shrink-0">{getFileIcon(file.name)}</div>
              <div className="flex-1 min-w-0">
                <p className="font-medium text-slate-900 dark:text-slate-100 truncate">{file.name}</p>
                <p className="text-sm text-slate-500">{formatFileSize(file.size)}</p>
              </div>
              <button
                onClick={() => setFile(null)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-300"
              >
                <Trash2 className="h-5 w-5" />
              </button>
            </div>
          )}

          {uploading && (
            <div className="mt-4">
              <div className="flex justify-between text-sm text-slate-600 dark:text-slate-400 mb-2">
                <span>上传中...</span>
                <span>{progress}%</span>
              </div>
              <div className="h-2 bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden">
                <div
                  className="h-full bg-primary-500 transition-all duration-300"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>
          )}
        </div>

        <div className="p-6 border-t border-slate-200 dark:border-slate-700 flex justify-end gap-3">
          <Button variant="ghost" onClick={handleClose} disabled={uploading}>
            取消
          </Button>
          <Button onClick={handleUpload} disabled={!file || uploading}>
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

  return (
    <div className="p-6 lg:p-8">
      {/* Header */}
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">文档管理</h1>
          <p className="text-slate-600 dark:text-slate-400 mt-1">管理您的知识库文档</p>
        </div>
        <Button onClick={() => setUploadModalOpen(true)} className="gap-2">
          <Upload className="h-4 w-4" />
          上传文档
        </Button>
      </div>

      {/* Search */}
      <div className="mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400" />
          <Input
            type="search"
            placeholder="搜索文档..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10"
          />
        </div>
      </div>

      {/* Documents Table */}
      <Card variant="bordered">
        <CardContent className="p-0">
          {loading ? (
            <div className="p-8 text-center text-slate-500">加载文档中...</div>
          ) : filteredDocuments.length === 0 ? (
            <div className="p-8 text-center">
              <FileText className="h-12 w-12 mx-auto text-slate-400 mb-4" />
              <p className="text-slate-600 dark:text-slate-400 mb-2">未找到文档</p>
              <p className="text-sm text-slate-500 mb-4">
                {searchTerm ? '尝试其他搜索词' : '上传您的第一个文档开始使用'}
              </p>
              {!searchTerm && (
                <Button onClick={() => setUploadModalOpen(true)} className="gap-2">
                  <Upload className="h-4 w-4" />
                  上传文档
                </Button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-slate-50 dark:bg-slate-900 border-b border-slate-200 dark:border-slate-700">
                  <tr>
                    <th className="text-left py-3 px-6 font-semibold text-sm text-slate-700 dark:text-slate-300">
                      名称
                    </th>
                    <th className="text-left py-3 px-6 font-semibold text-sm text-slate-700 dark:text-slate-300">
                      大小
                    </th>
                    <th className="text-left py-3 px-6 font-semibold text-sm text-slate-700 dark:text-slate-300">
                      上传日期
                    </th>
                    <th className="text-left py-3 px-6 font-semibold text-sm text-slate-700 dark:text-slate-300">
                      状态
                    </th>
                    <th className="text-left py-3 px-6 font-semibold text-sm text-slate-700 dark:text-slate-300">
                      文档块
                    </th>
                    <th className="text-right py-3 px-6 font-semibold text-sm text-slate-700 dark:text-slate-300">
                      操作
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-700">
                  {filteredDocuments.map((doc) => (
                    <tr
                      key={doc.id}
                      className="hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer"
                      onClick={() => navigate(`/knowledge/documents/${doc.id}`)}
                    >
                      <td className="py-4 px-6">
                        <div className="flex items-center gap-3">
                          <FileText className="h-5 w-5 text-slate-400 flex-shrink-0" />
                          <div>
                            <p className="font-medium text-slate-900 dark:text-slate-100 truncate max-w-xs">
                              {doc.title || doc.fileName}
                            </p>
                            <p className="text-xs text-slate-500">{doc.fileType}</p>
                          </div>
                        </div>
                      </td>
                      <td className="py-4 px-6 text-sm text-slate-600 dark:text-slate-400">
                        {formatFileSize(doc.fileSize)}
                      </td>
                      <td className="py-4 px-6 text-sm text-slate-600 dark:text-slate-400">
                        {formatDate(doc.createdAt)}
                      </td>
                      <td className="py-4 px-6">{getStatusBadge(doc.status)}</td>
                      <td className="py-4 px-6 text-sm text-slate-600 dark:text-slate-400">
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
                              className="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
                              title="重新索引"
                            >
                              <RefreshCw className="h-4 w-4" />
                            </button>
                          )}
                          <button
                            onClick={() => handleDelete(doc.id)}
                            className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
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
                      ? 'bg-primary-600 text-white'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700'
                  }`}
                >
                  {pageNum + 1}
                </button>
              )
            })}
          </div>
          <Button
            variant="ghost"
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
