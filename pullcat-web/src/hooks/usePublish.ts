import { useState, useCallback } from 'react'
import { publishReview } from '../lib/api'

interface UsePublishReturn {
  publishing: boolean
  publishError: string | null
  published: boolean
  commentId: number | null
  prUrl: string | null
  publish: (reviewId: string, includeSummary: boolean, selectedIssueIds: string[]) => Promise<boolean>
  reset: () => void
}

export function usePublish(): UsePublishReturn {
  const [publishing, setPublishing] = useState(false)
  const [publishError, setPublishError] = useState<string | null>(null)
  const [published, setPublished] = useState(false)
  const [commentId, setCommentId] = useState<number | null>(null)
  const [prUrl, setPrUrl] = useState<string | null>(null)

  const publish = useCallback(async (reviewId: string, includeSummary: boolean, selectedIssueIds: string[]) => {
    setPublishing(true)
    setPublishError(null)
    try {
      const result = await publishReview(reviewId, includeSummary, selectedIssueIds)
      setPublished(true)
      setCommentId(result.commentId)
      setPrUrl(result.prUrl)
      return true
    } catch (e) {
      setPublishError(e instanceof Error ? e.message : 'Failed to publish review')
      return false
    } finally {
      setPublishing(false)
    }
  }, [])

  const reset = useCallback(() => {
    setPublishing(false)
    setPublishError(null)
    setPublished(false)
    setCommentId(null)
    setPrUrl(null)
  }, [])

  return { publishing, publishError, published, commentId, prUrl, publish, reset }
}
