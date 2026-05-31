import { useReducer, useRef, useCallback, useEffect } from 'react'
import { toast } from 'sonner'
import { createSSEConnection, createReview, getReview } from '../lib/api'
import type { AnalysisResult, AnalysisStatus, ReviewSession, TaskState } from '../types/review'
import { TASK_LABELS, ANALYSIS_TYPES } from '../types/review'

interface ReviewState {
  reviewId: string | null
  prUrl: string | null
  prTitle: string | null
  prOwner: string | null
  prRepo: string | null
  prNumber: number | null
  prFileCount: number | null
  prAdditions: number | null
  prDeletions: number | null
  rawDiff: string | null
  loading: boolean
  isAnalyzing: boolean
  error: string | null
  tasks: TaskState[]
  results: Record<string, AnalysisResult | null>
  ruleSuggestionUrl: string | null
}

type Action =
  | { type: 'START_SUBMIT' }
  | { type: 'SUBMIT_SUCCESS'; reviewId: string }
  | { type: 'SUBMIT_ERROR'; error: string }
  | { type: 'TASK_PROGRESS'; task: string; status: AnalysisStatus; model: string }
  | { type: 'TASK_RESULT'; result: AnalysisResult }
  | { type: 'ANALYSIS_COMPLETE' }
  | { type: 'TOGGLE_ISSUE'; issueId: string }
  | { type: 'SSE_CONNECT'; reviewId: string }
  | { type: 'PR_INFO'; prUrl: string; title: string; owner: string; repo: string; pullNumber: number; fileCount: number; additions: number; deletions: number; diff: string }
  | { type: 'LOAD_REVIEW'; session: ReviewSession }
  | { type: 'RULE_SUGGESTION'; url: string }

function createInitialTasks(): TaskState[] {
  return ANALYSIS_TYPES.map((name) => ({
    name,
    label: TASK_LABELS[name],
    status: 'PENDING' as AnalysisStatus,
    model: '',
    startedAt: null,
    completedAt: null,
  }))
}

function reviewReducer(state: ReviewState, action: Action): ReviewState {
  switch (action.type) {
    case 'START_SUBMIT':
      return {
        ...state,
        loading: true,
        isAnalyzing: true,
        error: null,
        tasks: createInitialTasks(),
        results: {},
      }

    case 'SUBMIT_SUCCESS':
      return {
        ...state,
        reviewId: action.reviewId,
        loading: true,
      }

    case 'SUBMIT_ERROR':
      return {
        ...state,
        loading: false,
        isAnalyzing: false,
        error: action.error,
      }

    case 'TASK_PROGRESS':
      return {
        ...state,
        tasks: state.tasks.map((t) =>
          t.name === action.task
            ? { ...t, status: action.status, model: action.model || t.model }
            : t
        ),
      }

    case 'TASK_RESULT':
      return {
        ...state,
        results: {
          ...state.results,
          [action.result.type.toLowerCase()]: action.result,
        },
      }

    case 'ANALYSIS_COMPLETE':
      return {
        ...state,
        loading: false,
        isAnalyzing: false,
      }

    case 'TOGGLE_ISSUE':
      return {
        ...state,
        results: Object.fromEntries(
          Object.entries(state.results).map(([key, result]) => {
            if (!result?.issues) return [key, result]
            return [
              key,
              {
                ...result,
                issues: result.issues.map((issue) =>
                  issue.id === action.issueId
                    ? { ...issue, selected: !issue.selected }
                    : issue
                ),
              },
            ]
          })
        ) as Record<string, AnalysisResult | null>,
      }

    case 'SSE_CONNECT':
      return {
        ...state,
        reviewId: action.reviewId,
      }

    case 'PR_INFO':
      return {
        ...state,
        prUrl: action.prUrl,
        prTitle: action.title,
        prOwner: action.owner,
        prRepo: action.repo,
        prNumber: action.pullNumber,
        prFileCount: action.fileCount,
        prAdditions: action.additions,
        prDeletions: action.deletions,
        rawDiff: action.diff,
      }

    case 'LOAD_REVIEW': {
      const session = action.session
      const meta = session.prMetadata
      const analyses = session.analyses || {}
      const loadedResults: Record<string, AnalysisResult | null> = {}
      const loadedTasks: TaskState[] = []

      for (const name of ANALYSIS_TYPES) {
        const result = analyses[name]
        if (result) {
          loadedResults[name] = result
          loadedTasks.push({
            name,
            label: TASK_LABELS[name],
            status: result.status,
            model: result.model,
            startedAt: result.startedAt,
            completedAt: result.completedAt,
          })
        } else {
          loadedResults[name] = null
          loadedTasks.push({
            name,
            label: TASK_LABELS[name],
            status: 'PENDING' as AnalysisStatus,
            model: '',
            startedAt: null,
            completedAt: null,
          })
        }
      }

      return {
        ...state,
        reviewId: session.id,
        prUrl: session.prUrl,
        prTitle: meta?.title || null,
        prOwner: meta?.owner || null,
        prRepo: meta?.repo || null,
        prNumber: meta?.pullNumber || null,
        prFileCount: meta?.fileCount || null,
        prAdditions: meta?.additions || null,
        prDeletions: meta?.deletions || null,
        rawDiff: session.rawDiff || null,
        loading: false,
        isAnalyzing: false,
        tasks: loadedTasks,
        results: loadedResults,
      }
    }

    case 'RULE_SUGGESTION':
      return {
        ...state,
        ruleSuggestionUrl: action.url,
      }

    default:
      return state
  }
}

