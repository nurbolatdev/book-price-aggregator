import { createRef } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PasswordInput } from './PasswordInput'

describe('PasswordInput', () => {
  it('toggles visibility and keeps focus in the input', () => {
    render(<PasswordInput aria-label="Пароль" />)
    const input = screen.getByLabelText('Пароль')
    expect(input).toHaveAttribute('type', 'password')
    const toggle = screen.getByRole('button', { name: 'Показать пароль' })
    expect(toggle).toHaveAttribute('aria-pressed', 'false')
    fireEvent.click(toggle)
    expect(input).toHaveAttribute('type', 'text')
    expect(screen.getByRole('button', { name: 'Скрыть пароль' })).toHaveAttribute('aria-pressed', 'true')
    expect(input).toHaveFocus()
  })

  it('forwards autoComplete and ref', () => {
    const ref = createRef<HTMLInputElement>()
    render(<PasswordInput ref={ref} aria-label="Пароль" autoComplete="new-password" />)
    expect(screen.getByLabelText('Пароль')).toHaveAttribute('autocomplete', 'new-password')
    expect(ref.current).toBe(screen.getByLabelText('Пароль'))
  })
})
