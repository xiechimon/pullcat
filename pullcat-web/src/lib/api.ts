const BASE_URL = import.meta.env.VITE_API_URL || ''

export interface CreateReviewResponse {
  reviewId: string
  status: string
  sseUrl: string
}

export interface PublishResponse {
  status: string
  commentId: number
  prUrl: string
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${url}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || `HTTP ${res.status}`)
  }
  return res.json()
}

export async function createReview(prUrl: string): Promise<CreateReviewResponse> {
  return request<CreateReviewResponse>('/api/reviews', {
    method: 'POST',
    body: JSON.stringify({ prUrl }),
  })
}

export async function getReview(id: string) {
  return request(`/api/reviews/${id}`)
}

export async function publishReview(id: string, includeSummary: boolean, selectedIssueIds: string[]): Promise<PublishResponse> {
  return request<PublishResponse>(`/api/reviews/${id}/publish`, {
    method: 'POST',
    body: JSON.stringify({ includeSummary, selectedIssueIds }),
  })
}

export function createSSEConnection(sseUrl: string): EventSource {
  return new EventSource(`${BASE_URL}${sseUrl}`)
}
