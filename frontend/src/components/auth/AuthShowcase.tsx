import React from 'react'
import {
  Bot,
  BrainCircuit,
  Code2,
  Database,
  FileText,
  GitBranch,
  Layers3,
  ShieldCheck,
  Sparkles,
} from 'lucide-react'
import heroImage from '../../assets/hero.png'

const capabilities = [
  {
    icon: Code2,
    title: '项目级代码审查',
    description: '从 ZIP 项目到文件级问题清单，支持公开演示项目直接复用。',
  },
  {
    icon: BrainCircuit,
    title: '5 个智能体协作',
    description: '4 个审查 Agent 负责质量、架构、安全、性能，教学 Agent 生成学习报告。',
  },
  {
    icon: Database,
    title: '知识库与 RAG',
    description: '文档解析、向量化、Elasticsearch 检索和上下文问答形成闭环。',
  },
]

const pipeline = [
  { label: 'Upload', text: '项目 / 文档输入' },
  { label: 'Queue', text: 'Kafka 异步处理' },
  { label: 'Report', text: '审查与教学报告' },
]

const stackSignals = [
  { icon: ShieldCheck, label: 'JWT 鉴权' },
  { icon: GitBranch, label: 'Actions 部署' },
  { icon: FileText, label: 'Resend 邮件' },
]

const AuthShowcase: React.FC = () => {
  return (
    <section className="relative hidden min-h-screen overflow-hidden bg-slate-950 text-white lg:flex lg:flex-col lg:justify-between">
      <div className="absolute inset-0 cv-grid opacity-80" />
      <div className="absolute inset-x-0 top-0 h-80 cv-scanline opacity-80" />
      <div className="absolute inset-x-10 top-20 h-px bg-gradient-to-r from-transparent via-emerald-300/60 to-transparent" />
      <div className="absolute bottom-0 left-0 h-96 w-full bg-[linear-gradient(160deg,transparent_8%,rgba(34,197,94,0.11)_9%,rgba(34,197,94,0.11)_10%,transparent_11%,transparent_46%,rgba(56,189,248,0.1)_47%,rgba(56,189,248,0.1)_48%,transparent_49%)]" />

      <div className="relative z-10 p-10 xl:p-14">
        <div className="cv-kicker">
          <Sparkles className="h-4 w-4 text-amber-300" />
          AI code intelligence workbench
        </div>

        <div className="mt-12 max-w-xl">
          <div className="mb-6 flex items-center gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-lg border border-emerald-300/30 bg-emerald-300/15 shadow-2xl shadow-emerald-950/60 backdrop-blur">
              <Bot className="h-8 w-8 text-emerald-300" />
            </div>
            <div className="rounded-lg border border-white/10 bg-white/[0.06] px-4 py-3 backdrop-blur">
              <p className="font-mono text-sm text-emerald-200">codeview.top</p>
              <p className="text-xs text-slate-400">Spring Cloud + React + Docker</p>
            </div>
          </div>

          <h1 className="text-5xl font-black leading-tight tracking-normal xl:text-6xl">
            CodeView
            <span className="block text-emerald-300">AI Review Lab</span>
          </h1>
          <p className="mt-5 text-lg leading-8 text-slate-200">
            面向工程项目的 AI 代码理解与审查平台：把项目上传、智能体审查、知识库问答、教学报告和部署运维串成一个可演示的全栈系统。
          </p>
        </div>

        <div className="mt-10 grid max-w-2xl gap-3">
          {capabilities.map((item) => {
            const Icon = item.icon
            return (
              <div
                key={item.title}
                className="group flex items-start gap-4 rounded-lg border border-white/12 bg-white/[0.07] p-4 backdrop-blur transition hover:border-emerald-300/40 hover:bg-white/[0.1]"
              >
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-emerald-300/15 text-emerald-300 transition group-hover:bg-emerald-300 group-hover:text-slate-950">
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <p className="font-semibold text-white">{item.title}</p>
                  <p className="mt-1 text-sm leading-6 text-slate-300">{item.description}</p>
                </div>
              </div>
            )
          })}
        </div>
      </div>

      <div className="relative z-10 mx-10 mb-10 rounded-lg border border-white/12 bg-white/[0.08] p-5 shadow-2xl shadow-black/30 backdrop-blur xl:mx-14">
        <div className="flex items-center justify-between gap-5">
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 text-sm uppercase tracking-normal text-slate-400">
              <Layers3 className="h-4 w-4 text-sky-300" />
              runtime pipeline
            </div>
            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              {pipeline.map((step, index) => (
                <div key={step.label} className="rounded-lg border border-white/10 bg-black/20 p-3">
                  <div className="flex items-center gap-2">
                    <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-emerald-300 text-xs font-bold text-slate-950">
                      {index + 1}
                    </span>
                    <span className="text-sm font-semibold">{step.label}</span>
                  </div>
                  <p className="mt-2 text-xs text-slate-300">{step.text}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="hidden shrink-0 items-center justify-center rounded-lg border border-white/10 bg-white/5 p-6 xl:flex">
            <img src={heroImage} alt="CodeView platform layers" className="h-36 w-36 object-contain" />
          </div>
        </div>

        <div className="mt-5 grid grid-cols-3 gap-3 border-t border-white/10 pt-4 text-sm text-slate-300">
          {stackSignals.map((item) => {
            const Icon = item.icon
            return (
              <div key={item.label} className="flex items-center gap-2">
                <Icon className="h-4 w-4 text-emerald-300" />
                {item.label}
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}

export default AuthShowcase
