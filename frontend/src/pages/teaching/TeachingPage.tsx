import React, { useState, useEffect } from 'react'
import {
  FileText,
  BookOpen,
  Brain,
  Target,
  Plus,
  Search,
  Trash2,
  Eye,
  Calendar,
  Tag,
  TrendingUp
} from 'lucide-react'
import Card, { CardHeader, CardTitle, CardDescription, CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import { teachingService } from '../../services/teaching.service'
import type { TeachingDocument, TeachingDocumentType, TeachingStats } from '../../types'

const TeachingPage: React.FC = () => {
  const [documents, setDocuments] = useState<TeachingDocument[]>([])
  const [stats, setStats] = useState<TeachingStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [selectedDocument, setSelectedDocument] = useState<TeachingDocument | null>(null)
  const [showGenerateModal, setShowGenerateModal] = useState(false)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const pageSize = 10

  useEffect(() => {
    loadDocuments()
    loadStats()
  }, [currentPage])

  const loadDocuments = async () => {
    setLoading(true)
    try {
      const res = await teachingService.getUserDocuments(currentPage, pageSize)
      if (res.code === 200 && res.data) {
        setDocuments(res.data.data || [])
        setTotalPages(res.data.totalPages || 0)
      }
    } catch (error) {
      console.error('Failed to load teaching documents:', error)
    } finally {
      setLoading(false)
    }
  }

  const loadStats = async () => {
    try {
      const res = await teachingService.getStats()
      if (res.code === 200 && res.data) {
        setStats(res.data)
      }
    } catch (error) {
      console.error('Failed to load stats:', error)
    }
  }

  const handleSearch = async () => {
    if (!searchKeyword.trim()) {
      loadDocuments()
      return
    }

    setLoading(true)
    try {
      const res = await teachingService.searchDocuments(searchKeyword)
      if (res.code === 200 && res.data) {
        setDocuments(res.data || [])
        setTotalPages(0)
      }
    } catch (error) {
      console.error('Failed to search documents:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (documentId: number) => {
    if (!confirm('确定要删除这个教学文档吗？')) return

    try {
      const res = await teachingService.deleteDocument(documentId)
      if (res.code === 200) {
        loadDocuments()
        loadStats()
      }
    } catch (error) {
      console.error('Failed to delete document:', error)
      alert('删除失败')
    }
  }

  const getDocumentTypeIcon = (type: TeachingDocumentType) => {
    switch (type) {
      case 'LESSON':
        return <BookOpen className="h-5 w-5" />
      case 'PRACTICE':
        return <Target className="h-5 w-5" />
      case 'REVIEW':
        return <Calendar className="h-5 w-5" />
      case 'KNOWLEDGE_GAP':
        return <Brain className="h-5 w-5" />
      default:
        return <FileText className="h-5 w-5" />
    }
  }

  const getDocumentTypeName = (type: TeachingDocumentType) => {
    switch (type) {
      case 'LESSON':
        return '课时'
      case 'PRACTICE':
        return '练习'
      case 'REVIEW':
        return '复习'
      case 'KNOWLEDGE_GAP':
        return '知识缺口'
      default:
        return '自定义'
    }
  }

  const getStatusBadgeVariant = (status: string): 'default' | 'success' | 'warning' | 'error' => {
    switch (status) {
      case 'PUBLISHED':
        return 'success'
      case 'DRAFT':
        return 'warning'
      case 'ARCHIVED':
        return 'default'
      default:
        return 'default'
    }
  }

  const getStatusName = (status: string) => {
    switch (status) {
      case 'PUBLISHED':
        return '已发布'
      case 'DRAFT':
        return '草稿'
      case 'ARCHIVED':
        return '已归档'
      default:
        return status
    }
  }

  const getPriorityBadgeVariant = (priority: number): 'default' | 'success' | 'warning' | 'error' => {
    switch (priority) {
      case 1:
        return 'error'
      case 2:
        return 'default'
      case 3:
        return 'success'
      default:
        return 'default'
    }
  }

  return (
    <div className="p-6 lg:p-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-8 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
            教学文档
          </h1>
          <p className="mt-1 text-slate-600 dark:text-slate-400">
            根据测试结果和知识点生成的个性化学习材料
          </p>
        </div>
        <Button onClick={() => setShowGenerateModal(true)}>
          <Plus className="h-4 w-4 mr-2" />
          生成文档
        </Button>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <Card variant="bordered">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-blue-50 rounded-lg">
                  <FileText className="h-5 w-5 text-blue-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-slate-900">{stats.total}</p>
                  <p className="text-xs text-slate-500">总计文档</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card variant="bordered">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-green-50 rounded-lg">
                  <TrendingUp className="h-5 w-5 text-green-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-slate-900">{stats.published}</p>
                  <p className="text-xs text-slate-500">已发布</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card variant="bordered">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-yellow-50 rounded-lg">
                  <Tag className="h-5 w-5 text-yellow-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-slate-900">{stats.draft}</p>
                  <p className="text-xs text-slate-500">草稿</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card variant="bordered">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-purple-50 rounded-lg">
                  <BookOpen className="h-5 w-5 text-purple-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-slate-900">
                    {Object.values(stats.byType || {}).reduce((a, b) => a + b, 0)}
                  </p>
                  <p className="text-xs text-slate-500">类型分布</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Search Bar */}
      <Card variant="bordered" className="mb-6">
        <CardContent className="p-4">
          <div className="flex gap-3">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <input
                type="text"
                placeholder="搜索教学文档..."
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                className="w-full pl-10 pr-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
            <Button onClick={handleSearch}>搜索</Button>
            {searchKeyword && (
              <Button variant="outline" onClick={() => { setSearchKeyword(''); loadDocuments() }}>
                清除
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Documents List */}
      <Card variant="bordered">
        <CardHeader>
          <CardTitle>文档列表</CardTitle>
          <CardDescription>
            共 {documents.length} 个文档
            {totalPages > 1 && ` · 第 ${currentPage + 1}/${totalPages} 页`}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="py-12 text-center text-slate-500">加载中...</div>
          ) : documents.length === 0 ? (
            <div className="py-12 text-center text-slate-500">
              <FileText className="h-16 w-16 mx-auto mb-4 text-slate-400" />
              <p>还没有教学文档</p>
              <p className="text-sm mt-2">点击"生成文档"按钮创建你的第一个教学文档</p>
            </div>
          ) : (
            <>
              <div className="space-y-3">
                {documents.map((doc) => (
                  <div
                    key={doc.id}
                    className="p-4 border border-slate-200 rounded-lg hover:shadow-md transition-shadow cursor-pointer"
                    onClick={() => setSelectedDocument(doc)}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex items-start gap-3 flex-1">
                        <div className="p-2 bg-primary-50 text-primary-600 rounded-lg">
                          {getDocumentTypeIcon(doc.documentType)}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <h3 className="font-semibold text-slate-900 dark:text-slate-100 truncate">
                              {doc.title}
                            </h3>
                            <Badge size="sm" variant={getStatusBadgeVariant(doc.status)}>
                              {getStatusName(doc.status)}
                            </Badge>
                            <Badge size="sm" variant={getPriorityBadgeVariant(doc.priority)}>
                              优先级{doc.priority}
                            </Badge>
                          </div>
                          <div className="flex items-center gap-3 text-xs text-slate-500">
                            <span className="flex items-center gap-1">
                              <BookOpen className="h-3 w-3" />
                              {getDocumentTypeName(doc.documentType)}
                            </span>
                            <span>·</span>
                            <span className="flex items-center gap-1">
                              <Calendar className="h-3 w-3" />
                              {new Date(doc.createdAt).toLocaleDateString()}
                            </span>
                            {doc.tags && (
                              <>
                                <span>·</span>
                                <span className="flex items-center gap-1">
                                  <Tag className="h-3 w-3" />
                                  {doc.tags}
                                </span>
                              </>
                            )}
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={(e) => {
                            e.stopPropagation()
                            setSelectedDocument(doc)
                          }}
                        >
                          <Eye className="h-4 w-4" />
                          查看
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={(e) => {
                            e.stopPropagation()
                            handleDelete(doc.id)
                          }}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 mt-6 pt-6 border-t border-slate-200">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                    disabled={currentPage === 0}
                  >
                    上一页
                  </Button>
                  <span className="text-sm text-slate-600">
                    {currentPage + 1} / {totalPages}
                  </span>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={currentPage >= totalPages - 1}
                  >
                    下一页
                  </Button>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      {/* Document Detail Modal */}
      {selectedDocument && (
        <div
          className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50"
          onClick={() => setSelectedDocument(null)}
        >
          <div
            className="bg-white dark:bg-slate-800 rounded-lg shadow-xl max-w-4xl w-full max-h-[80vh] overflow-hidden flex flex-col"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6 border-b border-slate-200 dark:border-slate-700 flex items-start justify-between">
              <div>
                <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">
                  {selectedDocument.title}
                </h2>
                <div className="flex items-center gap-2 mt-2">
                  <Badge variant={getStatusBadgeVariant(selectedDocument.status)}>
                    {getStatusName(selectedDocument.status)}
                  </Badge>
                  <span className="text-sm text-slate-500">
                    {new Date(selectedDocument.createdAt).toLocaleString()}
                  </span>
                </div>
              </div>
              <button
                onClick={() => setSelectedDocument(null)}
                className="text-slate-400 hover:text-slate-600"
              >
                ✕
              </button>
            </div>
            <div className="p-6 overflow-y-auto flex-1">
              <div className="prose prose-sm max-w-none dark:prose-invert">
                <MarkdownRenderer content={selectedDocument.content} />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Generate Document Modal - Placeholder */}
      {showGenerateModal && (
        <div
          className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50"
          onClick={() => setShowGenerateModal(false)}
        >
          <div
            className="bg-white dark:bg-slate-800 rounded-lg shadow-xl max-w-2xl w-full"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="p-6 border-b border-slate-200 dark:border-slate-700 flex items-start justify-between">
              <div>
                <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100">
                  生成教学文档
                </h2>
                <p className="text-sm text-slate-500 mt-1">
                  根据测试结果和知识点生成个性化学习材料
                </p>
              </div>
              <button
                onClick={() => setShowGenerateModal(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                ✕
              </button>
            </div>
            <div className="p-6">
              <p className="text-slate-600 text-center py-8">
                文档生成功能需要配合测试模块使用<br />
                请先完成测试后再生成教学文档
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// Simple markdown renderer component
const MarkdownRenderer: React.FC<{ content: string }> = ({ content }) => {
  const renderMarkdown = (text: string) => {
    // Simple markdown to HTML conversion
    return text
      .replace(/^# (.*$)/gim, '<h1 class="text-2xl font-bold mb-4 mt-6">$1</h1>')
      .replace(/^## (.*$)/gim, '<h2 class="text-xl font-bold mb-3 mt-5">$1</h2>')
      .replace(/^### (.*$)/gim, '<h3 class="text-lg font-bold mb-2 mt-4">$1</h3>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`(.*?)`/g, '<code class="bg-slate-100 px-1 rounded">$1</code>')
      .replace(/^\- (.*$)/gim, '<li class="ml-4">$1</li>')
      .replace(/\n\n/g, '</p><p>')
      .replace(/^\> (.*$)/gim, '<blockquote class="border-l-4 border-slate-300 pl-4 italic text-slate-600">$1</blockquote>')
  }

  return (
    <div
      dangerouslySetInnerHTML={{
        __html: renderMarkdown(content).split('<p></p>').map((para, i) => (
          <p key={i} className={para.startsWith('<') ? '' : 'mb-4 mt-4'}>{para}</p>
        ))
      }}
    />
  )
}

export default TeachingPage
