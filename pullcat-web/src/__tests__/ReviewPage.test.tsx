/// <reference types="@testing-library/jest-dom" />
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ReviewPage } from '../pages/ReviewPage'

interface NavigateState {
  reviewId?: string
  sseUrl?: string
}

function renderWithRouter(state?: NavigateState) {
  return render(
    <MemoryRouter initialEntries={[state ? { pathname: '/review/test-id', state } : '/review/test-id']}>
      <Routes>
        <Route path="/review/:id" element={<ReviewPage />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('ReviewPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders task progress when review is started', async () => {
    renderWithRouter({ reviewId: 'test-id', sseUrl: '/api/pullcat/v1/sse/test-id' })

    await waitFor(() => {
      expect(screen.getByText('分析进度')).toBeInTheDocument()
    })

    expect(screen.getByText(/变更总结/)).toBeInTheDocument()
    expect(screen.getByText(/等待分析/)).toBeInTheDocument()
  })

  it('displays error message area in the DOM', () => {
    renderWithRouter({ reviewId: 'test-id', sseUrl: '/api/pullcat/v1/sse/test-id' })

    expect(screen.getByText('分析进度')).toBeInTheDocument()
  })

  it('does not render progress when no state is provided', () => {
    render(
      <MemoryRouter initialEntries={['/review/test-id']}>
        <Routes>
          <Route path="/review/:id" element={<ReviewPage />} />
        </Routes>
      </MemoryRouter>
    )

    const heading = screen.queryByText('分析进度')
    expect(heading).toBeNull()
  })
})
