import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Button, buttonClasses } from './Button'
import { IconButton } from './IconButton'

describe('Button', () => {
  it('ignores clicks while loading', () => {
    const onClick = vi.fn()
    render(
      <Button loading onClick={onClick}>
        Сохранить
      </Button>,
    )
    const btn = screen.getByRole('button', { name: 'Сохранить' })
    expect(btn).toBeDisabled()
    expect(btn).toHaveAttribute('aria-busy', 'true')
    fireEvent.click(btn)
    fireEvent.click(btn)
    expect(onClick).not.toHaveBeenCalled()
  })

  it('calls onClick when idle', () => {
    const onClick = vi.fn()
    render(<Button onClick={onClick}>Ок</Button>)
    fireEvent.click(screen.getByRole('button', { name: 'Ок' }))
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('maps variants to distinct classes', () => {
    const all = (['primary', 'secondary', 'light', 'dark', 'ghost'] as const).map((variant) => buttonClasses({ variant }))
    expect(new Set(all).size).toBe(5)
  })

  it('light variant has no border', () => {
    expect(buttonClasses({ variant: 'light' })).not.toContain('border')
  })

  it('outline-accent variant has an accent border and text', () => {
    const cls = buttonClasses({ variant: 'outline-accent' })
    expect(cls).toContain('border-accent')
    expect(cls).toContain('text-accent')
  })

  it('bar size is 52px tall and bold; other sizes stay semibold', () => {
    const bar = buttonClasses({ size: 'bar' })
    expect(bar).toContain('h-[52px]')
    expect(bar).toContain('font-bold')
    expect(bar).not.toContain('font-semibold')
    for (const size of ['sm', 'md', 'lg', 'xl'] as const) {
      const cls = buttonClasses({ size })
      expect(cls).toContain('font-semibold')
      expect(cls).not.toContain('font-bold')
    }
  })

  it('buttonClasses matches the component classes', () => {
    render(
      <Button variant="dark" size="lg">
        X
      </Button>,
    )
    expect(screen.getByRole('button').className).toBe(buttonClasses({ variant: 'dark', size: 'lg' }))
  })
})

describe('IconButton', () => {
  it('is found by its label', () => {
    render(<IconButton label="Основное меню">x</IconButton>)
    expect(screen.getByRole('button', { name: 'Основное меню' })).toBeInTheDocument()
  })
})
