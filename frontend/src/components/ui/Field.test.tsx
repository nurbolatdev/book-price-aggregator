import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Field } from './Field'
import { Input } from './Input'

describe('Field', () => {
  it('associates label, hint and error with the input', () => {
    render(
      <Field label="Email" hint="Рабочий адрес" error="Неверный email">
        {({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} aria-invalid={invalid} />}
      </Field>,
    )
    const input = screen.getByLabelText('Email')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    const ids = (input.getAttribute('aria-describedby') ?? '').split(' ')
    expect(ids).toHaveLength(2)
    expect(document.getElementById(ids[0] ?? '')).toHaveTextContent('Рабочий адрес')
    expect(document.getElementById(ids[1] ?? '')).toHaveTextContent('Неверный email')
    expect(screen.getByText('Неверный email')).toBeInTheDocument()
  })

  it('is valid and undescribed without hint and error', () => {
    render(<Field label="Имя">{({ id, describedBy, invalid }) => <Input id={id} aria-describedby={describedBy} aria-invalid={invalid} />}</Field>)
    const input = screen.getByLabelText('Имя')
    expect(input).not.toHaveAttribute('aria-describedby')
    expect(input).toHaveAttribute('aria-invalid', 'false')
  })
})
