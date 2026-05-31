/// <reference types="@testing-library/jest-dom" />
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { IssueCard } from '../components/IssueCard'

function renderIssueCard(overrides: Record<string, unknown> = {}) {
  const defaults = {
    id: 'issue-1',
    severity: 'HIGH' as const,
    file: 'src/Foo.java',
    line: 42,
    title: 'NPE risk',
    description: 'Potential null pointer dereference',
    suggestion: 'Add null check before dereferencing',
    suggestionCode: null,
    confidence: 0.9,
    selected: true,
    onToggle: vi.fn(),
    ...overrides,
  }

  return render(
    <IssueCard
      id={defaults.id as string}
      severity={defaults.severity as 'HIGH'}
      file={defaults.file as string | null}
      line={defaults.line as number | null}
      title={defaults.title as string}
      description={defaults.description as string}
      suggestion={defaults.suggestion as string}
      suggestionCode={defaults.suggestionCode as string | null}
      confidence={defaults.confidence as number}
      selected={defaults.selected as boolean}
      onToggle={defaults.onToggle as (id: string) => void}
    />
  )
}

describe('IssueCard', () => {
  it('renders issue title and description', () => {
    renderIssueCard()

    expect(screen.getByText('NPE risk')).toBeInTheDocument()
    expect(screen.getByText('Potential null pointer dereference')).toBeInTheDocument()
  })

  it('renders severity badge', () => {
    renderIssueCard()

    expect(screen.getByText('高')).toBeInTheDocument()
  })

  it('renders file location', () => {
    renderIssueCard()

    expect(screen.getByText('src/Foo.java:42')).toBeInTheDocument()
  })

  it('renders suggestion text', () => {
    renderIssueCard()

    expect(screen.getByText('建议：')).toBeInTheDocument()
    expect(screen.getByText('Add null check before dereferencing')).toBeInTheDocument()
  })

  it('renders suggestionCode as code block when present', () => {
    renderIssueCard({
      suggestionCode: 'if (user != null) {\n  return user.getName();\n}\nreturn "unknown";',
    })

    expect(screen.getByText('AI 生成的修复建议，请人工确认后应用')).toBeInTheDocument()
    const codeEl = document.querySelector('code')
    expect(codeEl).not.toBeNull()
    expect(codeEl!.textContent).toContain('if (user != null)')
    expect(codeEl!.textContent).toContain('return user.getName()')
  })

  it('does not render suggestionCode section when null', () => {
    renderIssueCard({ suggestionCode: null })

    expect(screen.queryByText('AI 生成的修复建议，请人工确认后应用')).toBeNull()
  })

  it('renders confidence percentage', () => {
    renderIssueCard({ confidence: 0.85 })

    expect(screen.getByText('置信度: 85%')).toBeInTheDocument()
  })

  it('calls onToggle when checkbox is clicked', () => {
    const onToggle = vi.fn()
    renderIssueCard({ onToggle })

    const checkbox = screen.getByRole('checkbox')
    checkbox.click()

    expect(onToggle).toHaveBeenCalledWith('issue-1')
  })
})
