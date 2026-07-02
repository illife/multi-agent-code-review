import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { ArrowRight, CheckCircle, Fingerprint, ShieldCheck } from 'lucide-react'
import Input from '../../components/ui/Input'
import Button from '../../components/ui/Button'
import AuthShowcase from '../../components/auth/AuthShowcase'
import { authService } from '../../services/auth.service'
import { setToken, setUser } from '../../store/slices/authSlice'
import type { RegisterRequest } from '../../types'

const registerSchema = z.object({
  username: z.string().min(3, '用户名至少3个字符').max(20, '用户名最多20个字符'),
  email: z.string().email('请输入有效的邮箱地址'),
  fullName: z.string().optional(),
  password: z.string().min(6, '密码至少6个字符'),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: '两次输入的密码不一致',
  path: ['confirmPassword'],
})

type RegisterFormData = z.infer<typeof registerSchema>

const onboardingHighlights = [
  '公开项目可供所有账号查看与审查，适合面试时直接演示',
  '4 个审查 Agent 覆盖代码规范、架构、安全与性能',
  '第 5 个教学 Agent 会把问题汇总成可讲解的学习报告',
  '文档知识库支持上传、切块、向量化、检索和问答闭环',
]

const RegisterPage: React.FC = () => {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const [error, setError] = useState<string>('')
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  })

  const onSubmit = async (data: RegisterRequest) => {
    setError('')
    setLoading(true)

    try {
      const response = await authService.register(data)

      if (response.code === 200 && response.data) {
        const { accessToken, refreshToken, userId, username, emailVerified } = response.data

        localStorage.setItem('token', accessToken)
        localStorage.setItem('refreshToken', refreshToken)
        localStorage.setItem('userId', userId.toString())
        localStorage.setItem('username', username)

        const userInfo = {
          id: userId,
          username,
          email: data.email,
          fullName: data.fullName,
          role: 'USER',
          isActive: true,
          emailVerified: emailVerified || false,
        }
        localStorage.setItem('user', JSON.stringify(userInfo))

        dispatch(setToken(accessToken))
        dispatch(setUser({
          id: userId.toString(),
          username,
          email: data.email,
          role: 'USER',
          emailVerified: emailVerified || false,
        }))

        navigate('/dashboard')
      } else {
        setError(response.message || '注册失败，请重试')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '注册失败，用户名或邮箱可能已被使用')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="grid min-h-screen overflow-hidden bg-slate-950 text-slate-950 lg:grid-cols-[1.08fr_0.92fr]">
      <AuthShowcase />

      <section className="relative flex min-h-screen items-center justify-center px-5 py-10 sm:px-8">
        <div className="absolute inset-0 cv-grid opacity-35 lg:hidden" />
        <div className="absolute inset-x-0 top-0 h-72 cv-scanline opacity-50 lg:hidden" />

        <div className="relative z-10 w-full max-w-[520px]">
          <div className="mb-7 text-white">
            <div className="mb-5 inline-flex items-center gap-2 rounded-lg border border-emerald-300/25 bg-emerald-300/10 px-3 py-1 text-sm text-emerald-100 shadow-sm">
              <Fingerprint className="h-4 w-4 text-emerald-300" />
              创建新的演示工作区
            </div>
            <h1 className="text-4xl font-black tracking-normal text-white">创建账号</h1>
            <p className="mt-3 text-base leading-7 text-slate-300">
              新账号可以直接进入 CodeView，查看公开项目、上传文档、体验问答，并触发项目文件级代码审查。
            </p>
          </div>

          <div className="rounded-lg border border-white/10 bg-white p-6 shadow-[0_30px_100px_rgba(2,6,23,0.34)] sm:p-8">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              {error && (
                <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                  {error}
                </div>
              )}

              <div className="grid gap-4 sm:grid-cols-2">
                <Input
                  label="用户名 *"
                  placeholder="选择用户名"
                  error={errors.username?.message}
                  className="h-12 border-slate-300 bg-slate-50"
                  {...register('username')}
                />
                <Input
                  label="真实姓名"
                  placeholder="你的名字"
                  className="h-12 border-slate-300 bg-slate-50"
                  {...register('fullName')}
                />
              </div>

              <Input
                label="邮箱 *"
                type="email"
                placeholder="your@email.com"
                error={errors.email?.message}
                className="h-12 border-slate-300 bg-slate-50"
                {...register('email')}
              />

              <div className="grid gap-4 sm:grid-cols-2">
                <Input
                  label="密码 *"
                  type="password"
                  placeholder="至少6个字符"
                  error={errors.password?.message}
                  className="h-12 border-slate-300 bg-slate-50"
                  {...register('password')}
                />

                <Input
                  label="确认密码 *"
                  type="password"
                  placeholder="再次输入密码"
                  error={errors.confirmPassword?.message}
                  className="h-12 border-slate-300 bg-slate-50"
                  {...register('confirmPassword')}
                />
              </div>

              <div className="grid gap-2 rounded-lg border border-slate-200 bg-slate-50 p-4">
                {onboardingHighlights.map((feature) => (
                  <div key={feature} className="flex items-start gap-2 text-sm text-slate-700">
                    <CheckCircle className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
                    <span>{feature}</span>
                  </div>
                ))}
              </div>

              <Button type="submit" className="h-12 w-full bg-slate-950 text-white hover:bg-slate-800" loading={loading}>
                创建账号并进入工作台
                <ArrowRight className="h-4 w-4" />
              </Button>
            </form>

            <div className="mt-6 text-center text-sm text-slate-600">
              已有账号？{' '}
              <Link to="/login" className="font-semibold text-emerald-700 hover:text-emerald-800">
                立即登录
              </Link>
            </div>
          </div>

          <p className="mt-6 flex items-center justify-center gap-2 text-center text-xs text-slate-400">
            <ShieldCheck className="h-3.5 w-3.5 text-emerald-300" />
            注册后数据保存在独立账号下，公开项目支持跨账号演示
          </p>
        </div>
      </section>
    </main>
  )
}

export default RegisterPage
