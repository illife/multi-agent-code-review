import React, { useState, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { setUser, logout } from '../../store/slices/authSlice'
import { authService } from '../../services/auth.service'
import type { UserInfo } from '../../types'
import Card, { CardHeader, CardTitle, CardContent } from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import { User, Mail, Shield, Calendar, Save, LogOut, Key, Eye, EyeOff, BadgeCheck, Send } from 'lucide-react'

const pageCardClass = 'border border-white/10 bg-white/[0.06] text-slate-100 shadow-2xl shadow-slate-950/30 backdrop-blur-xl dark:border-white/10 dark:bg-white/[0.06]'
const sectionPanelClass = 'rounded-xl border border-white/10 bg-slate-950/35 p-4'
const labelClass = 'flex items-center gap-2 text-sm font-medium text-slate-300'
const fieldClass = 'w-full rounded-lg border border-white/10 bg-slate-950/45 px-3 py-2 text-sm text-slate-100 outline-none placeholder:text-slate-600 focus:border-emerald-300/60 focus:ring-2 focus:ring-emerald-300/20 disabled:cursor-not-allowed disabled:text-slate-400 disabled:opacity-100'
const subtleButtonClass = 'border border-white/15 bg-white/[0.04] text-slate-200 hover:border-emerald-300/35 hover:bg-emerald-300/10 hover:text-emerald-100'
const primaryButtonClass = 'bg-emerald-300 text-slate-950 hover:bg-emerald-200 focus-visible:ring-emerald-400'

const ProfilePage: React.FC = () => {
  const dispatch = useDispatch()
  const user = useSelector((state: any) => state.auth.user)

  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null)
  const [sendingVerification, setSendingVerification] = useState(false)

  // Profile edit state
  const [editing, setEditing] = useState(false)
  const [fullName, setFullName] = useState('')

  // Password change state
  const [showPasswordForm, setShowPasswordForm] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showCurrentPassword, setShowCurrentPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)

  // Fetch current user info
  useEffect(() => {
    fetchUserInfo()
  }, [])

  const fetchUserInfo = async () => {
    setLoading(true)
    try {
      const response = await authService.getCurrentUser()
      if (response.code === 200 && response.data) {
        setUserInfo(response.data)
        setFullName(response.data.fullName || '')
      }
    } catch (error) {
      console.error('Failed to fetch user info:', error)
      showMessage('error', '获取用户信息失败')
    } finally {
      setLoading(false)
    }
  }

  const showMessage = (type: 'success' | 'error', text: string) => {
    setMessage({ type, text })
    setTimeout(() => setMessage(null), 3000)
  }

  const handleSaveProfile = async () => {
    if (!fullName.trim()) {
      showMessage('error', '姓名不能为空')
      return
    }

    setSaving(true)
    try {
      const response = await authService.updateProfile(fullName.trim())
      if (response.code === 200 && response.data) {
        setUserInfo(response.data)
        // Update Redux store
        dispatch(setUser({
          ...user,
          fullName: response.data.fullName,
        }))
        showMessage('success', '个人资料更新成功')
        setEditing(false)
      } else {
        showMessage('error', response.message || '更新失败')
      }
    } catch (error: any) {
      showMessage('error', error.response?.data?.message || '更新失败')
    } finally {
      setSaving(false)
    }
  }

  const handleChangePassword = async () => {
    // Validation
    if (!currentPassword || !newPassword || !confirmPassword) {
      showMessage('error', '请填写所有密码字段')
      return
    }

    if (newPassword.length < 6) {
      showMessage('error', '新密码长度至少6位')
      return
    }

    if (newPassword !== confirmPassword) {
      showMessage('error', '两次输入的新密码不一致')
      return
    }

    setSaving(true)
    try {
      const response = await authService.changePassword(currentPassword, newPassword)
      if (response.code === 200) {
        showMessage('success', '密码修改成功，请重新登录')
        // Clear form
        setCurrentPassword('')
        setNewPassword('')
        setConfirmPassword('')
        setShowPasswordForm(false)
        // Logout after 2 seconds
        setTimeout(() => {
          dispatch(logout())
          window.location.href = '/login'
        }, 2000)
      } else {
        showMessage('error', response.message || '密码修改失败')
      }
    } catch (error: any) {
      showMessage('error', error.response?.data?.message || '密码修改失败，请检查当前密码是否正确')
    } finally {
      setSaving(false)
    }
  }

  const handleResendVerificationEmail = async () => {
    setSendingVerification(true)
    try {
      const response = await authService.resendVerificationEmail()
      if (response.code === 200) {
        showMessage('success', '验证邮件已发送，请查看邮箱')
      } else {
        showMessage('error', response.message || '发送验证邮件失败')
      }
    } catch (error: any) {
      showMessage('error', error.response?.data?.message || '发送验证邮件失败')
    } finally {
      setSendingVerification(false)
    }
  }

  const handleLogout = () => {
    if (confirm('确定要退出登录吗？')) {
      dispatch(logout())
      window.location.href = '/login'
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="text-center">
          <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-emerald-300/20 border-t-emerald-300"></div>
          <p className="mt-4 text-slate-300">加载中...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl p-4 text-slate-100 sm:p-6 lg:p-8">
      {/* Page Header */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-emerald-200/70">Account Console</p>
          <h1 className="mt-2 text-2xl font-semibold text-slate-50">个人设置</h1>
          <p className="mt-2 text-sm text-slate-400">管理个人身份、邮箱验证和账户安全。</p>
        </div>
        <Button
          variant="outline"
          onClick={handleLogout}
          className="w-full border border-red-300/30 bg-red-500/10 text-red-100 hover:border-red-300/45 hover:bg-red-500/15 sm:w-auto"
        >
          <LogOut className="h-4 w-4 mr-2" />
          退出登录
        </Button>
      </div>

      {/* Message Banner */}
      {message && (
        <div className={`mb-6 rounded-xl border p-4 text-sm shadow-lg shadow-slate-950/20 ${
          message.type === 'success'
            ? 'border-emerald-300/25 bg-emerald-300/10 text-emerald-100'
            : 'border-red-300/25 bg-red-500/10 text-red-100'
        }`}>
          {message.text}
        </div>
      )}

      {/* Profile Card */}
      <div className="space-y-6">
      <Card variant="bordered" className={pageCardClass}>
        <CardHeader className="border-b border-white/10">
          <CardTitle className="flex items-center gap-2 text-lg text-slate-50">
            <User className="h-5 w-5 text-emerald-200" />
            个人资料
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6 p-4 sm:p-6">
          {/* Avatar and basic info */}
          <div className="flex flex-col gap-5 rounded-2xl border border-white/10 bg-gradient-to-br from-emerald-300/12 via-cyan-300/8 to-slate-950/20 p-4 sm:flex-row sm:items-center sm:p-5">
            <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl border border-white/15 bg-gradient-to-br from-emerald-300 to-cyan-400 text-2xl font-bold text-slate-950 shadow-lg shadow-emerald-950/30">
              {userInfo?.fullName?.charAt(0).toUpperCase() || userInfo?.username?.charAt(0).toUpperCase() || 'U'}
            </div>
            <div className="flex-1">
              <h3 className="text-xl font-semibold text-slate-50">
                {userInfo?.fullName || '未设置姓名'}
              </h3>
              <p className="mt-1 text-sm text-slate-400">@{userInfo?.username}</p>
              <div className="mt-3 flex flex-wrap gap-2 text-xs">
                <span className="rounded-full border border-white/10 bg-slate-950/40 px-3 py-1 text-slate-300">
                  {userInfo?.role === 'ADMIN' ? '管理员' : '普通用户'}
                </span>
                <span className={`rounded-full border px-3 py-1 ${
                  userInfo?.emailVerified
                    ? 'border-emerald-300/25 bg-emerald-300/10 text-emerald-100'
                    : 'border-amber-300/25 bg-amber-300/10 text-amber-100'
                }`}>
                  {userInfo?.emailVerified ? '邮箱已验证' : '邮箱待验证'}
                </span>
              </div>
            </div>
            {!editing && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setEditing(true)}
                className={subtleButtonClass}
              >
                编辑
              </Button>
            )}
          </div>

          {/* User details */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className={labelClass}>
                <User className="h-4 w-4 text-emerald-200/80" />
                用户名
              </label>
              <input
                type="text"
                value={userInfo?.username || ''}
                disabled
                className={fieldClass}
              />
            </div>

            <div className="space-y-2">
              <label className={labelClass}>
                <Mail className="h-4 w-4 text-emerald-200/80" />
                邮箱
              </label>
              <div className="flex flex-col gap-2 sm:flex-row">
                <input
                  type="email"
                  value={userInfo?.email || ''}
                  disabled
                  className={`${fieldClass} min-w-0 flex-1`}
                />
                <span className={`inline-flex shrink-0 items-center gap-1 rounded-lg border px-3 text-sm font-medium ${
                  userInfo?.emailVerified
                    ? 'border-emerald-300/25 bg-emerald-300/10 text-emerald-100'
                    : 'border-amber-300/25 bg-amber-300/10 text-amber-100'
                }`}>
                  <BadgeCheck className="h-4 w-4" />
                  {userInfo?.emailVerified ? '已验证' : '未验证'}
                </span>
              </div>
            </div>

            <div className="space-y-2">
              <label className={labelClass}>
                <Shield className="h-4 w-4 text-emerald-200/80" />
                角色
              </label>
              <input
                type="text"
                value={userInfo?.role === 'ADMIN' ? '管理员' : '普通用户'}
                disabled
                className={fieldClass}
              />
            </div>

            <div className="space-y-2">
              <label className={labelClass}>
                <Calendar className="h-4 w-4 text-emerald-200/80" />
                注册时间
              </label>
              <input
                type="text"
                value={userInfo?.createdAt ? new Date(userInfo.createdAt).toLocaleDateString('zh-CN') : ''}
                disabled
                className={fieldClass}
              />
            </div>
          </div>

          {/* Editable name field */}
          {editing && (
            <div className="space-y-3 border-t border-white/10 pt-5">
              <label className="text-sm font-medium text-slate-300">
                姓名 *
              </label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="请输入您的姓名"
                className={fieldClass}
              />
              <div className="flex gap-2">
                <Button
                  onClick={handleSaveProfile}
                  disabled={saving}
                  className={`flex items-center gap-2 ${primaryButtonClass}`}
                >
                  <Save className="h-4 w-4" />
                  {saving ? '保存中...' : '保存'}
                </Button>
                <Button
                  variant="outline"
                  onClick={() => {
                    setEditing(false)
                    setFullName(userInfo?.fullName || '')
                  }}
                  disabled={saving}
                  className={subtleButtonClass}
                >
                  取消
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {!userInfo?.emailVerified && (
        <Card variant="bordered" className={pageCardClass}>
          <CardHeader className="border-b border-white/10">
            <CardTitle className="flex items-center gap-2 text-lg text-slate-50">
              <Mail className="h-5 w-5 text-amber-200" />
              邮箱验证
            </CardTitle>
          </CardHeader>
          <CardContent className="p-4 sm:p-6">
            <div className="flex flex-col gap-4 rounded-xl border border-amber-300/20 bg-amber-300/10 p-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h4 className="font-medium text-amber-100">确认邮箱后账号安全功能会更完整</h4>
                <p className="mt-1 text-sm leading-6 text-amber-100/75">
                  找回密码、安全提醒和账号确认邮件都会发送到 {userInfo?.email}。
                </p>
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={handleResendVerificationEmail}
                loading={sendingVerification}
                className="shrink-0 border border-amber-300/30 bg-amber-300/10 text-amber-100 hover:bg-amber-300/15"
              >
                重新发送
                <Send className="h-4 w-4" />
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Security Card */}
      <Card variant="bordered" className={pageCardClass}>
        <CardHeader className="border-b border-white/10">
          <CardTitle className="flex items-center gap-2 text-lg text-slate-50">
            <Key className="h-5 w-5 text-emerald-200" />
            安全设置
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4 p-4 sm:p-6">
          {!showPasswordForm ? (
            <div className={`${sectionPanelClass} flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between`}>
              <div>
                <h4 className="font-medium text-slate-100">修改密码</h4>
                <p className="text-sm text-slate-400 mt-1">定期更改密码有助于保护账户安全</p>
              </div>
              <Button
                variant="outline"
                onClick={() => setShowPasswordForm(true)}
                className={subtleButtonClass}
              >
                修改
              </Button>
            </div>
          ) : (
            <div className={`${sectionPanelClass} space-y-4`}>
              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-300">当前密码 *</label>
                <div className="relative">
                  <input
                    type={showCurrentPassword ? 'text' : 'password'}
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="请输入当前密码"
                    className={`${fieldClass} pr-10`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 transition hover:text-emerald-200"
                  >
                    {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-300">新密码 *</label>
                <div className="relative">
                  <input
                    type={showNewPassword ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="请输入新密码（至少6位）"
                    className={`${fieldClass} pr-10`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 transition hover:text-emerald-200"
                  >
                    {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-300">确认新密码 *</label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="请再次输入新密码"
                    className={`${fieldClass} pr-10`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 transition hover:text-emerald-200"
                  >
                    {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div className="flex gap-2 pt-2">
                <Button
                  onClick={handleChangePassword}
                  disabled={saving}
                  className={`flex items-center gap-2 ${primaryButtonClass}`}
                >
                  <Save className="h-4 w-4" />
                  {saving ? '保存中...' : '确认修改'}
                </Button>
                <Button
                  variant="outline"
                  onClick={() => {
                    setShowPasswordForm(false)
                    setCurrentPassword('')
                    setNewPassword('')
                    setConfirmPassword('')
                  }}
                  disabled={saving}
                  className={subtleButtonClass}
                >
                  取消
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Account Info Card */}
      <Card variant="bordered" className={pageCardClass}>
        <CardHeader className="border-b border-white/10">
          <CardTitle className="text-lg text-slate-50">账户信息</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 p-4 text-sm sm:p-6">
          <div className="flex justify-between rounded-lg border border-white/10 bg-slate-950/30 px-4 py-3">
            <span className="text-slate-400">账户状态</span>
            <span className="font-medium text-emerald-200">正常</span>
          </div>
          <div className="flex justify-between rounded-lg border border-white/10 bg-slate-950/30 px-4 py-3">
            <span className="text-slate-400">用户ID</span>
            <span className="font-mono text-slate-200">{userInfo?.id}</span>
          </div>
        </CardContent>
      </Card>
      </div>
    </div>
  )
}

export default ProfilePage
