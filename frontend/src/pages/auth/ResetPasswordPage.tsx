import React, { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowLeft, CheckCircle2, KeyRound } from 'lucide-react'
import * as z from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Input from '../../components/ui/Input'
import Button from '../../components/ui/Button'
import AuthShowcase from '../../components/auth/AuthShowcase'
import { authService } from '../../services/auth.service'

const resetPasswordSchema = z.object({
  password: z.string().min(6, '密码至少6个字符'),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: '两次输入的密码不一致',
  path: ['confirmPassword'],
})

type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>

const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams()
  const token = useMemo(() => searchParams.get('token') || '', [searchParams])
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordFormData>({
    resolver: zodResolver(resetPasswordSchema),
  })

  const onSubmit = async ({ password }: ResetPasswordFormData) => {
    if (!token) {
      setError('重置链接缺少 token，请重新发起找回密码')
      return
    }

    setLoading(true)
    setError('')

    try {
      await authService.resetPassword(token, password)
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      setSuccess(true)
    } catch (err: any) {
      setError(err.response?.data?.message || '密码重置失败，链接可能已过期')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="grid min-h-screen bg-[#f6f3ec] text-slate-950 lg:grid-cols-[1.08fr_0.92fr]">
      <AuthShowcase />

      <section className="flex min-h-screen items-center justify-center px-5 py-10 sm:px-8">
        <div className="w-full max-w-[460px]">
          <Link to="/login" className="mb-6 inline-flex items-center gap-2 text-sm font-semibold text-slate-600 hover:text-teal-700">
            <ArrowLeft className="h-4 w-4" />
            返回登录
          </Link>

          <div className="mb-8">
            <div className="mb-5 inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white/70 px-3 py-1 text-sm text-slate-600 shadow-sm">
              <KeyRound className="h-4 w-4 text-teal-700" />
              设置新的登录凭据
            </div>
            <h1 className="text-4xl font-black tracking-normal text-slate-950">设置新密码</h1>
            <p className="mt-3 text-base leading-7 text-slate-600">
              重置完成后，请使用新密码重新登录 CodeView。
            </p>
          </div>

          <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-[0_24px_80px_rgba(15,23,32,0.12)] sm:p-8">
            {success ? (
              <div className="space-y-5">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-teal-700 text-white">
                  <CheckCircle2 className="h-6 w-6" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-slate-950">密码已更新</h2>
                  <p className="mt-2 text-sm leading-6 text-slate-600">
                    你的密码已经重置成功，系统也会发送一封安全提醒邮件。
                  </p>
                </div>
                <Link to="/login" className="inline-flex h-10 items-center justify-center rounded-lg bg-slate-950 px-4 font-medium text-white hover:bg-slate-800">
                  去登录
                </Link>
              </div>
            ) : (
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
                {!token && (
                  <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
                    当前链接缺少 token，请从重置密码邮件中重新打开。
                  </div>
                )}
                {error && (
                  <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                    {error}
                  </div>
                )}

                <Input
                  label="新密码"
                  type="password"
                  placeholder="至少6个字符"
                  error={errors.password?.message}
                  className="h-12 border-slate-300 bg-slate-50"
                  {...register('password')}
                />

                <Input
                  label="确认新密码"
                  type="password"
                  placeholder="再次输入新密码"
                  error={errors.confirmPassword?.message}
                  className="h-12 border-slate-300 bg-slate-50"
                  {...register('confirmPassword')}
                />

                <Button type="submit" className="h-12 w-full bg-slate-950 text-white hover:bg-slate-800" loading={loading} disabled={!token}>
                  确认重置密码
                  <KeyRound className="h-4 w-4" />
                </Button>
              </form>
            )}
          </div>
        </div>
      </section>
    </main>
  )
}

export default ResetPasswordPage
