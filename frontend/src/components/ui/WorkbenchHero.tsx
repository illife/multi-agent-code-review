import React from 'react'
import { Sparkles } from 'lucide-react'

interface WorkbenchMetric {
  icon: React.ComponentType<{ className?: string }>
  label: string
  value: React.ReactNode
  tone?: string
}

interface WorkbenchHeroProps {
  kicker: string
  title: string
  description: string
  metrics: WorkbenchMetric[]
  action?: React.ReactNode
}

const WorkbenchHero: React.FC<WorkbenchHeroProps> = ({
  kicker,
  title,
  description,
  metrics,
  action,
}) => {
  return (
    <section className="relative mb-6 overflow-hidden rounded-lg border border-white/10 bg-white/[0.06] p-6 shadow-[0_30px_100px_rgba(2,6,23,0.3)] backdrop-blur-xl lg:p-8">
      <div className="absolute inset-0 cv-grid opacity-30" />
      <div className="absolute inset-x-0 top-0 h-56 cv-scanline opacity-45" />

      <div className="relative flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div className="max-w-3xl">
          <div className="cv-kicker">
            <Sparkles className="h-4 w-4 text-amber-300" />
            {kicker}
          </div>
          <h1 className="mt-5 text-4xl font-black tracking-normal text-white">{title}</h1>
          <p className="mt-3 text-base leading-7 text-slate-300">{description}</p>
        </div>
        {action && <div className="shrink-0">{action}</div>}
      </div>

      <div className="relative mt-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
        {metrics.map((item) => {
          const Icon = item.icon
          return (
            <div key={item.label} className="rounded-lg border border-white/10 bg-slate-950/35 p-4">
              <div className="flex items-center gap-3">
                <Icon className={`h-5 w-5 ${item.tone || 'text-emerald-300'}`} />
                <span className="text-sm text-slate-400">{item.label}</span>
              </div>
              <p className="mt-2 min-w-0 truncate font-mono text-lg font-bold text-white">{item.value}</p>
            </div>
          )
        })}
      </div>
    </section>
  )
}

export default WorkbenchHero
