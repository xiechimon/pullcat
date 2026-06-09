import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { ConfirmDialog } from '../components/ConfirmDialog'
import {
  createRule as createRuleApi,
  deleteRule as deleteRuleApi,
  getRules,
  getRuleSuggestions,
  toggleRule as toggleRuleApi,
  updateRule as updateRuleApi,
} from '../lib/api'
import type { RuleRespDTO, Severity } from '../types/review'

type Tab = 'rules' | 'suggestions'

export function RepoSettingsPage() {
  const { owner, repo } = useParams<{ owner: string; repo: string }>()
  const [rules, setRules] = useState<RuleRespDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<RuleRespDTO | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<Tab>('rules')
  const [suggestions, setSuggestions] = useState<RuleRespDTO[]>([])
  const [suggestionsLoading, setSuggestionsLoading] = useState(false)
  const [addedIds, setAddedIds] = useState<Set<string>>(new Set())
  const fullName = `${owner}/${repo}`

  const fetchRules = () => {
    if (!owner || !repo) return
    getRules(owner, repo)
      .then(setRules)
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  const fetchSuggestions = () => {
    if (!owner || !repo) return
    setSuggestionsLoading(true)
    getRuleSuggestions(owner, repo)
      .then((data) => {
        setSuggestions(data)
        setAddedIds(new Set())
      })
      .catch(() => {})
      .finally(() => setSuggestionsLoading(false))
  }

  useEffect(() => { fetchRules() }, [owner, repo])

  useEffect(() => {
    if (activeTab === 'suggestions' && suggestions.length === 0 && !suggestionsLoading) {
      fetchSuggestions()
    }
  }, [activeTab])

  const saveRule = async (rule: RuleRespDTO) => {
    try {
      if (!owner || !repo) return
      if (rule.id) {
        await updateRuleApi(owner, repo, rule.id, rule)
      } else {
        await createRuleApi(owner, repo, rule)
      }
      fetchRules()
      setShowForm(false)
      setEditing(null)
      toast.success('规则已保存')
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存规则失败')
    }
  }

  const adoptSuggestion = async (rule: RuleRespDTO) => {
    if (!owner || !repo) return
    const created = await createRuleApi(owner, repo, { ...rule, id: undefined })
    if (created) {
      setAddedIds(prev => new Set(prev).add(rule.id!))
      fetchRules()
    }
  }

  const adoptAllSuggestions = () => {
    suggestions
      .filter(s => !addedIds.has(s.id!))
      .forEach(s => {
        if (!owner || !repo) return
        createRuleApi(owner, repo, { ...s, id: undefined }).then(created => {
          if (created) {
            setAddedIds(prev => new Set(prev).add(s.id!))
            fetchRules()
          }
        })
      })
  }

  const toggleRule = async (rule: RuleRespDTO) => {
    try {
      if (!owner || !repo || !rule.id) return
      await toggleRuleApi(owner, repo, rule.id)
      fetchRules()
      toast.success(rule.enabled ? '规则已禁用' : '规则已启用')
    } catch (e) {
      fetchRules()
      toast.error(e instanceof Error ? e.message : '切换规则失败')
    }
  }

  const deleteRule = async (id: string) => {
    try {
      if (!owner || !repo) return
      await deleteRuleApi(owner, repo, id)
      fetchRules()
      toast.success('规则已删除')
    } catch (e) {
      fetchRules()
      toast.error(e instanceof Error ? e.message : '删除规则失败')
    }
  }

  const tabs: { key: Tab; label: string; badge?: number }[] = [
    { key: 'rules', label: '规则列表' },
    { key: 'suggestions', label: 'AI 建议', badge: suggestions.filter(s => !addedIds.has(s.id!)).length },
  ]

  const pendingSuggestions = suggestions.filter(s => !addedIds.has(s.id!))

  return (
    <div className="page-shell space-y-4 py-8 animate-fade-in">
      <section className="surface-card overflow-hidden">
        <div className="grid gap-0 md:grid-cols-[minmax(0,1.18fr)_18rem]">
          <div className="px-5 py-5 md:px-6 md:py-6">
            <div className="space-y-1">
              <div className="text-sm font-medium text-slate-500 dark:text-slate-400">当前关注点</div>
              <h1 className="text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">{fullName}</h1>
            </div>

            <div className="mt-4 flex flex-wrap gap-2">
              {tabs.map(tab => (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key)}
                  className={`inline-flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium transition-colors ${
                    activeTab === tab.key
                      ? 'bg-slate-950 text-white dark:bg-white dark:text-slate-950'
                      : 'bg-slate-100 text-slate-500 hover:text-slate-700 dark:bg-slate-900 dark:text-slate-400 dark:hover:text-slate-200'
                  }`}
                >
                  <span>{tab.label}</span>
                  {tab.badge != null && tab.badge > 0 && (
                    <span className={`rounded-full px-2 py-0.5 text-xs ${activeTab === tab.key ? 'bg-white/15 text-white dark:bg-slate-200 dark:text-slate-950' : 'bg-white text-slate-500 dark:bg-slate-950 dark:text-slate-400'}`}>
                      {tab.badge}
                    </span>
                  )}
                </button>
              ))}
            </div>

            {activeTab === 'rules' && (
              <div className="mt-5 space-y-4">
                <div className="flex justify-end">
                  <button
                    onClick={() => { setEditing(null); setShowForm(!showForm) }}
                    className="inline-flex items-center justify-center rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-800"
                  >
                    添加规则
                  </button>
                </div>

                {showForm && (
                  <RuleForm
                    rule={editing}
                    onSave={saveRule}
                    onCancel={() => { setShowForm(false); setEditing(null) }}
                  />
                )}

                {loading ? (
                  <div className="animate-pulse space-y-3">{[...Array(3)].map((_, i) => <div key={i} className="h-20 rounded-2xl bg-slate-200 dark:bg-slate-800" />)}</div>
                ) : rules.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-slate-300 px-6 py-10 text-center text-sm text-slate-400 dark:border-slate-700 dark:text-slate-500">
                    暂无自定义规则
                  </div>
                ) : (
                  <div className="space-y-3">
                    {rules.map(rule => (
                      <div key={rule.id} className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-4 dark:border-slate-800 dark:bg-slate-900/50">
                        <div className="flex items-start justify-between gap-4">
                          <div className="min-w-0 flex-1">
                            <div className="flex flex-wrap items-center gap-2">
                              <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${rule.enabled ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300' : 'bg-slate-200 text-slate-500 dark:bg-slate-800 dark:text-slate-400'}`}>
                                {rule.enabled ? '启用' : '禁用'}
                              </span>
                              <span className="text-xs text-slate-400 dark:text-slate-500">{rule.type}</span>
                              <span className="text-sm font-medium text-slate-950 dark:text-white">{rule.name}</span>
                            </div>
                            <code className="mt-2 block break-all text-xs text-emerald-700 dark:text-emerald-400">{rule.pattern}</code>
                            <div className="mt-2 text-xs text-slate-500 dark:text-slate-400">{rule.message}</div>
                          </div>
                          <div className="flex shrink-0 gap-2">
                            <button onClick={() => { setEditing(rule); setShowForm(true) }} className="text-xs font-medium text-slate-500 transition-colors hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200">编辑</button>
                            <button onClick={() => toggleRule(rule)} className="text-xs font-medium text-slate-500 transition-colors hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200">
                              {rule.enabled ? '禁用' : '启用'}
                            </button>
                            <button onClick={() => rule.id && setDeleteTarget(rule.id)} className="text-xs font-medium text-red-500 transition-colors hover:text-red-600">删除</button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'suggestions' && (
              <div className="mt-5 space-y-4">
                {suggestionsLoading ? (
                  <div className="animate-pulse space-y-3">{[...Array(3)].map((_, i) => (
                    <div key={i} className="h-24 rounded-2xl bg-slate-200 dark:bg-slate-800" />
                  ))}</div>
                ) : suggestions.length === 0 ? (
                  <div className="rounded-2xl border border-dashed border-slate-300 px-6 py-10 text-center text-sm text-slate-400 dark:border-slate-700 dark:text-slate-500">
                    暂无建议
                  </div>
                ) : (
                  <>
                    {pendingSuggestions.length > 0 && (
                      <div className="flex justify-end">
                        <button
                          onClick={adoptAllSuggestions}
                          className="inline-flex items-center justify-center rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-800"
                        >
                          一键添加 {pendingSuggestions.length} 条
                        </button>
                      </div>
                    )}
                    <div className="space-y-3">
                      {suggestions.map(rule => {
                        const isAdded = addedIds.has(rule.id!)
                        return (
                          <div key={rule.id} className={`rounded-2xl border px-4 py-4 transition-colors ${
                            isAdded
                              ? 'border-emerald-300 bg-emerald-50 dark:border-emerald-800 dark:bg-emerald-950/20'
                              : 'border-slate-200 bg-slate-50/80 dark:border-slate-800 dark:bg-slate-900/50'
                          }`}>
                            <div className="flex items-start justify-between gap-4">
                              <div className="min-w-0 flex-1">
                                <div className="flex flex-wrap items-center gap-2">
                                  <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                                    rule.severity === 'CRITICAL' ? 'bg-red-100 text-red-700 dark:bg-red-950/50 dark:text-red-300' :
                                    rule.severity === 'HIGH' ? 'bg-orange-100 text-orange-700 dark:bg-orange-950/50 dark:text-orange-300' :
                                    'bg-yellow-100 text-yellow-700 dark:bg-yellow-950/50 dark:text-yellow-300'
                                  }`}>
                                    {rule.severity}
                                  </span>
                                  <span className="text-sm font-medium text-slate-950 dark:text-white">{rule.name}</span>
                                </div>
                                <code className="mt-2 block break-all text-xs text-emerald-700 dark:text-emerald-400">{rule.pattern}</code>
                                <div className="mt-2 text-xs text-slate-500 dark:text-slate-400">{rule.message}</div>
                                {rule.suggestion && (
                                  <div className="mt-1 text-xs text-slate-400 dark:text-slate-500">{rule.suggestion}</div>
                                )}
                              </div>
                              <button
                                onClick={() => adoptSuggestion(rule)}
                                disabled={isAdded}
                                className={`shrink-0 rounded-xl px-3 py-2 text-xs font-medium transition-colors ${
                                  isAdded
                                    ? 'bg-slate-200 text-slate-400 dark:bg-slate-800 dark:text-slate-500'
                                    : 'bg-emerald-700 text-white hover:bg-emerald-800'
                                }`}
                              >
                                {isAdded ? '已添加' : '添加'}
                              </button>
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  </>
                )}
              </div>
            )}
          </div>

          <div className="border-t border-slate-200 bg-slate-50/70 px-5 py-5 dark:border-slate-800 dark:bg-slate-900/50 md:border-l md:border-t-0 md:px-6">
            <div className="text-xs font-semibold uppercase tracking-[0.14em] text-slate-400 dark:text-slate-500">工作台摘要</div>
            <div className="mt-4 space-y-3">
              <div className="rounded-xl bg-white px-4 py-4 dark:bg-slate-950/70">
                <div className="text-sm font-medium text-slate-500 dark:text-slate-400">现有规则</div>
                <div className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">{rules.length}</div>
              </div>
              <div className="rounded-xl bg-white px-4 py-4 dark:bg-slate-950/70">
                <div className="text-sm font-medium text-slate-500 dark:text-slate-400">待采纳建议</div>
                <div className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 dark:text-white">{pendingSuggestions.length}</div>
              </div>
              <div className="rounded-xl bg-white px-4 py-4 text-sm text-slate-500 dark:bg-slate-950/70 dark:text-slate-400">
                规则会在仓库审查时自动执行
              </div>
            </div>
          </div>
        </div>
      </section>

      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => { if (!open) setDeleteTarget(null) }}
        title="确认删除"
        description="确定删除此规则？此操作不可撤销。"
        confirmLabel="删除"
        onConfirm={() => { if (deleteTarget) deleteRule(deleteTarget) }}
      />
    </div>
  )
}

function RuleForm({ rule, onSave, onCancel }: { rule: RuleRespDTO | null; onSave: (r: RuleRespDTO) => void; onCancel: () => void }) {
  const [form, setForm] = useState<RuleRespDTO>(rule || {
    name: '', type: 'CODE_PATTERN', pattern: '', severity: 'MEDIUM', message: '', suggestion: '', enabled: true,
  })

  return (
    <div className="rounded-2xl border border-emerald-300 bg-emerald-50/70 p-5 space-y-3 dark:border-emerald-800 dark:bg-emerald-950/20">
      <input className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-950 outline-none transition focus:border-emerald-500 dark:border-slate-700 dark:bg-slate-950 dark:text-white" placeholder="规则名称" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
      <div className="flex gap-2">
        <select className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-950 outline-none transition focus:border-emerald-500 dark:border-slate-700 dark:bg-slate-950 dark:text-white" value={form.type} onChange={e => setForm({ ...form, type: e.target.value as RuleRespDTO['type'] })}>
          <option value="CODE_PATTERN">代码匹配</option>
          <option value="FORBIDDEN_API">禁用 API</option>
          <option value="FILE_PATH_MATCH">文件路径匹配</option>
        </select>
        <select className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-950 outline-none transition focus:border-emerald-500 dark:border-slate-700 dark:bg-slate-950 dark:text-white" value={form.severity} onChange={e => setForm({ ...form, severity: e.target.value as Severity })}>
          <option value="CRITICAL">CRITICAL</option>
          <option value="HIGH">HIGH</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="LOW">LOW</option>
          <option value="INFO">INFO</option>
        </select>
      </div>
      <input className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 font-mono text-sm text-slate-950 outline-none transition focus:border-emerald-500 dark:border-slate-700 dark:bg-slate-950 dark:text-white" placeholder="正则表达式" value={form.pattern} onChange={e => setForm({ ...form, pattern: e.target.value })} />
      <input className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-950 outline-none transition focus:border-emerald-500 dark:border-slate-700 dark:bg-slate-950 dark:text-white" placeholder="问题描述" value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} />
      <input className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-950 outline-none transition focus:border-emerald-500 dark:border-slate-700 dark:bg-slate-950 dark:text-white" placeholder="修复建议" value={form.suggestion} onChange={e => setForm({ ...form, suggestion: e.target.value })} />
      <div className="flex gap-2 justify-end">
        <button onClick={onCancel} className="px-4 py-2 text-sm font-medium text-slate-500 transition-colors hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200">取消</button>
        <button onClick={() => onSave(form)} className="rounded-xl bg-emerald-700 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-emerald-800">保存</button>
      </div>
    </div>
  )
}
