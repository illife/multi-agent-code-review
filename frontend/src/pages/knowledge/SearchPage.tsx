import React, { useState } from 'react'
import {
  Search,
  Filter,
  X,
  FileText,
  Download,
  Hash,
} from 'lucide-react'
import Card, { CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Input from '../../components/ui/Input'
import { knowledgeService } from '../../services/knowledge.service'
import type { SearchHitDto, SearchRequest } from '../../types'

interface SearchFilters {
  documentType?: string
  dateFrom?: string
  dateTo?: string
}

const SearchPage: React.FC = () => {
  const [query, setQuery] = useState('')
  const [filters, setFilters] = useState<SearchFilters>({})
  const [showFilters, setShowFilters] = useState(false)
  const [results, setResults] = useState<SearchHitDto[]>([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const handleSearch = async (page: number = 0) => {
    if (!query.trim()) return

    try {
      setLoading(true)
      setSearched(true)

      const request: SearchRequest = {
        query,
        page,
        size: 10,
        documentType: filters.documentType,
        dateFrom: filters.dateFrom,
        dateTo: filters.dateTo,
      }

      const response = await knowledgeService.search(request)
      if (response.code === 200 && response.data) {
        if (page === 0) {
          setResults(response.data.results || [])
        } else {
          setResults((prev) => [...prev, ...(response.data.results || [])])
        }
        setTotalPages(response.data.totalPages || 0)
      }
    } catch (error) {
      console.error('Search failed:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setCurrentPage(0)
    handleSearch(0)
  }

  const handleClearFilters = () => {
    setFilters({})
    if (query) {
      setCurrentPage(0)
      handleSearch(0)
    }
  }

  const highlightMatch = (text: string, query: string) => {
    if (!query) return text

    const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
    const parts = text.split(regex)

    return parts.map((part, index) =>
      regex.test(part) ? (
        <mark key={`match-${index}`} className="bg-yellow-200 dark:bg-yellow-800 rounded px-0.5">
          {part}
        </mark>
      ) : (
        <span key={`text-${index}`}>{part}</span>
      )
    )
  }

  const exportResults = () => {
    const data = results.map((r) => ({
      document: r.fileName,
      chunk: r.chunkIndex,
      content: r.content,
      score: r.score,
    }))

    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: 'application/json',
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `search-results-${Date.now()}.json`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
  }

  return (
    <div className="p-6 lg:p-8">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
          搜索文档
        </h1>
        <p className="text-slate-600 dark:text-slate-400 mt-1">
          在所有已索引的文档中搜索
        </p>
      </div>

      {/* Search Bar */}
      <Card variant="bordered" className="mb-6">
        <CardContent className="p-4">
          <form onSubmit={handleSubmit} className="flex gap-3">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400" />
              <Input
                type="search"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value)
                }}
                placeholder="在文档中搜索内容..."
                className="pl-10"
              />
            </div>
            <Button
              type="button"
              variant="outline"
              onClick={() => setShowFilters(!showFilters)}
              className="gap-2"
            >
              <Filter className="h-4 w-4" />
              筛选
            </Button>
            <Button type="submit" disabled={loading || !query.trim()}>
              {loading ? (
                <div className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <>
                  <Search className="h-4 w-4 mr-2" />
                  搜索
                </>
              )}
            </Button>
          </form>

          {/* Filters */}
          {showFilters && (
            <div className="mt-4 pt-4 border-t border-slate-200 dark:border-slate-700">
              <div className="grid gap-4 sm:grid-cols-3">
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                    文档类型
                  </label>
                  <select
                    value={filters.documentType || ''}
                    onChange={(e) =>
                      setFilters({ ...filters, documentType: e.target.value || undefined })
                    }
                    className="w-full px-3 py-2 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-lg text-sm"
                  >
                    <option value="">所有类型</option>
                    <option value="application/pdf">PDF</option>
                    <option value="application/msword">Word (DOC)</option>
                    <option value="application/vnd.openxmlformats-officedocument.wordprocessingml.document">
                      Word (DOCX)
                    </option>
                    <option value="text/plain">Text</option>
                    <option value="text/markdown">Markdown</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                    开始日期
                  </label>
                  <Input
                    type="date"
                    value={filters.dateFrom || ''}
                    onChange={(e) =>
                      setFilters({ ...filters, dateFrom: e.target.value || undefined })
                    }
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                    结束日期
                  </label>
                  <Input
                    type="date"
                    value={filters.dateTo || ''}
                    onChange={(e) =>
                      setFilters({ ...filters, dateTo: e.target.value || undefined })
                    }
                  />
                </div>
              </div>
              <div className="mt-4 flex justify-end">
                <Button variant="ghost" onClick={handleClearFilters} className="text-sm">
                  <X className="h-4 w-4 mr-1" />
                  清除筛选
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Results */}
      {searched && (
        <>
          {results.length > 0 && (
            <div className="mb-4 flex items-center justify-between">
              <p className="text-sm text-slate-600 dark:text-slate-400">
                找到 {results.length} 个结果 "{query}"
              </p>
              <Button variant="outline" onClick={exportResults} className="gap-2">
                <Download className="h-4 w-4" />
                导出
              </Button>
            </div>
          )}

          <div className="space-y-4">
            {results.length === 0 && !loading ? (
              <Card variant="bordered">
                <CardContent className="p-8 text-center">
                  <Search className="h-12 w-12 text-slate-400 mx-auto mb-4" />
                  <p className="text-slate-600 dark:text-slate-400 mb-2">未找到结果</p>
                  <p className="text-sm text-slate-500">
                    尝试不同的关键词或调整筛选条件
                  </p>
                </CardContent>
              </Card>
            ) : (
              results.map((result) => (
                <Card
                  key={result.id}
                  variant="bordered"
                  className="hover:shadow-md transition-shadow cursor-pointer"
                >
                  <CardContent className="p-4">
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <FileText className="h-4 w-4 text-primary-600" />
                        <span className="font-medium text-slate-900 dark:text-slate-100">
                          {result.fileName}
                        </span>
                      </div>
                      <Badge size="sm" variant="info">
                        {(result.score * 100).toFixed(0)}% 相关度
                      </Badge>
                    </div>
                    <div className="flex items-center gap-4 text-xs text-slate-500 mb-3">
                      <span className="flex items-center gap-1">
                        <Hash className="h-3 w-3" />
                        文档块 #{result.chunkIndex + 1}
                      </span>
                    </div>
                    <p className="text-sm text-slate-700 dark:text-slate-300 line-clamp-3">
                      {result.highlight ? highlightMatch(result.highlight, query) : result.content}
                    </p>
                  </CardContent>
                </Card>
              ))
            )}

            {loading && (
              <div className="text-center py-8">
                <div className="h-8 w-8 border-4 border-primary-600 border-t-transparent rounded-full animate-spin mx-auto" />
                <p className="text-slate-600 dark:text-slate-400 mt-2">搜索中...</p>
              </div>
            )}

            {currentPage + 1 < totalPages && !loading && results.length > 0 && (
              <div className="text-center">
                <Button
                  variant="outline"
                  onClick={() => {
                    const nextPage = currentPage + 1
                    setCurrentPage(nextPage)
                    handleSearch(nextPage)
                  }}
                >
                  加载更多结果
                </Button>
              </div>
            )}
          </div>
        </>
      )}

      {/* Empty State */}
      {!searched && (
        <Card variant="bordered">
          <CardContent className="p-12 text-center">
            <div className="rounded-full bg-primary-100 p-4 mx-auto w-fit mb-4">
              <Search className="h-12 w-12 text-primary-600" />
            </div>
            <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100 mb-2">
              搜索您的知识库
            </h3>
            <p className="text-sm text-slate-600 dark:text-slate-400 max-w-md mx-auto mb-6">
              输入关键词、短语或问题，在所有已索引的文档中查找相关内容。
            </p>
            <div className="grid gap-3 max-w-md mx-auto">
              {[
                '机器学习算法',
                'API 认证方法',
                '数据库优化技术',
              ].map((example, index) => (
                <button
                  key={index}
                  onClick={() => {
                    setQuery(example)
                    handleSearch(0)
                  }}
                  className="text-left p-3 bg-slate-50 dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-700 hover:border-primary-300 hover:bg-primary-50 transition-colors"
                >
                  <p className="text-sm text-slate-700 dark:text-slate-300">{example}</p>
                </button>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

export default SearchPage
