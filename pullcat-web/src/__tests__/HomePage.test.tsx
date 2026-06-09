/// <reference types="@testing-library/jest-dom" />
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { toast } from 'sonner'
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

  it('renders an input-first review workspace', () => {
    renderHomePage()
    expect(screen.getByRole('heading', { name: '新建审查', level: 1 })).toBeInTheDocument()
    expect(screen.getByText('贴入 PR，立即开始')).toBeInTheDocument()
    expect(screen.getByText('GitHub Pull Request')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('粘贴 GitHub PR 链接')).toBeInTheDocument()
  })

  it('renders compact review cues and submit button', () => {
    renderHomePage()
    expect(screen.getByText('会话参数')).toBeInTheDocument()
    expect(screen.getByText('来源')).toBeInTheDocument()
    expect(screen.getByText('GitHub PR')).toBeInTheDocument()
    expect(screen.getByText('创建')).toBeInTheDocument()
    expect(screen.getByText('即时')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /审查|review/i })).toBeInTheDocument()
  })

  it('validates invalid URL', () => {
    const toastSpy = vi.spyOn(toast, 'error')
    renderHomePage()
    const input = screen.getByPlaceholderText('粘贴 GitHub PR 链接')
    fireEvent.change(input, { target: { value: 'not-a-valid-url' } })
    fireEvent.click(screen.getByRole('button', { name: /审查|review/i }))
    expect(toastSpy).toHaveBeenCalledWith(expect.stringMatching(/无效|invalid|请输入/i))
  })
})
