import React, { useState, useRef, useEffect } from 'react'
import {
  Send,
  User,
  Bot,
  FileText,
  Trash2,
  AlertCircle,
  Loader2,
} from 'lucide-react'
import Card, { CardContent } from '../../components/ui/Card'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Input from '../../components/ui/Input'
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

  return (
    <div className="p-3 bg-slate-50 dark:bg-slate-900 rounded-lg border border-slate-200 dark:border-slate-700">
      <div className="flex items-center gap-2 mb-2">
        <FileText className="h-4 w-4 text-primary-600" />
        <span className="text-sm font-medium text-slate-900 dark:text-slate-100 truncate">
          {displayName}
        </span>
        <Badge size="sm" variant="info">
          {source.relevance ? `${(source.relevance * 100).toFixed(0)}%` : source.score ? `${(source.score * 100).toFixed(0)}%` : 'N/A'}
        </Badge>
      </div>
      {displayContent && (
        <p className="text-xs text-slate-600 dark:text-slate-400 line-clamp-2">
          {displayContent}
        </p>
      )}
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
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`}>
      <div
        className={`flex gap-3 max-w-[80%] ${isUser ? 'flex-row-reverse' : 'flex-row'}`}
      >
        <div
          className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${
            isUser ? 'bg-primary-600 text-white' : 'bg-slate-200 dark:bg-slate-700'
          }`}
        >
          {isUser ? <User className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
        </div>
        <div
          className={`rounded-2xl px-4 py-2 ${
            isUser
              ? 'bg-primary-600 text-white'
              : 'bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700'
          }`}
        >
          {message.sources && message.sources.length > 0 && (
            <div className="mb-2">
              <p className="text-xs opacity-75 mb-1">来源:</p>
              <div className="grid gap-2">
                {message.sources.slice(0, 3).map((source, index) => (
                  <SourceCard key={index} source={source} />
                ))}
              </div>
            </div>
          )}
          <p className="text-sm whitespace-pre-wrap">{message.content}</p>
          {message.isStreaming && (
            <span className="inline-block w-2 h-4 bg-current animate-pulse ml-1" />
          )}
          <p className="text-xs mt-2 opacity-60">
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
    '什么是这个项目的主要功能？',
    '如何使用 Spring Boot 进行微服务开发？',
    '文档中提到了哪些设计模式？',
    'Elasticsearch 的配置方式是什么？',
  ]

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!question.trim() || isLoading) return

    // 添加用户消息
    const userMessage: ChatMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: question,
      timestamp: new Date().toISOString(),
    }
    setMessages((prev) => [...prev, userMessage])
    const questionText = question
    setQuestion('')
    setIsLoading(true)
    setConnectionError(null)

    try {
      // 调用真实的问答 API
      const response = await qaService.askQuestion(questionText)

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

      // 添加错误消息到对话
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

  const clearConversation = () => {
    setMessages([])
    setConnectionError(null)
  }

  const handleSuggestedQuestion = (q: string) => {
    setQuestion(q)
  }

  return (
    <div className="p-6 lg:p-8 h-full flex flex-col">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">
          智能问答
        </h1>
        <p className="text-slate-600 dark:text-slate-400 mt-1">
          上传文档后，可以向 AI 提问相关问题
        </p>
      </div>

      {/* Connection Status */}
      <div className="mb-4 flex items-center gap-2">
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
          <div className="w-2 h-2 rounded-full bg-green-500" />
          <span>已连接</span>
        </div>
        {messages.length > 1 && (
          <Button
            variant="ghost"
            size="sm"
            onClick={clearConversation}
            className="gap-2"
          >
            <Trash2 className="h-4 w-4" />
            清空对话
          </Button>
        )}
      </div>

      {/* Connection Error */}
      {connectionError && (
        <div className="mb-4 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg flex items-center gap-3">
          <AlertCircle className="h-5 w-5 text-red-600" />
          <div className="flex-1">
            <p className="text-sm font-medium text-red-800 dark:text-red-400">
              连接错误
            </p>
            <p className="text-xs text-red-600 dark:text-red-500">{connectionError}</p>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setConnectionError(null)}
          >
            ×
          </Button>
        </div>
      )}

      {/* Chat Area */}
      <Card variant="bordered" className="flex-1 flex flex-col overflow-hidden">
        <CardContent className="flex-1 p-4 overflow-y-auto">
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center">
              <div className="rounded-full bg-primary-100 p-4 mb-4">
                <Bot className="h-12 w-12 text-primary-600" />
              </div>
              <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100 mb-2">
                开始对话
              </h3>
              <p className="text-sm text-slate-600 dark:text-slate-400 max-w-md">
                上传文档后，可以向我提问相关问题。我会使用 AI 搜索和分析您的文档来提供准确的答案。
              </p>
              <div className="mt-6 grid gap-2 text-left max-w-md">
                {suggestedQuestions.map((suggestion, index) => (
                  <button
                    key={index}
                    type="button"
                    onClick={() => handleSuggestedQuestion(suggestion)}
                    className="w-full text-left px-3 py-2 text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              {messages.map((message) => (
                <ChatBubble key={message.id} message={message} />
              ))}
              {isLoading && (
                <div className="flex justify-start">
                  <ChatBubble
                    message={{
                      id: 'loading',
                      role: 'assistant',
                      content: '正在思考中...',
                      timestamp: new Date().toISOString(),
                      isStreaming: true,
                    }}
                  />
                </div>
              )}
            </div>
          )}
        </CardContent>

        {/* Input Area */}
        <div className="border-t border-slate-200 dark:border-slate-700 p-4">
          <form onSubmit={handleSubmit} className="flex gap-3">
            <Input
              type="text"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="输入您的问题..."
              disabled={isLoading}
              className="flex-1"
            />
            <Button
              type="submit"
              disabled={isLoading || !question.trim()}
              className="gap-2"
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
      </Card>
    </div>
  )
}

export default QAPage
