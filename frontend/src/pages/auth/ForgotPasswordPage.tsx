import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, MailCheck, Send } from 'lucide-react'
import * as z from 'zod'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Input from '../../components/ui/Input'
import Button from '../../components/ui/Button'
import AuthShowcase from '../../components/auth/AuthShowcase'
import { authService } from '../../services/auth.service'

const forgotPasswordSchema = z.object({
  email: z.string().email('请输入有效的邮箱地址'),
})

type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>

const ForgotPasswordPage: React.FC = () => {
  const [loading, setLoading] = useState(false)
  const [submittedEmail, setSubmittedEmail] = useState('')
  const [error, setError] = useState('')

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
  })

  const onSubmit = async ({ email }: ForgotPasswordFormData) => {
    setLoading(true)
    setError('')

    try {
      await authService.forgotPassword(email)
      setSubmittedEmail(email)
    } catch (err: any) {
      setError(err.response?.data?.message || '发送重置邮件失败，请稍后再试')
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
              <MailCheck className="h-4 w-4 text-teal-700" />
              邮件找回账号访问权
            </div>
            <h1 className="text-4xl font-black tracking-normal text-slate-950">重置密码</h1>
            <p className="mt-3 text-base leading-7 text-slate-600">
              输入注册邮箱后，系统会发送一封带有安全链接的邮件。链接有效期为 24 小时。
            </p>
          </div>

          <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-[0_24px_80px_rgba(15,23,32,0.12)] sm:p-8">
            {submittedEmail ? (
              <div className="space-y-5">
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-teal-700 text-white">
                  <MailCheck className="h-6 w-6" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-slate-950">邮件已发送</h2>
                  <p className="mt-2 text-sm leading-6 text-slate-600">
                    请查看 {submittedEmail} 的收件箱。如果没有收到，也可以检查垃圾邮件或稍后重新发送。
                  </p>
                </div>
                <div className="flex flex-col gap-3 sm:flex-row">
                  <Button type="button" onClick={() => setSubmittedEmail('')} className="bg-slate-950 text-white hover:bg-slate-800">
                    重新发送
                  </Button>
                  <Link to="/login" className="inline-flex h-10 items-center justify-center rounded-lg border-2 border-slate-300 px-4 font-medium text-slate-700 hover:bg-slate-100">
                    回到登录
                  </Link>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
                {error && (
                  <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                    {error}
                  </div>
                )}

                <Input
                  label="注册邮箱"
                  type="email"
                  placeholder="your@email.com"
                  error={errors.email?.message}
                  className="h-12 border-slate-300 bg-slate-50"
                  {...register('email')}
                />

                <Button type="submit" className="h-12 w-full bg-slate-950 text-white hover:bg-slate-800" loading={loading}>
                  发送重置邮件
                  <Send className="h-4 w-4" />
                </Button>
              </form>
            )}
          </div>
        </div>
      </section>
    </main>
  )
}

export default ForgotPasswordPage
