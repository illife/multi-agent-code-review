import React from 'react'
import {
  Bot,
  BrainCircuit,
  Code2,
  Database,
  GitBranch,
  GraduationCap,
  ShieldCheck,
  Sparkles,
} from 'lucide-react'
import heroImage from '../../assets/hero.png'

const capabilities = [
  {
    icon: Code2,
    title: '代码审查',
    description: '从项目、文件和审查报告串起完整质量反馈。',
  },
  {
    icon: Database,
    title: '知识库检索',
    description: '文档上传后完成解析、向量化和 Elasticsearch 索引。',
  },
  {
    icon: GraduationCap,
    title: '学习闭环',
    description: '把审查问题沉淀成可追踪的学习路径与练习。',
  },
]

const pipeline = [
  { label: 'Upload', text: '文档与项目输入' },
  { label: 'Index', text: '向量检索与索引' },
  { label: 'Review', text: 'AI 审查与问答' },
]

const AuthShowcase: React.FC = () => {
  return (
    <section className="relative hidden min-h-screen overflow-hidden bg-[#0f1720] text-white lg:flex lg:flex-col lg:justify-between">
      <div className="absolute inset-x-0 top-0 h-48 bg-[linear-gradient(110deg,rgba(94,234,212,0.22),rgba(250,204,21,0.08)_44%,transparent_70%)]" />
      <div className="absolute bottom-0 left-0 h-64 w-full bg-[linear-gradient(160deg,transparent_15%,rgba(255,255,255,0.08)_16%,rgba(255,255,255,0.08)_17%,transparent_18%,transparent_42%,rgba(94,234,212,0.14)_43%,rgba(94,234,212,0.14)_44%,transparent_45%)]" />
      <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.035)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.035)_1px,transparent_1px)] bg-[size:36px_36px]" />

      <div className="relative z-10 p-10 xl:p-14">
        <div className="inline-flex items-center gap-2 rounded-lg border border-white/15 bg-white/10 px-3 py-1 text-sm text-white/80 backdrop-blur">
          <Sparkles className="h-4 w-4 text-[#facc15]" />
          面试演示版 · 多模块 AI 工程平台
        </div>

        <div className="mt-12 max-w-xl">
          <div className="mb-6 flex h-16 w-16 items-center justify-center rounded-lg border border-white/15 bg-white/10 shadow-2xl shadow-black/30 backdrop-blur">
            <Bot className="h-8 w-8 text-[#5eead4]" />
          </div>
          <h1 className="text-5xl font-black leading-tight tracking-normal">
            CodeView AI
            <span className="block text-[#5eead4]">Review Lab</span>
          </h1>
          <p className="mt-5 text-lg leading-8 text-slate-200">
            一个把代码审查、知识库问答、学习路径和项目管理串在一起的全栈 AI 系统，适合现场展示架构、工程化和业务闭环。
          </p>
        </div>

        <div className="mt-10 grid max-w-2xl gap-3">
          {capabilities.map((item) => {
            const Icon = item.icon
            return (
              <div
                key={item.title}
                className="flex items-start gap-4 rounded-lg border border-white/12 bg-white/[0.07] p-4 backdrop-blur"
              >
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-[#5eead4]/15 text-[#5eead4]">
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
          <div>
            <p className="text-sm uppercase text-slate-400">runtime pipeline</p>
            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              {pipeline.map((step, index) => (
                <div key={step.label} className="rounded-lg bg-black/20 p-3">
                  <div className="flex items-center gap-2">
                    <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-[#facc15] text-xs font-bold text-slate-950">
                      {index + 1}
                    </span>
                    <span className="text-sm font-semibold">{step.label}</span>
                  </div>
                  <p className="mt-2 text-xs text-slate-300">{step.text}</p>
                </div>
              ))}
            </div>
          </div>
          <div className="hidden shrink-0 items-center justify-center rounded-lg bg-white/5 p-6 xl:flex">
            <img src={heroImage} alt="CodeView platform layers" className="h-36 w-36 object-contain" />
          </div>
        </div>
        <div className="mt-5 grid grid-cols-3 gap-3 border-t border-white/10 pt-4 text-sm text-slate-300">
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-4 w-4 text-[#5eead4]" />
            JWT 鉴权
          </div>
          <div className="flex items-center gap-2">
            <GitBranch className="h-4 w-4 text-[#facc15]" />
            CI/CD 部署
          </div>
          <div className="flex items-center gap-2">
            <BrainCircuit className="h-4 w-4 text-[#93c5fd]" />
            Qwen 推理
          </div>
        </div>
      </div>
    </section>
  )
}

export default AuthShowcase
