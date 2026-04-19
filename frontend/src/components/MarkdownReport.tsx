import React from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'
import { oneDark, oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism'

interface MarkdownReportProps {
  markdown: string
  darkMode?: boolean
}

const MarkdownReport: React.FC<MarkdownReportProps> = ({ markdown, darkMode = true }) => {
  return (
    <div className="prose prose-slate dark:prose-invert max-w-none">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          code: ({ className, children, ...props }: any) => {
            const match = /language-(\w+)/.exec(className || '')
            const language = match ? match[1] : ''
            const codeString = String(children).replace(/\n$/, '')

            if (match) {
              return (
                <SyntaxHighlighter
                  style={darkMode ? oneDark : oneLight}
                  language={language}
                  PreTag="div"
                  className="rounded-lg text-sm my-3"
                  showLineNumbers
                >
                  {codeString}
                </SyntaxHighlighter>
              )
            }

            return (
              <code className={className} {...props}>
                {children}
              </code>
            )
          },
        }}
      >
        {markdown}
      </ReactMarkdown>
    </div>
  )
}

export default MarkdownReport
