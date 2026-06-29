import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { ArrowRight, CheckCircle, Fingerprint } from 'lucide-react'
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
  '代码审查、知识库、问答和学习路径协同',
  '文档上传后自动解析、切块、向量化并建立索引',
  '支持项目级分析报告，沉淀工程质量与知识闭环',
  '独立工作区管理账号、文档、审查记录和项目结果',
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
    <main className="grid min-h-screen bg-[#f6f3ec] text-slate-950 lg:grid-cols-[1.08fr_0.92fr]">
      <AuthShowcase />

      <section className="flex min-h-screen items-center justify-center px-5 py-10 sm:px-8">
        <div className="w-full max-w-[500px]">
          <div className="mb-7">
            <div className="mb-5 inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white/70 px-3 py-1 text-sm text-slate-600 shadow-sm">
              <Fingerprint className="h-4 w-4 text-teal-700" />
              创建新的工作身份
            </div>
            <h1 className="text-4xl font-black tracking-normal text-slate-950">创建账号</h1>
            <p className="mt-3 text-base leading-7 text-slate-600">
              当前服务器是全新环境，注册后即可上传文档、体验问答、查看项目分析与代码审查。
            </p>
          </div>

          <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-[0_24px_80px_rgba(15,23,32,0.12)] sm:p-8">
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

              <div className="grid gap-2 rounded-lg bg-[#f6f3ec] p-4">
                {onboardingHighlights.map((feature) => (
                  <div key={feature} className="flex items-center gap-2 text-sm text-slate-700">
                    <CheckCircle className="h-4 w-4 shrink-0 text-teal-700" />
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
              <Link to="/login" className="font-semibold text-teal-700 hover:text-teal-800">
                立即登录
              </Link>
            </div>
          </div>

          <p className="mt-6 text-center text-xs text-slate-500">
            注册后即可进入工作台，账号、文档和项目结果会保存在你的工作区。
          </p>
        </div>
      </section>
    </main>
  )
}

export default RegisterPage
