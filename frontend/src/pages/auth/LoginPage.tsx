import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Bot, ArrowRight } from 'lucide-react'
import Input from '../../components/ui/Input'
import Button from '../../components/ui/Button'
import { authService } from '../../services/auth.service'
import type { LoginRequest } from '../../types'

const loginSchema = z.object({
  username: z.string().min(1, '请输入用户名'),
  password: z.string().min(6, '密码至少6个字符'),
})

type LoginFormData = z.infer<typeof loginSchema>

const LoginPage: React.FC = () => {
  const navigate = useNavigate()
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

        // 统一使用 'token' 键存储
        localStorage.setItem('token', accessToken)
        localStorage.setItem('refreshToken', refreshToken)

        // 构建用户对象（从登录响应中获取）
        const user = {
          id: userId?.toString() || '',
          username: username || '',
          email: '',
          role: 'USER'
        }
        localStorage.setItem('user', JSON.stringify(user))

        // 等待下一帧确保状态更新后再跳转
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
    <div className="flex min-h-screen items-center justify-center bg-slate-50 dark:bg-slate-950 px-4">
      <div className="w-full max-w-md">
        {/* Logo & Title */}
        <div className="text-center mb-8">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-primary-600 text-white shadow-lg">
            <Bot className="h-8 w-8" />
          </div>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-100">
            欢迎回来
          </h1>
          <p className="mt-2 text-slate-600 dark:text-slate-400">
            登录到 CodeReview AI 平台
          </p>
        </div>

        {/* Login Form */}
        <div className="rounded-xl bg-white dark:bg-slate-900 p-8 shadow-md">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            {error && (
              <div className="rounded-lg bg-error-50 p-3 text-sm text-error-700 dark:bg-error-900 dark:text-error-300">
                {error}
              </div>
            )}

            <Input
              label="用户名"
              placeholder="请输入用户名"
              error={errors.username?.message}
              {...register('username')}
            />

            <Input
              label="密码"
              type="password"
              placeholder="请输入密码"
              error={errors.password?.message}
              {...register('password')}
            />

            <div className="flex items-center justify-between text-sm">
              <label className="flex items-center gap-2 text-slate-600 dark:text-slate-400">
                <input type="checkbox" className="rounded border-slate-300 text-primary-600 focus:ring-primary-500" />
                记住我
              </label>
              <Link to="/forgot-password" className="text-primary-600 hover:text-primary-700">
                忘记密码？
              </Link>
            </div>

            <Button
              type="submit"
              className="w-full"
              loading={loading}
            >
              登录
              <ArrowRight className="h-4 w-4" />
            </Button>
          </form>

          <div className="mt-6 text-center text-sm text-slate-600 dark:text-slate-400">
            还没有账号？{' '}
            <Link to="/register" className="font-medium text-primary-600 hover:text-primary-700">
              立即注册
            </Link>
          </div>
        </div>

        {/* Footer */}
        <p className="mt-8 text-center text-xs text-slate-500">
          © 2024 CodeReview AI. All rights reserved.
        </p>
      </div>
    </div>
  )
}

export default LoginPage
