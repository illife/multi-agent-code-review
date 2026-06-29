import React, { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { AlertCircle, ArrowLeft, CheckCircle2, MailCheck } from 'lucide-react'
import AuthShowcase from '../../components/auth/AuthShowcase'
import { authService } from '../../services/auth.service'

type VerifyStatus = 'loading' | 'success' | 'error'

const VerifyEmailPage: React.FC = () => {
  const [searchParams] = useSearchParams()
  const token = useMemo(() => searchParams.get('token') || '', [searchParams])
  const [status, setStatus] = useState<VerifyStatus>('loading')
  const [message, setMessage] = useState('正在确认邮箱...')

  useEffect(() => {
    const verify = async () => {
      if (!token) {
        setStatus('error')
        setMessage('验证链接缺少 token，请重新发送验证邮件。')
        return
      }

      try {
        await authService.verifyEmail(token)
        setStatus('success')
        setMessage('邮箱已经验证成功。后续账号安全通知会发送到这个邮箱。')
      } catch (err: any) {
        setStatus('error')
        setMessage(err.response?.data?.message || '邮箱验证失败，链接可能已经过期。')
      }
    }

    verify()
  }, [token])

  const isSuccess = status === 'success'
  const isLoading = status === 'loading'

  return (
    <main className="grid min-h-screen bg-[#f6f3ec] text-slate-950 lg:grid-cols-[1.08fr_0.92fr]">
      <AuthShowcase />

      <section className="flex min-h-screen items-center justify-center px-5 py-10 sm:px-8">
        <div className="w-full max-w-[460px]">
          <Link to="/login" className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-teal-700">
            <ArrowLeft className="h-4 w-4" />
            返回登录
          </Link>

          <div className="rounded-lg border border-slate-200 bg-white p-7 shadow-[0_24px_80px_rgba(15,23,32,0.12)] sm:p-8">
            <div className={`flex h-14 w-14 items-center justify-center rounded-lg ${
              isLoading ? 'bg-slate-100 text-slate-600' : isSuccess ? 'bg-teal-700 text-white' : 'bg-red-50 text-red-700'
            }`}>
              {isLoading ? <MailCheck className="h-7 w-7 animate-pulse" /> : isSuccess ? <CheckCircle2 className="h-7 w-7" /> : <AlertCircle className="h-7 w-7" />}
            </div>

            <h1 className="mt-6 text-3xl font-black tracking-normal text-slate-950">
              {isLoading ? '正在验证邮箱' : isSuccess ? '邮箱已确认' : '验证未完成'}
            </h1>
            <p className="mt-3 text-base leading-7 text-slate-600">{message}</p>

            <div className="mt-6 flex flex-col gap-3 sm:flex-row">
              <Link to="/login" className="inline-flex h-10 items-center justify-center rounded-lg bg-slate-950 px-4 font-medium text-white hover:bg-slate-800">
                去登录
              </Link>
              {!isSuccess && (
                <Link to="/dashboard" className="inline-flex h-10 items-center justify-center rounded-lg border-2 border-slate-300 px-4 font-medium text-slate-700 hover:bg-slate-100">
                  回到工作台
                </Link>
              )}
            </div>
          </div>
        </div>
      </section>
    </main>
  )
}

export default VerifyEmailPage
