import { useState, useEffect, useRef, useCallback } from 'react'
import { createSSEConnection, createReview } from '../lib/api'
import type { AnalysisResult, AnalysisStatus, TaskState } from '../types/review'
import { TASK_LABELS, ANALYSIS_TYPES } from '../types/review'

interface UseReviewReturn {
  reviewId: string | null
  error: string | null
  loading: boolean
  isAnalyzing: boolean
  tasks: TaskState[]
  results: Record<string, AnalysisResult | null>
  startReview: (prUrl: string) => Promise<void>
  resumeReview: (reviewId: string, sseUrl: string) => void
}

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

export function useReview(): UseReviewReturn {
  const [reviewId, setReviewId] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [tasks, setTasks] = useState<TaskState[]>(createInitialTasks())
  const [results, setResults] = useState<Record<string, AnalysisResult | null>>({})
  const eventSourceRef = useRef<EventSource | null>(null)

  const connectSSE = useCallback((sseUrl: string) => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
    }

    const es = createSSEConnection(sseUrl)
    eventSourceRef.current = es

    es.onopen = () => {
      setLoading(false)
    }

    es.addEventListener('connected', (event) => {
      const data = JSON.parse((event as MessageEvent).data)
      setReviewId(data.reviewId)
    })

    es.addEventListener('analysis_started', () => {
      setTasks((prev) =>
        prev.map((t) => ({ ...t, status: 'PENDING' as AnalysisStatus }))
      )
    })

    es.addEventListener('task_progress', (event) => {
      const data = JSON.parse((event as MessageEvent).data)
      setTasks((prev) =>
        prev.map((t) =>
          t.name === data.task
            ? { ...t, status: data.status as AnalysisStatus, model: data.model || t.model }
            : t
        )
      )
    })

    es.addEventListener('task_result', (event) => {
      const data = JSON.parse((event as MessageEvent).data) as AnalysisResult
      setResults((prev) => ({
        ...prev,
        [data.type.toLowerCase()]: data,
      }))
    })

    es.addEventListener('all_complete', () => {
      setLoading(false)
      setIsAnalyzing(false)
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
        eventSourceRef.current = null
      }
    })

    es.onerror = () => {
      if (eventSourceRef.current) {
        setError('SSE connection error. Retrying...')
      }
    }
  }, [])

  const startReview = useCallback(async (prUrl: string) => {
    setLoading(true)
    setIsAnalyzing(true)
    setError(null)
    setTasks(createInitialTasks())
    setResults({})

    try {
      const response = await createReview(prUrl)
      setReviewId(response.reviewId)
      connectSSE(response.sseUrl)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to start review')
      setLoading(false)
      setIsAnalyzing(false)
    }
  }, [connectSSE])

  const resumeReview = useCallback((id: string, sseUrl: string) => {
    setReviewId(id)
    setLoading(true)
    setIsAnalyzing(true)
    setError(null)
    setTasks(createInitialTasks())
    setResults({})
    connectSSE(sseUrl)
  }, [connectSSE])

  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close()
      }
    }
  }, [])

  return { reviewId, error, loading, isAnalyzing, tasks, results, startReview, resumeReview }
}
