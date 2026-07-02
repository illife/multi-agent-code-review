import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { ArrowRight, LockKeyhole, ShieldCheck, UserRound } from 'lucide-react'
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
        const { accessToken, refreshToken, userId, username, email, emailVerified } = response.data

        localStorage.setItem('token', accessToken)
        localStorage.setItem('refreshToken', refreshToken)

        const user = {
          id: userId?.toString() || '',
          username: username || '',
          email: email || '',
          role: 'USER',
          emailVerified: emailVerified || false,
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
    <main className="grid min-h-screen overflow-hidden bg-slate-950 text-slate-950 lg:grid-cols-[1.08fr_0.92fr]">
      <AuthShowcase />

      <section className="relative flex min-h-screen items-center justify-center px-5 py-10 sm:px-8">
        <div className="absolute inset-0 cv-grid opacity-35 lg:hidden" />
        <div className="absolute inset-x-0 top-0 h-72 cv-scanline opacity-50 lg:hidden" />

        <div className="relative z-10 w-full max-w-[470px]">
          <div className="mb-8 text-white">
            <div className="mb-5 inline-flex items-center gap-2 rounded-lg border border-emerald-300/25 bg-emerald-300/10 px-3 py-1 text-sm text-emerald-100 shadow-sm">
              <LockKeyhole className="h-4 w-4 text-emerald-300" />
              安全访问工作台
            </div>
            <h1 className="text-4xl font-black tracking-normal text-white">欢迎回来</h1>
            <p className="mt-3 text-base leading-7 text-slate-300">
              继续查看项目级审查、知识库问答、教学报告和部署状态。面试演示时可以直接进入已准备好的公开项目。
            </p>
          </div>

          <div className="rounded-lg border border-white/10 bg-white p-6 shadow-[0_30px_100px_rgba(2,6,23,0.34)] sm:p-8">
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

              <div className="flex items-center justify-between gap-3 text-sm">
                <label className="flex items-center gap-2 text-slate-600">
                  <input type="checkbox" className="rounded border-slate-300 text-emerald-600 focus:ring-emerald-500" />
                  记住这台设备
                </label>
                <Link to="/forgot-password" className="font-semibold text-emerald-700 hover:text-emerald-800">
                  忘记密码？
                </Link>
              </div>

              <Button type="submit" className="h-12 w-full bg-slate-950 text-white hover:bg-slate-800" loading={loading}>
                进入 CodeView 工作台
                <ArrowRight className="h-4 w-4" />
              </Button>
            </form>

            <div className="mt-6 rounded-lg border border-slate-200 bg-slate-50 p-4">
              <div className="flex items-start gap-3">
                <div className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-emerald-600 text-white">
                  <UserRound className="h-4 w-4" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-slate-900">演示账号与新账号都可进入</p>
                  <p className="mt-1 text-sm leading-6 text-slate-600">
                    公开项目可以被所有用户查看和审查，删除权限只保留给项目创建者。
                  </p>
                </div>
              </div>
            </div>

            <div className="mt-6 flex items-center justify-center gap-2 text-center text-sm text-slate-600">
              <span>还没有账号？</span>
              <Link to="/register" className="font-semibold text-emerald-700 hover:text-emerald-800">
                创建新账号
              </Link>
            </div>
          </div>

          <p className="mt-6 flex items-center justify-center gap-2 text-center text-xs text-slate-400">
            <ShieldCheck className="h-3.5 w-3.5 text-emerald-300" />
            HTTPS 部署在 codeview.top，AI Token 访问已加限流保护
          </p>
        </div>
      </section>
    </main>
  )
}

export default LoginPage