function createInitialState(): ReviewState {
  return {
    reviewId: null,
    prUrl: null,
    prTitle: null,
    prOwner: null,
    prRepo: null,
    prNumber: null,
    prFileCount: null,
    prAdditions: null,
    prDeletions: null,
    rawDiff: null,
    loading: false,
    isAnalyzing: false,
    error: null,
    tasks: createInitialTasks(),
    results: {},
    ruleSuggestionUrl: null,
  }
}

interface UseReviewReducerReturn {
  reviewId: string | null
  prUrl: string | null
  prTitle: string | null
  prOwner: string | null
  prRepo: string | null
  prNumber: number | null
  prFileCount: number | null
  prAdditions: number | null
  prDeletions: number | null
  rawDiff: string | null
  error: string | null
  loading: boolean
  isAnalyzing: boolean
  tasks: TaskState[]
  results: Record<string, AnalysisResult | null>
  ruleSuggestionUrl: string | null
  startReview: (prUrl: string) => Promise<void>
  resumeReview: (reviewId: string, sseUrl: string) => void
  loadReview: (reviewId: string) => Promise<void>
  toggleIssue: (issueId: string) => void
  dismissRuleSuggestion: () => void
}

export function useReviewReducer(): UseReviewReducerReturn {
  const [state, dispatch] = useReducer(reviewReducer, null, createInitialState)
  const eventSourceRef = useRef<EventSource | null>(null)

  const connectSSE = useCallback((sseUrl: string) => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
    }

    const es = createSSEConnection(sseUrl)
    eventSourceRef.current = es

    es.addEventListener('connected', (event) => {
      const data = JSON.parse((event as MessageEvent).data)
      dispatch({ type: 'SSE_CONNECT', reviewId: data.reviewId })
    })

    es.addEventListener('analysis_started', () => {
      // Reset task statuses handled in START_SUBMIT
    })

    es.addEventListener('pr_info', (event) => {
      const data = JSON.parse((event as MessageEvent).data)
      const meta = data.metadata
      dispatch({
        type: 'PR_INFO',
        prUrl: data.prUrl,
        title: meta.title || '',
        owner: meta.owner || '',
        repo: meta.repo || '',
        pullNumber: meta.pullNumber || 0,
        fileCount: meta.fileCount || 0,
        additions: meta.additions || 0,
        deletions: meta.deletions || 0,
        diff: data.diff || '',
      })
    })

    es.addEventListener('task_progress', (event) => {
      const data = JSON.parse((event as MessageEvent).data)
      dispatch({
        type: 'TASK_PROGRESS',
        task: data.task,
        status: data.status as AnalysisStatus,
        model: data.model || '',
      })
    })

    es.addEventListener('task_result', (event) => {
      const data = JSON.parse((event as MessageEvent).data) as AnalysisResult
      dispatch({ type: 'TASK_RESULT', result: data })
    })

    es.addEventListener('all_complete', () => {
      dispatch({ type: 'ANALYSIS_COMPLETE' })
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
        eventSourceRef.current = null
      }
    })

    es.addEventListener('rule_suggestion', (event) => {
      const data = JSON.parse((event as MessageEvent).data)
      dispatch({ type: 'RULE_SUGGESTION', url: data.url || '' })
      toast.info(data.message || '发现潜在规则建议', {
        action: {
          label: '查看',
          onClick: () => window.location.href = data.url,
        },
      })
    })

    es.addEventListener('review_error', (event) => {
      const data = JSON.parse((event as MessageEvent).data)
      dispatch({
        type: 'SUBMIT_ERROR',
        error: data.message || 'Review failed',
      })
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
        eventSourceRef.current = null
      }
    })

    es.addEventListener('error', (event) => {
      try {
        const data = JSON.parse((event as MessageEvent).data)
        dispatch({
          type: 'SUBMIT_ERROR',
          error: data.message || 'Review failed',
        })
      } catch {
        dispatch({ type: 'SUBMIT_ERROR', error: 'SSE connection error' })
      }
    })

    es.onerror = () => {
      toast.warning('SSE 连接中断，正在重连...')
    }
  }, [])

  const startReview = useCallback(async (prUrl: string) => {
    dispatch({ type: 'START_SUBMIT' })

    try {
      const response = await createReview(prUrl)
      dispatch({ type: 'SUBMIT_SUCCESS', reviewId: response.reviewId })
      connectSSE(response.sseUrl)
    } catch (e) {
      dispatch({
        type: 'SUBMIT_ERROR',
        error: e instanceof Error ? e.message : 'Failed to start review',
      })
    }
  }, [connectSSE])

  const resumeReview = useCallback((id: string, sseUrl: string) => {
    dispatch({ type: 'START_SUBMIT' })
    dispatch({ type: 'SUBMIT_SUCCESS', reviewId: id })
    connectSSE(sseUrl)
  }, [connectSSE])

  const loadReview = useCallback(async (id: string) => {
    try {
      const session = await getReview(id)
      dispatch({ type: 'LOAD_REVIEW', session })
    } catch (e) {
      dispatch({
        type: 'SUBMIT_ERROR',
        error: e instanceof Error ? e.message : 'Failed to load review',
      })
    }
  }, [])

  const toggleIssue = useCallback((issueId: string) => {
    dispatch({ type: 'TOGGLE_ISSUE', issueId })
  }, [])

  const dismissRuleSuggestion = useCallback(() => {
    dispatch({ type: 'RULE_SUGGESTION', url: '' })
  }, [])

  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
      }
    }
  }, [])

  return {
    reviewId: state.reviewId,
    prUrl: state.prUrl,
    prTitle: state.prTitle,
    prOwner: state.prOwner,
    prRepo: state.prRepo,
    prNumber: state.prNumber,
    prFileCount: state.prFileCount,
    prAdditions: state.prAdditions,
    prDeletions: state.prDeletions,
    rawDiff: state.rawDiff,
    error: state.error,
    loading: state.loading,
    isAnalyzing: state.isAnalyzing,
    tasks: state.tasks,
    results: state.results,
    ruleSuggestionUrl: state.ruleSuggestionUrl,
    startReview,
    resumeReview,
    loadReview,
    toggleIssue,
    dismissRuleSuggestion,
  }
}
