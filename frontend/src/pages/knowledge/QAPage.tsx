import React, { useState, useRef, useEffect } from 'react'
import {
  AlertCircle,
  Bot,
  Database,
  FileText,
  Layers,
  Loader2,
  MessageSquareText,
  SearchCheck,
  Send,
  Trash2,
  User,
} from 'lucide-react'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Input from '../../components/ui/Input'
import WorkbenchHero from '../../components/ui/WorkbenchHero'
import { qaService } from '../../services/qa.service'

interface SourceReference {
  documentId: number
  documentName?: string
  title?: string
  fileName?: string
  chunkId?: number
  content?: string
  score: number
  relevance?: number
}

interface SourceCardProps {
  source: SourceReference
}

const SourceCard: React.FC<SourceCardProps> = ({ source }) => {
  const displayName = source.documentName || source.title || source.fileName || '未知文档'
  const displayContent = source.content || '暂无内容预览'
  const score = source.relevance ?? source.score

  return (
    <div className="rounded-lg border border-white/10 bg-slate-950/55 p-3 shadow-inner shadow-slate-950/40">
      <div className="mb-2 flex min-w-0 items-center gap-2">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-emerald-300/20 bg-emerald-300/10 text-emerald-300">
          <FileText className="h-4 w-4" />
        </div>
        <span className="min-w-0 flex-1 truncate text-sm font-medium text-slate-100" title={displayName}>
          {displayName}
        </span>
        <Badge
          size="sm"
          variant="info"
          className="border border-sky-300/25 bg-sky-300/10 text-sky-200"
        >
          {Number.isFinite(score) ? `${(score * 100).toFixed(0)}%` : 'N/A'}
        </Badge>
      </div>
      <p className="line-clamp-2 text-xs leading-5 text-slate-400">
        {displayContent}
      </p>
    </div>
  )
}

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  sources?: SourceReference[]
  isStreaming?: boolean
}

interface ChatBubbleProps {
  message: ChatMessage
}

