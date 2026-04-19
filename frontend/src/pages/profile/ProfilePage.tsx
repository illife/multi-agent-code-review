import React, { useState, useEffect } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { setUser, logout } from '../../store/slices/authSlice'
import { authService } from '../../services/auth.service'
import type { UserInfo } from '../../types'
import Card, { CardHeader, CardTitle, CardContent } from '../../components/ui/Card'
import Button from '../../components/ui/Button'
import { User, Mail, Shield, Calendar, Save, LogOut, Key, Eye, EyeOff } from 'lucide-react'

const ProfilePage: React.FC = () => {
  const dispatch = useDispatch()
  const user = useSelector((state: any) => state.auth.user)

  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null)

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
          <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-slate-200 border-slate-600"></div>
          <p className="mt-4 text-slate-600">加载中...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">个人设置</h1>
          <p className="text-slate-600 mt-1">管理您的个人信息和账户设置</p>
        </div>
        <Button
          variant="outline"
          onClick={handleLogout}
          className="text-red-600 hover:bg-red-50 hover:border-red-200"
        >
          <LogOut className="h-4 w-4 mr-2" />
          退出登录
        </Button>
      </div>

      {/* Message Banner */}
      {message && (
        <div className={`p-4 rounded-lg ${
          message.type === 'success'
            ? 'bg-green-50 text-green-800 border border-green-200'
            : 'bg-red-50 text-red-800 border border-red-200'
        }`}>
          {message.text}
        </div>
      )}

      {/* Profile Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <User className="h-5 w-5" />
            个人资料
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Avatar and basic info */}
          <div className="flex items-center gap-6">
            <div className="h-20 w-20 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white text-2xl font-bold">
              {userInfo?.fullName?.charAt(0).toUpperCase() || userInfo?.username?.charAt(0).toUpperCase() || 'U'}
            </div>
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-slate-900">
                {userInfo?.fullName || '未设置姓名'}
              </h3>
              <p className="text-slate-600">@{userInfo?.username}</p>
            </div>
            {!editing && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setEditing(true)}
              >
                编辑
              </Button>
            )}
          </div>

          {/* User details */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700 flex items-center gap-2">
                <User className="h-4 w-4" />
                用户名
              </label>
              <input
                type="text"
                value={userInfo?.username || ''}
                disabled
                className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-slate-50 text-slate-600"
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700 flex items-center gap-2">
                <Mail className="h-4 w-4" />
                邮箱
              </label>
              <input
                type="email"
                value={userInfo?.email || ''}
                disabled
                className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-slate-50 text-slate-600"
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700 flex items-center gap-2">
                <Shield className="h-4 w-4" />
                角色
              </label>
              <input
                type="text"
                value={userInfo?.role === 'ADMIN' ? '管理员' : '普通用户'}
                disabled
                className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-slate-50 text-slate-600"
              />
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700 flex items-center gap-2">
                <Calendar className="h-4 w-4" />
                注册时间
              </label>
              <input
                type="text"
                value={userInfo?.createdAt ? new Date(userInfo.createdAt).toLocaleDateString('zh-CN') : ''}
                disabled
                className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-slate-50 text-slate-600"
              />
            </div>
          </div>

          {/* Editable name field */}
          {editing && (
            <div className="space-y-2 pt-4 border-t">
              <label className="text-sm font-medium text-slate-700">
                姓名 *
              </label>
              <input
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="请输入您的姓名"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <div className="flex gap-2">
                <Button
                  onClick={handleSaveProfile}
                  disabled={saving}
                  className="flex items-center gap-2"
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
                >
                  取消
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Security Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Key className="h-5 w-5" />
            安全设置
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {!showPasswordForm ? (
            <div className="flex items-center justify-between p-4 bg-slate-50 rounded-lg">
              <div>
                <h4 className="font-medium text-slate-900">修改密码</h4>
                <p className="text-sm text-slate-600 mt-1">定期更改密码有助于保护账户安全</p>
              </div>
              <Button
                variant="outline"
                onClick={() => setShowPasswordForm(true)}
              >
                修改
              </Button>
            </div>
          ) : (
            <div className="space-y-4 p-4 bg-slate-50 rounded-lg">
              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-700">当前密码 *</label>
                <div className="relative">
                  <input
                    type={showCurrentPassword ? 'text' : 'password'}
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="请输入当前密码"
                    className="w-full px-3 py-2 pr-10 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-700"
                  >
                    {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-700">新密码 *</label>
                <div className="relative">
                  <input
                    type={showNewPassword ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="请输入新密码（至少6位）"
                    className="w-full px-3 py-2 pr-10 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-700"
                  >
                    {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-700">确认新密码 *</label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="请再次输入新密码"
                    className="w-full px-3 py-2 pr-10 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-700"
                  >
                    {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div className="flex gap-2 pt-2">
                <Button
                  onClick={handleChangePassword}
                  disabled={saving}
                  className="flex items-center gap-2"
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
                >
                  取消
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Account Info Card */}
      <Card>
        <CardHeader>
          <CardTitle>账户信息</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          <div className="flex justify-between">
            <span className="text-slate-600">账户状态</span>
            <span className="text-green-600 font-medium">正常</span>
          </div>
          <div className="flex justify-between">
            <span className="text-slate-600">用户ID</span>
            <span className="text-slate-900 font-mono">{userInfo?.id}</span>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

export default ProfilePage
