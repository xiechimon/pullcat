/// <reference types="@testing-library/jest-dom" />
import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { LoginPage } from '../pages/LoginPage'

describe('LoginPage', () => {
  it('uses task-focused copy and primary action styling', () => {
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '登录 GitHub' })).toBeInTheDocument()
    expect(screen.queryByText('连接 GitHub，再继续审查')).not.toBeInTheDocument()

    expect(screen.getByRole('link', { name: '登录 GitHub' })).toHaveAttribute('href', '/oauth2/authorization/github')
    expect(screen.getByText('可用功能')).toBeInTheDocument()
    expect(screen.getByText('保存审查历史')).toBeInTheDocument()
    expect(screen.getByText('配置自动发布')).toBeInTheDocument()
  })
})
