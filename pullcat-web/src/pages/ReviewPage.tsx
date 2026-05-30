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

  const { reviewId, error, isAnalyzing, tasks, results, resumeReview, toggleIssue } = useReviewReducer()
  const { publishing, publishError, published, publish } = usePublish()

  const [activeTaskType, setActiveTaskType] = useState<AnalysisType | null>(
    navigateState?.reviewId ? 'summary' : null
  )
  const initializedRef = useRef(false)

  useEffect(() => {
    if (initializedRef.current) return
    if (navigateState?.reviewId && navigateState?.sseUrl) {
      initializedRef.current = true
      resumeReview(navigateState.reviewId, navigateState.sseUrl)
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

  return (
    <div className="pb-20">
      {error && (
        <div className="content-section">
          <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
            {error}
          </div>
        </div>
      )}

      {isAnalyzing && (
        <div className="content-section space-y-6">
          <ReviewProgress
            tasks={tasks}
            results={results}
            activeTaskType={activeTaskType}
            onTaskSelect={setActiveTaskType}
          />
        </div>
      )}

      {activeTaskType && isAnalyzing && (
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
