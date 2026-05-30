import { useState, useCallback, useMemo } from 'react'
import { PRInput } from '../components/PRInput'
import { ReviewProgress } from '../components/ReviewProgress'
import { ResultSection } from '../components/ResultSection'
import { ActionBar } from '../components/ActionBar'
import { useReview } from '../hooks/useReview'
import { usePublish } from '../hooks/usePublish'
import { ANALYSIS_TYPES } from '../types/review'
import type { AnalysisType } from '../types/review'
import { ThemeToggle } from '../components/ThemeToggle'

export function ReviewPage() {
  const { reviewId, error, loading, isAnalyzing, tasks, results, startReview } = useReview()
  const { publishing, publishError, published, publish } = usePublish()

  const [activeTaskType, setActiveTaskType] = useState<AnalysisType | null>(null)
  const [, forceUpdate] = useState(0)

  const handleStartReview = useCallback(async (prUrl: string) => {
    setActiveTaskType('summary')
    await startReview(prUrl)
  }, [startReview])

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

  const completedTasks = ANALYSIS_TYPES.filter(
    (t) => results[t]?.status === 'COMPLETED' || results[t]?.status === 'FAILED'
  )

  return (
    <div className="min-h-screen pb-20 font-sans transition-colors duration-300">
      <header className="fixed h-[60px] md:h-[80px] top-0 left-0 right-0 z-50 bg-white/80 dark:bg-[#0f172a]/80 backdrop-blur-sm border-b border-[#047857]/30 flex flex-row items-center justify-between px-4 lg:px-16">
        <div className="flex items-center gap-3">
          <img src="/cat.png" alt="Pullcat" className="h-7 md:h-10 w-auto object-contain" />
          <span className="font-serif font-bold text-2xl tracking-tighter text-[#047857]">Pullcat</span>
        </div>
        <div className="flex items-center gap-2">
          <a href="#" className="hidden sm:block text-sm font-semibold text-[#047857] hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors px-3 py-2 rounded-lg">Docs</a>
          <a href="#" className="text-sm font-semibold text-[#047857] hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors px-3 py-2 rounded-lg">Console</a>
          <ThemeToggle />
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
        <PRInput onSubmit={handleStartReview} loading={isAnalyzing} />
      </div>

      {error && (
        <div className="content-section">
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
            {error}
          </div>
        </div>
      )}

      {(loading || reviewId !== null) && (
        <div className="content-section space-y-6">
          <ReviewProgress 
            tasks={tasks}
            results={results}
            activeTaskType={activeTaskType}
            onTaskSelect={setActiveTaskType}
          />
        </div>
      )}

      {activeTaskType && (loading || reviewId !== null) && (
        <div className="content-section space-y-4">
          {(() => {
            const result = results[activeTaskType]
            const task = tasks.find((t) => t.name === activeTaskType)
            if (result) {
              return (
                <ResultSection
                  key={activeTaskType}
                  type={activeTaskType}
                  result={result}
                  onIssueToggle={handleIssueToggle}
                />
              )
            } else if (task) {
              return (
                <div key={`loading-${activeTaskType}`} className="rounded-xl p-16 flex flex-col items-center justify-center border-2 border-dashed border-gray-200 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/30 space-y-5 transition-all duration-300">
                  {task.status === 'RUNNING' ? (
                    <>
                      <svg className="animate-spin h-10 w-10 text-[#047857]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                      </svg>
                      <p className="text-gray-500 dark:text-gray-400 font-medium animate-pulse text-lg">正在分析 {task.label}...</p>
                    </>
                  ) : task.status === 'PENDING' ? (
                    <>
                      <div className="h-10 w-10 text-gray-300 dark:text-gray-600 flex items-center justify-center text-3xl">○</div>
                      <p className="text-gray-400 dark:text-gray-500 font-medium text-lg">等待分析 {task.label}...</p>
                    </>
                  ) : task.status === 'FAILED' ? (
                    <p className="text-red-500 font-medium text-lg">分析 {task.label} 失败</p>
                  ) : (
                    <p className="text-gray-500 dark:text-gray-400 font-medium text-lg">准备中...</p>
                  )}
                </div>
              )
            }
            return null
          })()}
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
