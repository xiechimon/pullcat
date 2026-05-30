import { useState, useCallback, useMemo } from 'react'
import { PRInput } from '../components/PRInput'
import { ReviewProgress } from '../components/ReviewProgress'
import { ResultSection } from '../components/ResultSection'
import { ActionBar } from '../components/ActionBar'
import { useReview } from '../hooks/useReview'
import { usePublish } from '../hooks/usePublish'
import { ANALYSIS_TYPES } from '../types/review'

export function ReviewPage() {
  const { reviewId, error, loading, tasks, results, startReview } = useReview()
  const { publishing, publishError, published, publish } = usePublish()

  const [, forceUpdate] = useState(0)

  const handleIssueToggle = useCallback((issueId: string) => {
    for (const type of ANALYSIS_TYPES) {
      const result = results[type]
      if (result?.issues) {
        const issue = result.issues.find((i) => i.id === issueId)
        if (issue) {
          issue.selected = !issue.selected
          forceUpdate((n) => n + 1)
          return
        }
      }
    }
  }, [results])

  const { selectedCount, totalCount, selectedIssueIds } = useMemo(() => {
    let sel = 0
    let total = 0
    const ids: string[] = []
    for (const type of ANALYSIS_TYPES) {
      const result = results[type]
      if (result?.issues) {
        for (const issue of result.issues) {
          total++
          if (issue.selected) {
            sel++
            ids.push(issue.id)
          }
        }
      }
    }
    return { selectedCount: sel, totalCount: total, selectedIssueIds: ids }
  }, [results])

  const handlePublish = useCallback(async () => {
    if (!reviewId) return
    await publish(reviewId, true, selectedIssueIds)
  }, [reviewId, selectedIssueIds, publish])

  const hasResults = Object.keys(results).length > 0
  const completedTasks = ANALYSIS_TYPES.filter(
    (t) => results[t]?.status === 'COMPLETED' || results[t]?.status === 'FAILED'
  )

  return (
    <div className="min-h-screen pb-20 font-sans">
      <header className="fixed h-[60px] md:h-[80px] top-0 left-0 right-0 z-50 bg-white/80 dark:bg-[#0f172a]/80 backdrop-blur-sm border-b border-[#047857]/30 flex flex-row items-center justify-between px-4 lg:px-16">
        <div className="flex items-center gap-3">
          <img src="/cat.png" alt="Pullcat" className="h-7 md:h-10 w-auto object-contain" />
          <span className="font-serif font-bold text-2xl tracking-tighter text-[#047857]">Pullcat</span>
        </div>
        <div className="flex items-center gap-2">
          <a href="#" className="hidden sm:block text-sm font-semibold text-[#047857] hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors px-3 py-2 rounded-lg">Docs</a>
          <a href="#" className="text-sm font-semibold text-[#047857] hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors px-3 py-2 rounded-lg">Console</a>
        </div>
      </header>

      <div className="pt-28 md:pt-36 pb-12 px-4 flex flex-col items-center text-center">
        <h1 className="text-4xl md:text-5xl lg:text-6xl font-semibold font-serif text-[#047857] leading-tight max-w-4xl mb-6 tracking-tight">
          AI 驱动的 Pull Request 代码审查助手
        </h1>
        <p className="text-lg md:text-xl font-serif text-gray-600 dark:text-gray-400 max-w-2xl mb-10 leading-relaxed">
          输入 GitHub PR 链接，自动获取代码变更并进行多维度 AI 分析，审查后一键发布到 PR。
        </p>
      </div>

      <div className="input-card">
        <PRInput onSubmit={startReview} loading={loading} />
      </div>

      {error && (
        <div className="content-section">
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
            {error}
          </div>
        </div>
      )}

      {(loading || hasResults) && (
        <div className="content-section space-y-6">
          <ReviewProgress tasks={tasks} />
        </div>
      )}

      {hasResults && (
        <div className="content-section space-y-4">
          {ANALYSIS_TYPES.map((type) => {
            const result = results[type]
            if (!result) return null
            return (
              <ResultSection
                key={type}
                type={type}
                result={result}
                onIssueToggle={handleIssueToggle}
              />
            )
          })}
        </div>
      )}

      {completedTasks.length > 0 && totalCount >= 0 && !published && (
        <ActionBar
          selectedCount={selectedCount}
          totalCount={totalCount}
          publishing={publishing}
          published={published}
          publishError={publishError}
          onPublish={handlePublish}
        />
      )}
    </div>
  )
}
