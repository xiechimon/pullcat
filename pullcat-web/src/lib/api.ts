import type { ReviewListResponse, ReviewSession, Repo, StatsOverview } from '../types/review'

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

export async function getReview(id: string): Promise<ReviewSession> {
  return request<ReviewSession>(`/api/reviews/${id}`)
}

export async function getReviews(page: number, size: number, repo?: string): Promise<ReviewListResponse> {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (repo) params.set('repo', repo)
  return request<ReviewListResponse>(`/api/reviews?${params}`)
}

export async function deleteReview(id: string): Promise<{ deleted: boolean }> {
  return request<{ deleted: boolean }>(`/api/reviews/${id}`, { method: 'DELETE' })
}

export async function submitFeedback(
  reviewId: string,
  issueId: string,
  accepted: boolean,
  reason?: string,
): Promise<{ status: string }> {
  return request<{ status: string }>(`/api/reviews/${reviewId}/issues/${issueId}/feedback`, {
    method: 'POST',
    body: JSON.stringify({ accepted, reason }),
  })
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

export async function getRepos(): Promise<Repo[]> {
  return request<Repo[]>('/api/repos')
}

export async function addRepo(owner: string, repo: string): Promise<Repo> {
  return request<Repo>('/api/repos', {
    method: 'POST',
    body: JSON.stringify({ owner, repo }),
  })
}

export async function deleteRepo(owner: string, repo: string): Promise<{ deleted: boolean }> {
  return request<{ deleted: boolean }>(`/api/repos/${owner}/${repo}`, { method: 'DELETE' })
}

export async function getRepo(owner: string, repo: string): Promise<Repo> {
  return request<Repo>(`/api/repos/${owner}/${repo}`)
}

export async function getStatsOverview(): Promise<StatsOverview> {
  return request<StatsOverview>('/api/stats/overview')
}

export async function getRepoStats(owner: string, repo: string): Promise<Record<string, unknown>> {
  return request<Record<string, unknown>>(`/api/repos/${owner}/${repo}/stats`)
}

export async function getCurrentUser(): Promise<{ authenticated: boolean; login?: string; avatarUrl?: string }> {
  return request('/api/user')
}

export async function compareReviews(id1: string, id2: string): Promise<Record<string, unknown>> {
  return request('/api/reviews/compare', {
    method: 'POST',
    body: JSON.stringify({ reviewIds: [id1, id2] }),
  })
}
