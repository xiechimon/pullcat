/// <reference types="@testing-library/jest-dom" />
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { HomePage } from '../pages/HomePage'

function renderHomePage() {
  return render(
    <MemoryRouter>
      <HomePage />
    </MemoryRouter>
  )
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders PR URL input', () => {
    renderHomePage()
    expect(screen.getByPlaceholderText(/github\.com.*pull/)).toBeInTheDocument()
  })

  it('renders submit button', () => {
    renderHomePage()
    expect(screen.getByRole('button', { name: /审查|review/i })).toBeInTheDocument()
  })

  it('validates invalid URL', async () => {
    renderHomePage()
    const input = screen.getByPlaceholderText(/github\.com.*pull/)
    fireEvent.change(input, { target: { value: 'not-a-valid-url' } })
    fireEvent.click(screen.getByRole('button', { name: /审查|review/i }))

    await waitFor(() => {
      expect(screen.queryByText(/无效|invalid|请输入/)).toBeInTheDocument()
    })
  })
})
