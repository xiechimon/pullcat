import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getCurrentUser } from '../lib/api'

export function SettingsPage() {
  const [user, setUser] = useState<{ authenticated: boolean; login?: string; avatarUrl?: string } | null>(null)

  useEffect(() => {
    let cancelled = false
    getCurrentUser().then(u => { if (!cancelled) setUser(u) }).catch(() => {})
    return () => { cancelled = true }
  }, [])

  return (
    <div className="max-w-2xl mx-auto px-4 py-8 space-y-6">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">设置</h1>

      <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 space-y-4">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">个人设置</h2>
        {user?.authenticated ? (
          <div className="flex items-center gap-3">
            {user.avatarUrl && <img src={user.avatarUrl} alt="" className="w-10 h-10 rounded-full" />}
            <div>
              <div className="font-medium text-gray-900 dark:text-white">{user.login}</div>
              <div className="text-sm text-gray-500">已通过 GitHub 登录</div>
            </div>
          </div>
        ) : (
          <Link to="/login" className="text-emerald-600 hover:underline text-sm">登录</Link>
        )}
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 space-y-4">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">GitHub Token</h2>
        <p className="text-sm text-gray-500">Pullcat 使用环境变量中的 GITHUB_TOKEN 访问 GitHub API。如需更换，请在 <code className="px-1 bg-gray-100 dark:bg-gray-700 rounded">.env</code> 文件中修改。</p>
        <a href="https://github.com/settings/tokens" target="_blank" rel="noopener noreferrer" className="text-sm text-emerald-600 hover:underline">
          管理 GitHub Token →
        </a>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 space-y-4">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Webhook 设置</h2>
        <p className="text-sm text-gray-500">配置 GitHub Webhook 后，PR 打开或更新时自动触发审查。</p>
        <div className="text-sm text-gray-400">
          Webhook URL: <code className="px-1 bg-gray-100 dark:bg-gray-700 rounded">https://your-domain/api/webhooks/github</code>
        </div>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 space-y-4">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">通知设置</h2>
        <p className="text-sm text-gray-500">审查完成时发送通知（即将推出）</p>
      </div>
    </div>
  )
}
