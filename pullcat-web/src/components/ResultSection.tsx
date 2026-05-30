import { useState } from 'react'
import type { AnalysisResult, AnalysisType } from '../types/review'
import { TASK_LABELS } from '../types/review'
import { IssueCard } from './IssueCard'
import { MarkdownRenderer } from './MarkdownRenderer'

interface ResultSectionProps {
  type: AnalysisType
  result: AnalysisResult
  onIssueToggle: (issueId: string) => void
}

export function ResultSection({ type, result, onIssueToggle }: ResultSectionProps) {
  const [showLowConfidence, setShowLowConfidence] = useState(false)
  const issues = result.issues || []
  const visibleIssues = showLowConfidence
    ? issues
    : issues.filter((i) => i.confidence >= 0.5)
  const hiddenCount = issues.length - visibleIssues.length

  return (
    <div
      className="rounded-lg overflow-hidden"
      style={{
        backgroundColor: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
      }}
    >
      <div
        className="px-4 py-3 flex items-center justify-between"
        style={{
          backgroundColor: 'var(--color-primary-light)',
          borderBottom: '1px solid var(--color-border)',
        }}
      >
        <h3 className="text-base font-semibold" style={{ color: 'var(--color-text)' }}>
          {TASK_LABELS[type]}
        </h3>
        <div className="flex items-center gap-3 text-xs" style={{ color: 'var(--color-text-secondary)' }}>
          <span>{result.model}</span>
          {result.tokensUsed > 0 && <span>{result.tokensUsed} tokens</span>}
        </div>
      </div>

      <div className="p-4">
        {result.content && (
          <div className="mb-4 font-serif text-[1.05rem] leading-relaxed text-gray-800 dark:text-gray-200">
            <MarkdownRenderer content={extractSummaryText(result.content)} />
          </div>
        )}

        {issues.length === 0 && result.status === 'COMPLETED' ? (
          <div className="text-center py-4 text-sm" style={{ color: 'var(--color-text-secondary)' }}>
            未发现问题
          </div>
        ) : issues.length > 0 ? (
          <div className="space-y-3">
            {visibleIssues.map((issue) => (
              <IssueCard
                key={issue.id}
                {...issue}
                onToggle={onIssueToggle}
              />
            ))}
            {hiddenCount > 0 && (
              <button
                onClick={() => setShowLowConfidence(!showLowConfidence)}
                className="w-full text-center text-sm hover:underline py-2"
                style={{ color: 'var(--color-primary)' }}
              >
                {showLowConfidence
                  ? '隐藏低置信度问题'
                  : `显示 ${hiddenCount} 个低置信度问题`}
              </button>
            )}
          </div>
        ) : result.status === 'FAILED' ? (
          <div className="text-sm text-red-600">
            分析失败：{result.errorMessage || '未知错误'}
          </div>
        ) : null}
      </div>
    </div>
  )
}

function extractSummaryText(content: string): string {
  try {
    const json = JSON.parse(content)
    if (json.summary) return json.summary
  } catch {
    // JSON parse failed, use raw content
  }
  return content
}
