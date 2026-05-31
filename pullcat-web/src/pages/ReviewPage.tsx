import { useState, useCallback, useMemo, useEffect, useRef } from 'react'
import { useParams, useLocation } from 'react-router-dom'
import { toast } from 'sonner'
import { ReviewProgress } from '../components/ReviewProgress'
import { ResultSection } from '../components/ResultSection'
import { LoadingPlaceholder } from '../components/LoadingPlaceholder'
import { ActionBar } from '../components/ActionBar'
import { DiffViewer } from '../components/DiffViewer'
import { IssuePanel } from '../components/IssuePanel'
import { useReviewReducer } from '../hooks/useReviewReducer'
import { usePublish } from '../hooks/usePublish'
import { ANALYSIS_TYPES } from '../types/review'
import type { AnalysisType, Severity } from '../types/review'

interface NavigateState {
  reviewId?: string
  sseUrl?: string
}

export function ReviewPage() {
  const { id } = useParams<{ id: string }>()
  const location = useLocation()
  const navigateState = location.state as NavigateState | null

  const { reviewId, prUrl, prTitle, prOwner, prRepo, prNumber, prFileCount, prAdditions, prDeletions, rawDiff, error, isAnalyzing, tasks, results, ruleSuggestionUrl, resumeReview, loadReview, toggleIssue, dismissRuleSuggestion } = useReviewReducer()
  const { publishing, publishError, published, publish } = usePublish()

  const [activeTaskType, setActiveTaskType] = useState<AnalysisType | null>(
    navigateState?.reviewId ? 'summary' : null
  )
  const [activeIssueId, setActiveIssueId] = useState<string | null>(null)
  const startedRef = useRef(false)

  useEffect(() => {
    if (error) toast.error(error)
  }, [error])

  useEffect(() => {
    if (publishError) toast.error(publishError)
  }, [publishError])

  useEffect(() => {
    if (navigateState?.reviewId && navigateState?.sseUrl && !startedRef.current) {
      startedRef.current = true
      resumeReview(navigateState.reviewId, navigateState.sseUrl)
    } else if (!navigateState?.sseUrl && id && !startedRef.current) {
      startedRef.current = true
      setActiveTaskType('summary')
      loadReview(id)
    }
    return () => {
      startedRef.current = false
    }
  }, [navigateState, id, resumeReview, loadReview])

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

  const activeResult = activeTaskType ? results[activeTaskType] : null
  const activeIssues = activeResult?.issues || []

  const handleToggle = useCallback((issueId: string) => {
    toggleIssue(issueId)
  }, [toggleIssue])

  const handleSelectAll = useCallback(() => {
    for (const issue of activeIssues) {
      if (!issue.selected) toggleIssue(issue.id)
    }
  }, [activeIssues, toggleIssue])

  const handleDeselectAll = useCallback(() => {
    for (const issue of activeIssues) {
      if (issue.selected) toggleIssue(issue.id)
    }
  }, [activeIssues, toggleIssue])

  const handleSelectHighAbove = useCallback(() => {
    const highAbove: Severity[] = ['CRITICAL', 'HIGH']
    for (const issue of activeIssues) {
      if (highAbove.includes(issue.severity) && !issue.selected) toggleIssue(issue.id)
      else if (!highAbove.includes(issue.severity) && issue.selected) toggleIssue(issue.id)
    }
  }, [activeIssues, toggleIssue])

  const handlePublish = useCallback(async () => {
    if (!currentReviewId) return
    const ok = await publish(currentReviewId, true, selectedIssueIds)
    if (ok) {
      toast.success('发布成功！')
    }
  }, [currentReviewId, selectedIssueIds, publish])

  const completedTasks = ANALYSIS_TYPES.filter(
    (t) => results[t]?.status === 'COMPLETED' || results[t]?.status === 'FAILED'
  )
  const hasReview = isAnalyzing || completedTasks.length > 0

  const activeDiff = rawDiff || ''

  return (
    <div className="pb-20">
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

      {ruleSuggestionUrl && !isAnalyzing && (
        <div className="bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-300 dark:border-emerald-700 rounded-lg p-4 flex items-center justify-between">
          <p className="text-sm text-emerald-800 dark:text-emerald-300">
            发现潜在规则建议，可自动检测该仓库的重复问题模式
          </p>
          <div className="flex gap-2">
            <a
              href={ruleSuggestionUrl}
              className="px-3 py-1.5 text-xs bg-emerald-600 text-white rounded-lg hover:bg-emerald-700"
            >
              查看建议
            </a>
            <button
              onClick={dismissRuleSuggestion}
              className="px-3 py-1.5 text-xs text-gray-500 hover:text-gray-700"
            >
              忽略
            </button>
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
        <div className="content-section">
          {activeResult ? (
            <>
              {activeResult.content && activeResult.content.includes('"summary"') && (
                <div className="mb-4">
                  <ResultSection
                    type={activeTaskType}
                    result={activeResult}
                    onIssueToggle={toggleIssue}
                  />
                </div>
              )}
              <div className="grid grid-cols-1 lg:grid-cols-5 gap-4" style={{ minHeight: '60vh' }}>
                <div className="lg:col-span-3">
                  <DiffViewer
                    diff={activeDiff}
                    issues={activeIssues}
                    activeIssueId={activeIssueId}
                    onIssueClick={setActiveIssueId}
                  />
                </div>
                <div className="lg:col-span-2" style={{ maxHeight: 'calc(100vh - 300px)' }}>
                  <IssuePanel
                    issues={activeIssues}
                    activeIssueId={activeIssueId}
                    onToggle={handleToggle}
                    onSelectAll={handleSelectAll}
                    onDeselectAll={handleDeselectAll}
                    onSelectHighAbove={handleSelectHighAbove}
                    onIssueClick={setActiveIssueId}
                  />
                </div>
              </div>
            </>
          ) : (
            <LoadingPlaceholder
              task={tasks.find(t => t.name === activeTaskType) || {
                name: activeTaskType,
                label: activeTaskType,
                status: 'PENDING',
                model: '',
                startedAt: null,
                completedAt: null
              }}
            />
          )}
        </div>
      )}

      {completedTasks.length > 0 && totalCount >= 0 && !published && (
        <ActionBar
          selectedCount={selectedCount}
          totalCount={totalCount}
          publishing={publishing}
          published={published}
          disabled={isAnalyzing}
          onPublish={handlePublish}
        />
      )}
    </div>
  )
}
