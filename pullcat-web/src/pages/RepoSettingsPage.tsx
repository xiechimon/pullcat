import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { ConfirmDialog } from '../components/ConfirmDialog'
import type { Severity } from '../types/review'

interface Rule {
  id?: string
  name: string
  type: 'FILE_PATH_MATCH' | 'CODE_PATTERN' | 'FORBIDDEN_API'
  pattern: string
  severity: Severity
  message: string
  suggestion: string
  enabled: boolean
}

export function RepoSettingsPage() {
  const { owner, repo } = useParams<{ owner: string; repo: string }>()
  const [rules, setRules] = useState<Rule[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Rule | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null)
  const fullName = `${owner}/${repo}`

  const fetchRules = () => {
    fetch(`/api/repos/${owner}/${repo}/rules`, { credentials: 'include' })
      .then(r => r.json()).then(setRules)
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchRules() }, [owner, repo])

  const saveRule = async (rule: Rule) => {
    const method = rule.id ? 'PUT' : 'POST'
    const url = rule.id
      ? `/api/repos/${owner}/${repo}/rules/${rule.id}`
      : `/api/repos/${owner}/${repo}/rules`
    try {
      const res = await fetch(url, { method, credentials: 'include', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(rule) })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      fetchRules()
      setShowForm(false)
      setEditing(null)
      toast.success('规则已保存')
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '保存规则失败')
    }
  }

  const toggleRule = async (rule: Rule) => {
    try {
      const res = await fetch(`/api/repos/${owner}/${repo}/rules/${rule.id}/toggle`, { method: 'PUT', credentials: 'include' })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      fetchRules()
      toast.success(rule.enabled ? '规则已禁用' : '规则已启用')
    } catch (e) {
      fetchRules()
      toast.error(e instanceof Error ? e.message : '切换规则失败')
    }
  }

  const deleteRule = async (id: string) => {
    try {
      const res = await fetch(`/api/repos/${owner}/${repo}/rules/${id}`, { method: 'DELETE', credentials: 'include' })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      fetchRules()
      toast.success('规则已删除')
    } catch (e) {
      fetchRules()
      toast.error(e instanceof Error ? e.message : '删除规则失败')
    }
  }

  return (
    <div className="max-w-3xl mx-auto px-4 py-8 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{fullName}</h1>
          <p className="text-sm text-gray-500 mt-1">仓库设置 · 自定义规则</p>
        </div>
        <button
          onClick={() => { setEditing(null); setShowForm(!showForm) }}
          className="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 text-sm"
        >
          + 添加规则
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
        <div className="animate-pulse space-y-2">{[...Array(3)].map((_, i) => <div key={i} className="h-16 bg-gray-200 dark:bg-gray-700 rounded-lg" />)}</div>
      ) : rules.length === 0 ? (
        <div className="p-8 text-center text-gray-500 border border-dashed border-gray-300 dark:border-gray-600 rounded-xl">
          暂无自定义规则
        </div>
      ) : (
        <div className="space-y-3">
          {rules.map(rule => (
            <div key={rule.id} className="bg-white dark:bg-slate-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className={`text-xs px-1.5 py-0.5 rounded ${rule.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-400'}`}>
                      {rule.enabled ? '启用' : '禁用'}
                    </span>
                    <span className="text-xs text-gray-400">{rule.type}</span>
                    <span className="font-medium text-gray-900 dark:text-white">{rule.name}</span>
                  </div>
                  <code className="text-xs text-emerald-600 dark:text-emerald-400 mt-1 block">{rule.pattern}</code>
                  <div className="text-xs text-gray-500 mt-1">{rule.message}</div>
                </div>
                <div className="flex gap-1 ml-3">
                  <button onClick={() => { setEditing(rule); setShowForm(true) }} className="text-xs px-2 py-1 text-gray-500 hover:text-gray-700">编辑</button>
                  <button onClick={() => toggleRule(rule)} className="text-xs px-2 py-1 text-gray-500 hover:text-gray-700">
                    {rule.enabled ? '禁用' : '启用'}
                  </button>
                  <button onClick={() => rule.id && setDeleteTarget(rule.id)} className="text-xs px-2 py-1 text-red-400 hover:text-red-600">删除</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
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

function RuleForm({ rule, onSave, onCancel }: { rule: Rule | null; onSave: (r: Rule) => void; onCancel: () => void }) {
  const [form, setForm] = useState<Rule>(rule || {
    name: '', type: 'CODE_PATTERN', pattern: '', severity: 'MEDIUM', message: '', suggestion: '', enabled: true,
  })

  return (
    <div className="bg-white dark:bg-slate-800 rounded-xl border border-emerald-300 dark:border-emerald-700 p-6 space-y-3">
      <input className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-800 text-sm" placeholder="规则名称" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
      <div className="flex gap-2">
        <select className="px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-800 text-sm" value={form.type} onChange={e => setForm({ ...form, type: e.target.value as Rule['type'] })}>
          <option value="CODE_PATTERN">代码匹配</option>
          <option value="FORBIDDEN_API">禁用 API</option>
          <option value="FILE_PATH_MATCH">文件路径匹配</option>
        </select>
        <select className="px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-800 text-sm" value={form.severity} onChange={e => setForm({ ...form, severity: e.target.value as Severity })}>
          <option value="CRITICAL">CRITICAL</option>
          <option value="HIGH">HIGH</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="LOW">LOW</option>
          <option value="INFO">INFO</option>
        </select>
      </div>
      <input className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-800 text-sm font-mono" placeholder="正则表达式" value={form.pattern} onChange={e => setForm({ ...form, pattern: e.target.value })} />
      <input className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-800 text-sm" placeholder="问题描述" value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} />
      <input className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-800 text-sm" placeholder="修复建议" value={form.suggestion} onChange={e => setForm({ ...form, suggestion: e.target.value })} />
      <div className="flex gap-2 justify-end">
        <button onClick={onCancel} className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">取消</button>
        <button onClick={() => onSave(form)} className="px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm hover:bg-emerald-700">保存</button>
      </div>
    </div>
  )
}
