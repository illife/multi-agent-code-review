import React, { useState } from 'react'
import {
  BrainCircuit,
  CheckCircle,
  Search,
  Filter,
  X,
  FileText,
  Download,
  Hash,
  Layers,
  SlidersHorizontal,
} from 'lucide-react'
import Card, { CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Input from '../../components/ui/Input'
import WorkbenchHero from '../../components/ui/WorkbenchHero'
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

  const handleSearch = async (page: number = 0, overrideQuery?: string) => {
    const searchQuery = overrideQuery ?? query
    if (!searchQuery.trim()) return

    try {
      setLoading(true)
      setSearched(true)

      const request: SearchRequest = {
        query: searchQuery,
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

  const runExampleSearch = (example: string) => {
    setQuery(example)
    setCurrentPage(0)
    handleSearch(0, example)
  }

  const highlightMatch = (text: string, query: string) => {
    if (!query) return text

    const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
    const parts = text.split(regex)

    return parts.map((part, index) =>
      regex.test(part) ? (
        <mark key={`match-${index}`} className="rounded bg-amber-300/25 px-0.5 text-amber-100">
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

  const averageScore = results.length
    ? Math.round(results.reduce((sum, result) => sum + result.score, 0) / results.length * 100)
    : 0

  const activeFilterCount = Object.values(filters).filter(Boolean).length

  return (
    <div className="mx-auto max-w-[1500px] p-4 text-slate-100 sm:p-6 lg:p-8">
      <WorkbenchHero
        kicker="hybrid retrieval"
        title="文档搜索"
        description="面向知识库的检索控制台。通过关键词、文档类型和时间条件快速定位文档块，适合展示 Elasticsearch 检索、向量召回与 RAG 问答前置链路。"
        metrics={[
          { icon: Search, label: '当前查询', value: query || '未输入', tone: 'text-emerald-300' },
          { icon: Layers, label: '结果数量', value: results.length, tone: 'text-sky-300' },
          { icon: BrainCircuit, label: '平均相关度', value: results.length ? `${averageScore}%` : '-', tone: 'text-violet-300' },
          { icon: SlidersHorizontal, label: '筛选条件', value: activeFilterCount, tone: 'text-amber-300' },
          { icon: CheckCircle, label: '检索模式', value: 'Hybrid', tone: 'text-emerald-300' },
        ]}
      />

      {/* Search Bar */}
      <Card variant="bordered" className="mb-6 border-white/10 bg-white/[0.06] text-slate-100 backdrop-blur">
        <CardContent className="p-4">
          <form onSubmit={handleSubmit} className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto_auto]">
            <div className="relative">
              <Search className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-500" />
              <Input
                type="search"
                value={query}
                onChange={(e) => {
                  setQuery(e.target.value)
                }}
                placeholder="搜索技术方案、接口说明、异常处理、部署配置..."
                className="h-11 border-white/10 bg-slate-950/45 pl-11 text-slate-100 placeholder:text-slate-600 focus-visible:border-emerald-300/60 focus-visible:ring-emerald-300/30"
              />
            </div>
            <Button
              type="button"
              variant="outline"
              onClick={() => setShowFilters(!showFilters)}
              className="gap-2 border-white/15 text-slate-100 hover:bg-white/10"
            >
              <Filter className="h-4 w-4" />
              筛选
            </Button>
            <Button type="submit" className="bg-emerald-300 text-slate-950 hover:bg-emerald-200" disabled={loading || !query.trim()}>
              {loading ? (
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-slate-950 border-t-transparent" />
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
            <div className="mt-4 border-t border-white/10 pt-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-300">
                    文档类型
                  </label>
                  <select
                    value={filters.documentType || ''}
                    onChange={(e) =>
                      setFilters({ ...filters, documentType: e.target.value || undefined })
                    }
                    className="w-full rounded-lg border border-white/10 bg-slate-950/45 px-3 py-2 text-sm text-slate-100 focus:border-emerald-300/60 focus:ring-2 focus:ring-emerald-300/30"
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
                  <label className="mb-1 block text-sm font-medium text-slate-300">
                    开始日期
                  </label>
                  <Input
                    type="date"
                    value={filters.dateFrom || ''}
                    onChange={(e) =>
                      setFilters({ ...filters, dateFrom: e.target.value || undefined })
                    }
                    className="border-white/10 bg-slate-950/45 text-slate-100 focus-visible:border-emerald-300/60 focus-visible:ring-emerald-300/30"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-300">
                    结束日期
                  </label>
                  <Input
                    type="date"
                    value={filters.dateTo || ''}
                    onChange={(e) =>
                      setFilters({ ...filters, dateTo: e.target.value || undefined })
                    }
                    className="border-white/10 bg-slate-950/45 text-slate-100 focus-visible:border-emerald-300/60 focus-visible:ring-emerald-300/30"
                  />
                </div>
              </div>
              <div className="mt-4 flex justify-end">
                <Button variant="ghost" onClick={handleClearFilters} className="text-sm text-slate-300 hover:bg-white/10 hover:text-white">
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
              <p className="text-sm text-slate-400">
                找到 {results.length} 个结果 "{query}"
              </p>
              <Button variant="outline" onClick={exportResults} className="gap-2 border-white/15 text-slate-100 hover:bg-white/10">
                <Download className="h-4 w-4" />
                导出
              </Button>
            </div>
          )}

          <div className="space-y-4">
            {results.length === 0 && !loading ? (
              <Card variant="bordered" className="border-white/10 bg-white/[0.06] text-slate-100 backdrop-blur">
                <CardContent className="p-8 text-center">
                  <Search className="mx-auto mb-4 h-12 w-12 text-slate-500" />
                  <p className="mb-2 font-semibold text-white">未找到结果</p>
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
                  className="cursor-pointer border-white/10 bg-white/[0.06] text-slate-100 backdrop-blur transition hover:border-emerald-300/30 hover:bg-white/[0.085]"
                >
                  <CardContent className="p-4">
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <div className="rounded-lg bg-emerald-300/10 p-2 text-emerald-300">
                          <FileText className="h-4 w-4" />
                        </div>
                        <span className="font-medium text-white">
                          {result.fileName}
                        </span>
                      </div>
                      <Badge
                        size="sm"
                        variant="info"
                        className="border border-sky-300/25 bg-sky-300/10 text-sky-200"
                      >
                        {(result.score * 100).toFixed(0)}% 相关度
                      </Badge>
                    </div>
                    <div className="mb-3 flex items-center gap-4 text-xs text-slate-500">
                      <span className="flex items-center gap-1">
                        <Hash className="h-3 w-3" />
                        文档块 #{result.chunkIndex + 1}
                      </span>
                    </div>
                    <p className="line-clamp-3 text-sm leading-6 text-slate-300">
                      {result.highlight ? highlightMatch(result.highlight, query) : result.content}
                    </p>
                  </CardContent>
                </Card>
              ))
            )}

            {loading && (
              <div className="text-center py-8">
                <div className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-emerald-300 border-t-transparent" />
                <p className="mt-2 text-slate-400">搜索中...</p>
              </div>
            )}

            {currentPage + 1 < totalPages && !loading && results.length > 0 && (
              <div className="text-center">
                <Button
                  variant="outline"
                  className="border-white/15 text-slate-100 hover:bg-white/10"
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
        <Card variant="bordered" className="border-white/10 bg-white/[0.06] text-slate-100 backdrop-blur">
          <CardContent className="p-12 text-center">
            <div className="mx-auto mb-4 w-fit rounded-lg border border-emerald-300/25 bg-emerald-300/10 p-4 text-emerald-300">
              <Search className="h-12 w-12" />
            </div>
            <h3 className="mb-2 text-lg font-semibold text-white">
              搜索您的知识库
            </h3>
            <p className="mx-auto mb-6 max-w-md text-sm leading-6 text-slate-400">
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
                  onClick={() => runExampleSearch(example)}
                  className="rounded-lg border border-white/10 bg-slate-950/35 p-3 text-left transition-colors hover:border-emerald-300/35 hover:bg-white/[0.07]"
                >
                  <p className="text-sm text-slate-300">{example}</p>
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
