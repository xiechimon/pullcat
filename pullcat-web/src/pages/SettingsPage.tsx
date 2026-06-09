import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { disableAutoPublish, getAutoPublishRepos, getCurrentUser, setAutoPublish } from '../lib/api'
import type { AutoPublishRepoRespDTO, CurrentUserRespDTO } from '../types/review'

export function SettingsPage() {
  const [user, setUser] = useState<CurrentUserRespDTO | null>(null)
  const [webhookRepo, setWebhookRepo] = useState('')
  const [autoRepoInput, setAutoRepoInput] = useState('')
  const [autoPublishRepos, setAutoPublishRepos] = useState<AutoPublishRepoRespDTO[]>([])

  useEffect(() => {
    let cancelled = false
    getCurrentUser().then(u => { if (!cancelled) setUser(u) }).catch(() => {})
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    getAutoPublishRepos()
      .then(setAutoPublishRepos)
      .catch(() => {})
  }, [])

  const addAutoPublish = useCallback((ownerRepo: string) => {
    if (!ownerRepo.includes('/')) return
    const [owner, repo] = ownerRepo.split('/')
    setAutoPublish(owner, repo, true)
      .then(() => {
        setAutoPublishRepos(prev => {
          if (prev.some(r => r.owner === owner && r.repo === repo)) return prev
          return [...prev, { owner, repo, enabled: true }]
        })
      })
  }, [])

  const removeAutoPublish = useCallback((owner: string, repo: string) => {
    disableAutoPublish(owner, repo)
      .then(() => {
        setAutoPublishRepos(prev => prev.filter(r => !(r.owner === owner && r.repo === repo)))
      })
  }, [])

  const webhookReady = webhookRepo.includes('/')
  const isAuthenticated = Boolean(user?.authenticated)

  return (
    <div className="page-shell space-y-4 py-8 animate-fade-in">
      <section className="surface-card overflow-hidden">
        <div className="grid gap-0 md:grid-cols-[minmax(0,1.18fr)_18rem]">
          <div className="px-5 py-5 md:px-6 md:py-6">
            <div className="space-y-1">
              <div className="text-sm font-medium text-slate-500 dark:text-slate-400">当前关注点</div>
              <h1 className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">先把连接和自动化配好</h1>
            </div>

            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              <div className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4 dark:border-slate-800 dark:bg-slate-900/50">
                <div className="text-sm font-medium text-slate-500 dark:text-slate-400">登录状态</div>
                {isAuthenticated ? (
                  <div className="mt-4 flex items-center gap-3">
                    {user?.avatarUrl && <img src={user.avatarUrl} alt="" className="h-10 w-10 rounded-full" />}
                    <div>
                      <div className="text-sm font-medium text-slate-950 dark:text-white">{user?.login}</div>
                      <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">GitHub 已连接</div>
                    </div>
                  </div>
                ) : (
                  <Link to="/login" className="mt-4 inline-flex text-sm font-medium text-emerald-700 hover:text-emerald-800 dark:text-emerald-400 dark:hover:text-emerald-300">
                    去登录
                  </Link>
                )}
              </div>

              <div className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4 dark:border-slate-800 dark:bg-slate-900/50">
                <div className="text-sm font-medium text-slate-500 dark:text-slate-400">Token</div>
                <div className="mt-4 text-sm text-slate-950 dark:text-white">默认跟随 GitHub 登录</div>
                <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">Webhook 或本地场景再补 `.env`</div>
                <a href="https://github.com/settings/tokens" target="_blank" rel="noopener noreferrer" className="mt-4 inline-flex text-sm font-medium text-emerald-700 hover:text-emerald-800 dark:text-emerald-400 dark:hover:text-emerald-300">
                  管理 Token
                </a>
              </div>
            </div>

            <div className="mt-5 rounded-2xl border border-slate-200 bg-white px-4 py-4 dark:border-slate-800 dark:bg-slate-950/60">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                <label className="min-w-0 flex-1 space-y-2">
                  <span className="text-sm font-medium text-slate-500 dark:text-slate-400">Webhook 仓库</span>
                  <input
                    type="text"
                    value={webhookRepo}
                    onChange={e => setWebhookRepo(e.target.value)}
                    placeholder="owner/repo"
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-950 outline-none transition focus:border-emerald-500 focus:bg-white dark:border-slate-700 dark:bg-slate-900 dark:text-white dark:focus:bg-slate-950"
                  />
                </label>
                <a
                  href={webhookReady ? `https://github.com/${webhookRepo}/settings/hooks/new` : '#'}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`inline-flex items-center justify-center rounded-xl px-4 py-2.5 text-sm font-medium transition-colors ${
                    webhookReady
                      ? 'bg-emerald-700 text-white hover:bg-emerald-800'
                      : 'pointer-events-none bg-slate-200 text-slate-400 dark:bg-slate-800 dark:text-slate-500'
                  }`}
                >
                  管理 Webhook
                </a>
              </div>

              <div className="mt-4 space-y-2 text-xs text-slate-400 dark:text-slate-500">
                <div>Payload URL: <code className="rounded bg-slate-100 px-1.5 py-0.5 dark:bg-slate-900">https://your-domain/api/pullcat/v1/webhooks/github</code></div>
                <div>事件类型: <code className="rounded bg-slate-100 px-1.5 py-0.5 dark:bg-slate-900">Pull requests</code></div>
              </div>
            </div>
          </div>

          <div className="border-t border-slate-200 bg-slate-50/70 px-5 py-5 dark:border-slate-800 dark:bg-slate-900/50 md:border-l md:border-t-0 md:px-6">
            <div className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400 dark:text-slate-500">自动发布</div>
            <div className="mt-4 space-y-3">
              {autoPublishRepos.length === 0 ? (
                <div className="rounded-xl border border-dashed border-slate-300 bg-white px-4 py-6 text-center text-sm text-slate-400 dark:border-slate-700 dark:bg-slate-950/60 dark:text-slate-500">
                  还没有自动发布仓库
                </div>
              ) : (
                autoPublishRepos.map(r => (
                  <div key={`${r.owner}/${r.repo}`} className="flex items-center justify-between gap-3 rounded-xl bg-white px-4 py-3 dark:bg-slate-950/70">
                    <span className="truncate text-sm font-medium text-slate-950 dark:text-white">{r.owner}/{r.repo}</span>
                    <button
                      onClick={() => removeAutoPublish(r.owner, r.repo)}
                      className="text-xs font-medium text-red-500 transition-colors hover:text-red-600"
                    >
                      关闭
                    </button>
                  </div>
                ))
              )}
            </div>

            <div className="mt-4 space-y-2">
              <div className="text-sm font-medium text-slate-500 dark:text-slate-400">添加仓库</div>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={autoRepoInput}
                  onChange={e => setAutoRepoInput(e.target.value)}
                  placeholder="owner/repo"
                  onKeyDown={e => { if (e.key === 'Enter') { addAutoPublish(autoRepoInput); setAutoRepoInput('') } }}
                  className="min-w-0 flex-1 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-950 outline-none transition focus:border-emerald-500 dark:border-slate-700 dark:bg-slate-950 dark:text-white"
                />
                <button
                  onClick={() => { addAutoPublish(autoRepoInput); setAutoRepoInput('') }}
                  disabled={!autoRepoInput.includes('/')}
                  className="rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-800 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-400 dark:disabled:bg-slate-800 dark:disabled:text-slate-500"
                >
                  添加
                </button>
              </div>
              <div className="text-xs text-slate-400 dark:text-slate-500">审查结束后会直接回写 PR 评论</div>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
