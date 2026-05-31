import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { getCurrentUser } from '../lib/api'

interface AutoPublishRepo {
  owner: string
  repo: string
  enabled: boolean
}

export function SettingsPage() {
  const [user, setUser] = useState<{ authenticated: boolean; login?: string; avatarUrl?: string } | null>(null)
  const [webhookRepo, setWebhookRepo] = useState('')
  const [autoRepoInput, setAutoRepoInput] = useState('')
  const [autoPublishRepos, setAutoPublishRepos] = useState<AutoPublishRepo[]>([])

  useEffect(() => {
    let cancelled = false
    getCurrentUser().then(u => { if (!cancelled) setUser(u) }).catch(() => {})
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    fetch('/api/auto-publish', { credentials: 'include' })
      .then(r => r.json())
      .then(setAutoPublishRepos)
      .catch(() => {})
  }, [])

  const addAutoPublish = useCallback((ownerRepo: string) => {
    if (!ownerRepo.includes('/')) return
    const [owner, repo] = ownerRepo.split('/')
    fetch(`/api/repos/${owner}/${repo}/auto-publish`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ enabled: true }),
    })
      .then(() => {
        setAutoPublishRepos(prev => {
          if (prev.some(r => r.owner === owner && r.repo === repo)) return prev
          return [...prev, { owner, repo, enabled: true }]
        })
      })
  }, [])

  const removeAutoPublish = useCallback((owner: string, repo: string) => {
    fetch(`/api/repos/${owner}/${repo}/auto-publish`, { method: 'DELETE', credentials: 'include' })
      .then(() => {
        setAutoPublishRepos(prev => prev.filter(r => !(r.owner === owner && r.repo === repo)))
      })
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
        <p className="text-sm text-gray-500">登录 GitHub 后即无需单独配置 Token。如需在未登录或 webhook 场景使用，请在 <code className="px-1 bg-gray-100 dark:bg-gray-700 rounded">.env</code> 文件中配置 GITHUB_TOKEN。</p>
        <a href="https://github.com/settings/tokens" target="_blank" rel="noopener noreferrer" className="text-sm text-emerald-600 hover:underline">
          管理 GitHub Token →
        </a>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 space-y-4">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Webhook 设置</h2>
        <p className="text-sm text-gray-500">配置 GitHub Webhook 后，PR 打开或更新时自动触发审查。</p>
        <div className="space-y-2">
          <label className="text-xs text-gray-500">你的仓库（owner/repo）</label>
          <div className="flex gap-2">
            <input
              type="text"
              value={webhookRepo}
              onChange={e => setWebhookRepo(e.target.value)}
              placeholder="owner/repo"
              className="flex-1 px-3 py-2 rounded-lg border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-800 text-sm text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
            <a
                href={webhookRepo.includes('/') ? `https://github.com/${webhookRepo}/settings/hooks/new` : '#'}
                target="_blank"
                rel="noopener noreferrer"
                className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors whitespace-nowrap ${
                  webhookRepo.includes('/')
                    ? 'bg-emerald-700 hover:bg-emerald-800 text-white'
                    : 'bg-gray-200 dark:bg-gray-700 text-gray-400 dark:text-gray-500 cursor-not-allowed pointer-events-none'
                }`}
              >
                管理 Webhook →
              </a>
          </div>
        </div>
        <div className="text-sm text-gray-400">
          Payload URL: <code className="px-1 bg-gray-100 dark:bg-gray-700 rounded">https://your-domain/api/webhooks/github</code>
        </div>
        <div className="text-sm text-gray-400">
          需选择事件类型：<code className="px-1 bg-gray-100 dark:bg-gray-700 rounded">Pull requests</code>
        </div>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 space-y-4">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">自动发布</h2>
        <p className="text-sm text-gray-500">开启后，该仓库的 PR 审查完成后自动将结果发布到 PR 评论。</p>

        {autoPublishRepos.length > 0 && (
          <div className="space-y-2">
            {autoPublishRepos.map(r => (
              <div key={`${r.owner}/${r.repo}`} className="flex items-center justify-between py-2">
                <div className="flex items-center gap-3">
                  <span className="w-2 h-2 rounded-full bg-emerald-500" />
                  <span className="text-sm font-medium text-gray-900 dark:text-white">{r.owner}/{r.repo}</span>
                </div>
                <button
                  onClick={() => removeAutoPublish(r.owner, r.repo)}
                  className="text-xs text-red-500 hover:text-red-700 px-2 py-1"
                >
                  关闭
                </button>
              </div>
            ))}
          </div>
        )}

        <div className="border-t border-gray-100 dark:border-gray-700 pt-3">
          <p className="text-xs text-gray-400 mb-2">输入 owner/repo 添加自动发布：</p>
          <div className="flex gap-2">
            <input
              type="text"
              value={autoRepoInput}
              onChange={e => setAutoRepoInput(e.target.value)}
              placeholder="owner/repo"
              onKeyDown={e => { if (e.key === 'Enter') { addAutoPublish(autoRepoInput); setAutoRepoInput('') } }}
              className="flex-1 px-3 py-2 text-sm rounded-lg border border-gray-200 dark:border-gray-600 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
            />
            <button
              onClick={() => { addAutoPublish(autoRepoInput); setAutoRepoInput('') }}
              disabled={!autoRepoInput.includes('/')}
              className="px-4 py-2 text-sm font-medium bg-emerald-700 hover:bg-emerald-800 disabled:bg-gray-300 disabled:cursor-not-allowed text-white rounded-lg transition-colors whitespace-nowrap"
            >
              添加
            </button>
          </div>
        </div>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 space-y-4">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">通知设置</h2>
        <p className="text-sm text-gray-500">审查完成时发送通知（即将推出）</p>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 space-y-4">
        <h2 className="text-lg font-semibold text-gray-900 dark:text-white">自定义规则</h2>
        <p className="text-sm text-gray-500">为仓库配置自定义检测规则，审查时自动运行（即将推出）</p>
      </div>
    </div>
  )
}
