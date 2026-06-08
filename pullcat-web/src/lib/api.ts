import type {
  AutoPublishRepoRespDTO,
  BooleanStatusRespDTO,
  CompareReviewsRespDTO,
  CreateReviewRespDTO,
  CurrentUserRespDTO,
  DeletedRespDTO,
  PublishReviewRespDTO,
  RepoRespDTO,
  RepoStatsRespDTO,
  ReviewListRespDTO,
  ReviewSessionRespDTO,
  RuleRespDTO,
  StatsOverviewRespDTO,
  StatusRespDTO,
} from '../types/review'

const BASE_URL = import.meta.env.VITE_API_URL || ''

interface ApiResult<T> {
  success: boolean
  code: string
  message: string
  data: T
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${url}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    const raw = await res.text()
    let message = raw || `HTTP ${res.status}`
    try {
      const parsed = JSON.parse(raw)
      if (parsed.detail) console.error('[API Error Detail]', parsed.detail)
      message = parsed.message || parsed.error || message
    } catch {
      // not JSON, use raw text
    }
    throw new Error(message)
  }
  const payload = (await res.json()) as T | ApiResult<T>
  if (
    payload &&
    typeof payload === 'object' &&
    'success' in payload &&
    'code' in payload &&
    'message' in payload
  ) {
    const result = payload as ApiResult<T>
    if (!result.success) {
      throw new Error(result.message || 'Request failed')
    }
    return result.data
  }
  return payload as T
}

export async function createReview(prUrl: string): Promise<CreateReviewRespDTO> {
  return request<CreateReviewRespDTO>('/api/reviews', {
    method: 'POST',
    body: JSON.stringify({ prUrl }),
  })
}

export async function getReview(id: string): Promise<ReviewSessionRespDTO> {
  return request<ReviewSessionRespDTO>(`/api/reviews/${id}`)
}

export async function getReviews(page: number, size: number, repo?: string): Promise<ReviewListRespDTO> {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (repo) params.set('repo', repo)
  return request<ReviewListRespDTO>(`/api/reviews?${params}`)
}

export async function deleteReview(id: string): Promise<DeletedRespDTO> {
  return request<DeletedRespDTO>(`/api/reviews/${id}`, { method: 'DELETE' })
}

export async function submitFeedback(
  reviewId: string,
  issueId: string,
  accepted: boolean,
  reason?: string,
): Promise<StatusRespDTO> {
  return request<StatusRespDTO>(`/api/reviews/${reviewId}/issues/${issueId}/feedback`, {
    method: 'POST',
    body: JSON.stringify({ accepted, reason }),
  })
}

export async function publishReview(id: string, includeSummary: boolean, selectedIssueIds: string[]): Promise<PublishReviewRespDTO> {
  return request<PublishReviewRespDTO>(`/api/reviews/${id}/publish`, {
    method: 'POST',
    body: JSON.stringify({ includeSummary, selectedIssueIds }),
  })
}

export function createSSEConnection(sseUrl: string): EventSource {
  return new EventSource(`${BASE_URL}${sseUrl}`)
}

export async function getRepos(): Promise<RepoRespDTO[]> {
  return request<RepoRespDTO[]>('/api/repos')
}

export async function addRepo(owner: string, repo: string): Promise<RepoRespDTO> {
  return request<RepoRespDTO>('/api/repos', {
    method: 'POST',
    body: JSON.stringify({ owner, repo }),
  })
}

export async function deleteRepo(owner: string, repo: string): Promise<DeletedRespDTO> {
  return request<DeletedRespDTO>(`/api/repos/${owner}/${repo}`, { method: 'DELETE' })
}

export async function getRepo(owner: string, repo: string): Promise<RepoRespDTO> {
  return request<RepoRespDTO>(`/api/repos/${owner}/${repo}`)
}

export async function getStatsOverview(): Promise<StatsOverviewRespDTO> {
  return request<StatsOverviewRespDTO>('/api/stats/overview')
}

export async function getRepoStats(owner: string, repo: string): Promise<RepoStatsRespDTO> {
  return request<RepoStatsRespDTO>(`/api/repos/${owner}/${repo}/stats`)
}

export async function getCurrentUser(): Promise<CurrentUserRespDTO> {
  return request<CurrentUserRespDTO>('/api/user')
}

export async function compareReviews(id1: string, id2: string): Promise<CompareReviewsRespDTO> {
  return request<CompareReviewsRespDTO>('/api/reviews/compare', {
    method: 'POST',
    body: JSON.stringify({ reviewIds: [id1, id2] }),
  })
}

export async function getAutoPublishRepos(): Promise<AutoPublishRepoRespDTO[]> {
  return request<AutoPublishRepoRespDTO[]>('/api/auto-publish')
}

export async function setAutoPublish(owner: string, repo: string, enabled: boolean): Promise<BooleanStatusRespDTO> {
  return request<BooleanStatusRespDTO>(`/api/repos/${owner}/${repo}/auto-publish`, {
    method: 'PUT',
    body: JSON.stringify({ enabled }),
  })
}

export async function disableAutoPublish(owner: string, repo: string): Promise<BooleanStatusRespDTO> {
  return request<BooleanStatusRespDTO>(`/api/repos/${owner}/${repo}/auto-publish`, {
    method: 'DELETE',
  })
}

export async function getRules(owner: string, repo: string): Promise<RuleRespDTO[]> {
  return request<RuleRespDTO[]>(`/api/repos/${owner}/${repo}/rules`)
}

export async function getRuleSuggestions(owner: string, repo: string): Promise<RuleRespDTO[]> {
  return request<RuleRespDTO[]>(`/api/repos/${owner}/${repo}/rules/suggestions`)
}

export async function createRule(owner: string, repo: string, rule: RuleRespDTO): Promise<RuleRespDTO> {
  return request<RuleRespDTO>(`/api/repos/${owner}/${repo}/rules`, {
    method: 'POST',
    body: JSON.stringify(rule),
  })
}

export async function updateRule(owner: string, repo: string, ruleId: string, rule: RuleRespDTO): Promise<RuleRespDTO> {
  return request<RuleRespDTO>(`/api/repos/${owner}/${repo}/rules/${ruleId}`, {
    method: 'PUT',
    body: JSON.stringify(rule),
  })
}

export async function toggleRule(owner: string, repo: string, ruleId: string): Promise<RuleRespDTO> {
  return request<RuleRespDTO>(`/api/repos/${owner}/${repo}/rules/${ruleId}/toggle`, {
    method: 'PUT',
  })
}

export async function deleteRule(owner: string, repo: string, ruleId: string): Promise<DeletedRespDTO> {
  return request<DeletedRespDTO>(`/api/repos/${owner}/${repo}/rules/${ruleId}`, {
    method: 'DELETE',
  })
}

export async function logout(): Promise<StatusRespDTO> {
  return request<StatusRespDTO>('/api/logout', {
    method: 'POST',
  })
}
