import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { ArrowRight, LockKeyhole, UserRound } from 'lucide-react'
import Input from '../../components/ui/Input'
import Button from '../../components/ui/Button'
import AuthShowcase from '../../components/auth/AuthShowcase'
import { authService } from '../../services/auth.service'
import { setToken, setUser } from '../../store/slices/authSlice'
import type { LoginRequest } from '../../types'

const loginSchema = z.object({
  username: z.string().min(1, '请输入用户名'),
  password: z.string().min(6, '密码至少6个字符'),
})

type LoginFormData = z.infer<typeof loginSchema>

const LoginPage: React.FC = () => {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const [error, setError] = useState<string>('')
  const [loading, setLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  })

  const onSubmit = async (data: LoginRequest) => {
    setError('')
    setLoading(true)

    try {
      const response = await authService.login(data)

      if (response.code === 200 && response.data) {
        const { accessToken, refreshToken, userId, username } = response.data

        localStorage.setItem('token', accessToken)
        localStorage.setItem('refreshToken', refreshToken)

        const user = {
          id: userId?.toString() || '',
          username: username || '',
          email: '',
          role: 'USER',
        }
        localStorage.setItem('user', JSON.stringify(user))
        localStorage.setItem('userId', user.id)
        localStorage.setItem('username', user.username)

        dispatch(setToken(accessToken))
        dispatch(setUser(user))

        setTimeout(() => {
          navigate('/dashboard')
        }, 100)
      } else {
        setError(response.message || '登录失败，请重试')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '登录失败，请检查用户名和密码')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="grid min-h-screen bg-[#f6f3ec] text-slate-950 lg:grid-cols-[1.08fr_0.92fr]">
      <AuthShowcase />

      <section className="flex min-h-screen items-center justify-center px-5 py-10 sm:px-8">
        <div className="w-full max-w-[460px]">
          <div className="mb-8">
            <div className="mb-5 inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white/70 px-3 py-1 text-sm text-slate-600 shadow-sm">
              <LockKeyhole className="h-4 w-4 text-teal-700" />
              安全访问工作台
            </div>
            <h1 className="text-4xl font-black tracking-normal text-slate-950">欢迎回来</h1>
            <p className="mt-3 text-base leading-7 text-slate-600">
              登录后继续查看代码审查、知识库索引、学习进度和项目分析结果。
            </p>
          </div>

          <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-[0_24px_80px_rgba(15,23,32,0.12)] sm:p-8">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
              {error && (
                <div className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                  {error}
                </div>
              )}

              <Input
                label="用户名"
                placeholder="请输入用户名"
                error={errors.username?.message}
                className="h-12 border-slate-300 bg-slate-50"
                {...register('username')}
              />

              <Input
                label="密码"
                type="password"
                placeholder="请输入密码"
                error={errors.password?.message}
                className="h-12 border-slate-300 bg-slate-50"
                {...register('password')}
              />

              <div className="flex items-center justify-between text-sm">
                <label className="flex items-center gap-2 text-slate-600">
                  <input type="checkbox" className="rounded border-slate-300 text-teal-700 focus:ring-teal-600" />
                  记住这台设备
                </label>
                <span className="text-slate-400">Token 自动续期</span>
              </div>

              <Button type="submit" className="h-12 w-full bg-slate-950 text-white hover:bg-slate-800" loading={loading}>
                进入工作台
                <ArrowRight className="h-4 w-4" />
              </Button>
            </form>

            <div className="mt-6 rounded-lg bg-[#f6f3ec] p-4">
              <div className="flex items-start gap-3">
                <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-teal-700 text-white">
                  <UserRound className="h-4 w-4" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-slate-900">没有账号也可以直接开始</p>
                  <p className="mt-1 text-sm leading-6 text-slate-600">
                    创建账号后即可上传文档、提交代码审查，并在独立工作区中管理分析结果。
                  </p>
                </div>
              </div>
            </div>

            <div className="mt-6 text-center text-sm text-slate-600">
              还没有账号？{' '}
              <Link to="/register" className="font-semibold text-teal-700 hover:text-teal-800">
                创建新账号
              </Link>
            </div>
          </div>

          <p className="mt-6 text-center text-xs text-slate-500">
            CodeView AI Review Lab · HTTPS 部署在 codeview.top
          </p>
        </div>
      </section>
    </main>
  )
}

export default LoginPage
