import { useState, useCallback, useMemo, useEffect, useRef } from 'react'
import { useParams, useLocation } from 'react-router-dom'
import { ReviewProgress } from '../components/ReviewProgress'
import { ResultSection } from '../components/ResultSection'
import { LoadingPlaceholder } from '../components/LoadingPlaceholder'
import { ActionBar } from '../components/ActionBar'
import { useReviewReducer } from '../hooks/useReviewReducer'
import { usePublish } from '../hooks/usePublish'
import { ANALYSIS_TYPES } from '../types/review'
import type { AnalysisType } from '../types/review'

interface NavigateState {
  reviewId?: string
  sseUrl?: string
}

export function ReviewPage() {
  const { id } = useParams<{ id: string }>()
  const location = useLocation()
  const navigateState = location.state as NavigateState | null

  const { reviewId, prUrl, prTitle, prOwner, prRepo, prNumber, prFileCount, prAdditions, prDeletions, error, isAnalyzing, tasks, results, resumeReview, toggleIssue } = useReviewReducer()
  const { publishing, publishError, published, publish } = usePublish()

  const [activeTaskType, setActiveTaskType] = useState<AnalysisType | null>(
    navigateState?.reviewId ? 'summary' : null
  )
  const startedRef = useRef(false)

  useEffect(() => {
    if (navigateState?.reviewId && navigateState?.sseUrl && !startedRef.current) {
      startedRef.current = true
      resumeReview(navigateState.reviewId, navigateState.sseUrl)
    }
    return () => {
      startedRef.current = false
    }
  }, [navigateState, resumeReview])

  const currentReviewId = id || reviewId

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
    if (!currentReviewId) return
    await publish(currentReviewId, true, selectedIssueIds)
  }, [currentReviewId, selectedIssueIds, publish])

  const completedTasks = ANALYSIS_TYPES.filter(
    (t) => results[t]?.status === 'COMPLETED' || results[t]?.status === 'FAILED'
  )
  const hasReview = isAnalyzing || completedTasks.length > 0

  return (
    <div className="pb-20">
      {error && (
        <div className="content-section">
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
            {error}
          </div>
        </div>
      )}

      {prUrl && (
        <div className="content-section">
          <div className="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 p-4 space-y-2">
            <a
              href={prUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm font-semibold text-emerald-700 hover:text-emerald-800 dark:text-emerald-400 dark:hover:text-emerald-300 transition-colors"
            >
              {prOwner}/{prRepo} #{prNumber}
            </a>
            {prTitle && (
              <p className="text-sm text-gray-700 dark:text-gray-300">{prTitle}</p>
            )}
            <div className="flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
              {prFileCount !== null && <span>{prFileCount} files changed</span>}
              {prAdditions !== null && (
                <span className="text-emerald-600 dark:text-emerald-400">+{prAdditions}</span>
              )}
              {prDeletions !== null && (
                <span className="text-red-500 dark:text-red-400">-{prDeletions}</span>
              )}
            </div>
          </div>
        </div>
      )}

      {hasReview && (
        <div className="content-section space-y-6">
          <ReviewProgress
            tasks={tasks}
            results={results}
            activeTaskType={activeTaskType}
            onTaskSelect={setActiveTaskType}
          />
        </div>
      )}

      {activeTaskType && (
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
                  onIssueToggle={toggleIssue}
                />
              )
            }
            if (task) {
              return <LoadingPlaceholder key={`loading-${activeTaskType}`} task={task} />
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
