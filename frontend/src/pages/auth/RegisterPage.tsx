import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Bot, ArrowRight, CheckCircle } from 'lucide-react'
import Input from '../../components/ui/Input'
import Button from '../../components/ui/Button'
import { authService } from '../../services/auth.service'
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

const RegisterPage: React.FC = () => {
  const navigate = useNavigate()
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
        const { accessToken, refreshToken, userId, username } = response.data

        // Store tokens - 统一使用 'token' 键
        localStorage.setItem('token', accessToken)
        localStorage.setItem('refreshToken', refreshToken)
        localStorage.setItem('userId', userId.toString())
        localStorage.setItem('username', username)

        // Store user info
        const userInfo = {
          id: userId,
          username,
          email: data.email,
          fullName: data.fullName,
          role: 'USER',
          isActive: true,
        }
        localStorage.setItem('user', JSON.stringify(userInfo))

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
    <div className="flex min-h-screen items-center justify-center bg-slate-50 dark:bg-slate-950 px-4 py-8">
      <div className="w-full max-w-md">
        {/* Logo & Title */}
        <div className="text-center mb-8">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-primary-600 text-white shadow-lg">
            <Bot className="h-8 w-8" />
          </div>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-100">
            创建账号
          </h1>
          <p className="mt-2 text-slate-600 dark:text-slate-400">
            加入 CodeReview AI，开启智能编程之旅
          </p>
        </div>

        {/* Register Form */}
        <div className="rounded-xl bg-white dark:bg-slate-900 p-8 shadow-md">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            {error && (
              <div className="rounded-lg bg-error-50 p-3 text-sm text-error-700 dark:bg-error-900 dark:text-error-300">
                {error}
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
              <Input
                label="用户名 *"
                placeholder="选择用户名"
                error={errors.username?.message}
                {...register('username')}
              />
              <Input
                label="真实姓名"
                placeholder="你的名字"
                {...register('fullName')}
              />
            </div>

            <Input
              label="邮箱 *"
              type="email"
              placeholder="your@email.com"
              error={errors.email?.message}
              {...register('email')}
            />

            <Input
              label="密码 *"
              type="password"
              placeholder="至少6个字符"
              error={errors.password?.message}
              {...register('password')}
            />

            <Input
              label="确认密码 *"
              type="password"
              placeholder="再次输入密码"
              error={errors.confirmPassword?.message}
              {...register('confirmPassword')}
            />

            {/* Features */}
            <div className="space-y-2 pt-2">
              {[
                '8个AI智能体协作分析',
                '个性化学习路径推荐',
                '丰富的代码题库练习',
                '成就系统和排行榜',
              ].map((feature) => (
                <div key={feature} className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
                  <CheckCircle className="h-4 w-4 text-success-500 flex-shrink-0" />
                  <span>{feature}</span>
                </div>
              ))}
            </div>

            <Button
              type="submit"
              className="w-full"
              loading={loading}
            >
              创建账号
              <ArrowRight className="h-4 w-4" />
            </Button>
          </form>

          <div className="mt-6 text-center text-sm text-slate-600 dark:text-slate-400">
            已有账号？{' '}
            <Link to="/login" className="font-medium text-primary-600 hover:text-primary-700">
              立即登录
            </Link>
          </div>
        </div>

        {/* Footer */}
        <p className="mt-8 text-center text-xs text-slate-500">
          注册即表示您同意我们的{' '}
          <Link to="/terms" className="text-primary-600 hover:underline">
            服务条款
          </Link>{' '}
          和{' '}
          <Link to="/privacy" className="text-primary-600 hover:underline">
            隐私政策
          </Link>
        </p>
      </div>
    </div>
  )
}

export default RegisterPage