const ChatBubble: React.FC<ChatBubbleProps> = ({ message }) => {
  const isUser = message.role === 'user'

  return (
    <div className={`mb-5 flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`flex max-w-[88%] gap-3 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
        <div
          className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border ${
            isUser
              ? 'border-emerald-300/40 bg-emerald-300 text-slate-950'
              : 'border-sky-300/25 bg-sky-300/10 text-sky-300'
          }`}
        >
          {isUser ? <User className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
        </div>

        <div
          className={`min-w-0 rounded-lg border px-4 py-3 shadow-[0_18px_50px_rgba(2,6,23,0.24)] ${
            isUser
              ? 'border-emerald-300/35 bg-emerald-300 text-slate-950'
              : 'border-white/10 bg-white/[0.065] text-slate-100 backdrop-blur'
          }`}
        >
          {message.sources && message.sources.length > 0 && (
            <div className="mb-3">
              <div className={`mb-2 flex items-center gap-2 text-xs font-semibold ${isUser ? 'text-slate-800' : 'text-slate-400'}`}>
                <SearchCheck className="h-3.5 w-3.5" />
                检索来源
              </div>
              <div className="grid gap-2">
                {message.sources.slice(0, 3).map((source, index) => (
                  <SourceCard key={`${source.documentId}-${source.chunkId || index}`} source={source} />
                ))}
              </div>
            </div>
          )}

          <p className="whitespace-pre-wrap text-sm leading-7">{message.content}</p>
          {message.isStreaming && (
            <span className="ml-1 inline-block h-4 w-2 animate-pulse bg-current align-middle" />
          )}
          <p className={`mt-2 text-xs ${isUser ? 'text-slate-700' : 'text-slate-500'}`}>
            {new Date(message.timestamp).toLocaleTimeString('zh-CN', {
              hour: '2-digit',
              minute: '2-digit',
            })}
          </p>
        </div>
      </div>
    </div>
  )
}

const QAPage: React.FC = () => {
  const [question, setQuestion] = useState('')
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [connectionError, setConnectionError] = useState<string | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const suggestedQuestions = [
    'CodeView 的 RAG 问答链路是怎么工作的？',
    '知识库文档上传后经历了哪些处理步骤？',
    '这个项目里 Elasticsearch 和向量检索分别承担什么角色？',
    '面试时如何介绍项目的 AI Agent 协作设计？',
  ]

  const assistantMessages = messages.filter((message) => message.role === 'assistant').length
  const sourceCount = messages.reduce((sum, message) => sum + (message.sources?.length || 0), 0)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const askQuestion = async (questionText: string) => {
    const trimmedQuestion = questionText.trim()
    if (!trimmedQuestion || isLoading) return

    const userMessage: ChatMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: trimmedQuestion,
      timestamp: new Date().toISOString(),
    }
    setMessages((prev) => [...prev, userMessage])
    setQuestion('')
    setIsLoading(true)
    setConnectionError(null)

    try {
      const response = await qaService.askQuestion(trimmedQuestion)

      if (response.code === 200 && response.data) {
        const assistantMessage: ChatMessage = {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          content: response.data.answer || '抱歉，没有找到相关信息。',
          timestamp: new Date().toISOString(),
          sources: response.data.sources?.map((source: any) => ({
            documentId: source.documentId || 0,
            documentName: source.fileName || source.title || '未知文档',
            title: source.title,
            fileName: source.fileName,
            score: source.score || 0,
            relevance: source.score,
            content: source.content,
          })) || [],
          isStreaming: false,
        }
        setMessages((prev) => [...prev, assistantMessage])
      } else {
        throw new Error(response.message || '获取答案失败')
      }
    } catch (error: any) {
      console.error('Failed to ask question:', error)
      const errorMessage = error.response?.data?.message || error.message || '获取答案失败，请稍后重试'
      setConnectionError(errorMessage)

      const errorMessageBubble: ChatMessage = {
        id: `error-${Date.now()}`,
        role: 'assistant',
        content: `抱歉，${errorMessage}`,
        timestamp: new Date().toISOString(),
        isStreaming: false,
      }
      setMessages((prev) => [...prev, errorMessageBubble])
    } finally {
      setIsLoading(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    await askQuestion(question)
  }

  const clearConversation = () => {
    setMessages([])
    setConnectionError(null)
  }

  const handleSuggestedQuestion = (suggestion: string) => {
    setQuestion(suggestion)
  }

  return (
    <div className="mx-auto flex h-full max-w-[1500px] flex-col p-4 text-slate-100 sm:p-6 lg:p-8">
      <WorkbenchHero
        kicker="rag answer console"
        title="智能问答"
        description="基于已索引文档进行检索增强问答。这里展示从问题理解、知识库召回、来源引用到答案生成的完整 RAG 体验，适合面试时直接演示项目的 AI 应用闭环。"
        metrics={[
          { icon: MessageSquareText, label: '对话轮次', value: messages.length, tone: 'text-emerald-300' },
          { icon: Bot, label: 'AI 回答', value: assistantMessages, tone: 'text-sky-300' },
          { icon: FileText, label: '引用来源', value: sourceCount, tone: 'text-violet-300' },
          { icon: Database, label: '知识上下文', value: 'Indexed', tone: 'text-amber-300' },
          { icon: Layers, label: '生成模式', value: 'RAG', tone: 'text-emerald-300' },
        ]}
        action={
          messages.length > 0 ? (
            <Button
              variant="outline"
              onClick={clearConversation}
              className="border-white/15 text-slate-100 hover:bg-white/10"
            >
              <Trash2 className="h-4 w-4" />
              清空对话
            </Button>
          ) : undefined
        }
      />

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="inline-flex items-center gap-2 rounded-lg border border-emerald-300/20 bg-emerald-300/10 px-3 py-1.5 text-sm text-emerald-200">
          <span className="h-2 w-2 rounded-full bg-emerald-300 shadow-[0_0_16px_rgba(110,231,183,0.9)]" />
          已连接知识库问答服务
        </div>
        <div className="inline-flex items-center gap-2 rounded-lg border border-white/10 bg-white/[0.05] px-3 py-1.5 text-sm text-slate-400">
          <SearchCheck className="h-4 w-4 text-sky-300" />
          回答会附带可追溯来源
        </div>
      </div>

      {connectionError && (
        <div className="mb-4 flex items-center gap-3 rounded-lg border border-rose-300/25 bg-rose-300/10 p-4 text-rose-100">
          <AlertCircle className="h-5 w-5 text-rose-300" />
          <div className="min-w-0 flex-1">
            <p className="text-sm font-semibold">连接错误</p>
            <p className="truncate text-xs text-rose-200/80">{connectionError}</p>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setConnectionError(null)}
            className="text-rose-100 hover:bg-rose-300/10 hover:text-white"
            aria-label="关闭错误提示"
          >
            ×
          </Button>
        </div>
      )}

      <section className="flex min-h-[560px] flex-1 flex-col overflow-hidden rounded-lg border border-white/10 bg-white/[0.06] shadow-[0_30px_100px_rgba(2,6,23,0.28)] backdrop-blur">
        <div className="flex items-center justify-between border-b border-white/10 bg-slate-950/55 px-5 py-4">
          <div className="min-w-0">
            <h2 className="text-base font-semibold text-white">知识库对话</h2>
            <p className="mt-1 text-sm text-slate-500">围绕已上传文档提问，答案优先使用检索到的项目上下文。</p>
          </div>
          <div className="hidden items-center gap-2 rounded-lg border border-white/10 bg-white/[0.05] px-3 py-2 text-xs text-slate-400 sm:flex">
            <Database className="h-4 w-4 text-emerald-300" />
            RAG Pipeline
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-4 sm:p-6">
          {messages.length === 0 ? (
            <div className="flex h-full min-h-[390px] flex-col items-center justify-center text-center">
              <div className="mb-5 flex h-20 w-20 items-center justify-center rounded-lg border border-sky-300/25 bg-sky-300/10 p-5 text-sky-300 shadow-[0_20px_70px_rgba(14,165,233,0.18)]">
                <Bot className="h-12 w-12" />
              </div>
              <h3 className="mb-2 text-xl font-semibold text-white">开始一次可追溯问答</h3>
              <p className="max-w-xl text-sm leading-6 text-slate-400">
                适合向面试官演示：问题进入后先从知识库召回相关片段，再交给大模型生成答案，并把引用来源展示出来。
              </p>
              <div className="mt-7 grid w-full max-w-2xl gap-3 text-left sm:grid-cols-2">
                {suggestedQuestions.map((suggestion) => (
                  <button
                    key={suggestion}
                    type="button"
                    onClick={() => handleSuggestedQuestion(suggestion)}
                    className="rounded-lg border border-white/10 bg-slate-950/45 p-4 text-left text-sm leading-6 text-slate-300 transition hover:border-emerald-300/35 hover:bg-white/[0.08] hover:text-white"
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className="space-y-1">
              {messages.map((message) => (
                <ChatBubble key={message.id} message={message} />
              ))}
              {isLoading && (
                <ChatBubble
                  message={{
                    id: 'loading',
                    role: 'assistant',
                    content: '正在检索知识库并组织答案...',
                    timestamp: new Date().toISOString(),
                    isStreaming: true,
                  }}
                />
              )}
              <div ref={messagesEndRef} />
            </div>
          )}
        </div>

        <div className="border-t border-white/10 bg-slate-950/55 p-4">
          <form onSubmit={handleSubmit} className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto]">
            <Input
              type="text"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="输入一个和知识库相关的问题..."
              disabled={isLoading}
              className="h-12 border-white/10 bg-slate-950/70 text-slate-100 placeholder:text-slate-600 focus-visible:border-emerald-300/60 focus-visible:ring-emerald-300/30"
            />
            <Button
              type="submit"
              disabled={isLoading || !question.trim()}
              className="h-12 bg-emerald-300 px-5 text-slate-950 hover:bg-emerald-200"
            >
              {isLoading ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Send className="h-4 w-4" />
              )}
              发送
            </Button>
          </form>
        </div>
      </section>
    </div>
  )
}

export default QAPage
