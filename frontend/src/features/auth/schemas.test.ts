import { describe, expect, it } from 'vitest'
import type { ZodType } from 'zod'
import {
  changePasswordSchema,
  deleteAccountSchema,
  emailSchema,
  loginSchema,
  profileNameSchema,
  registerSchema,
  resetPasswordSchema,
} from './schemas'

function messages(schema: ZodType, input: unknown): Record<string, string> {
  const result = schema.safeParse(input)
  if (result.success) return {}
  return Object.fromEntries(result.error.issues.map((i) => [String(i.path[0]), i.message]))
}

describe('auth schemas', () => {
  it('email: required and format', () => {
    expect(messages(emailSchema, { email: '' }).email).toBe('Введите email')
    expect(messages(emailSchema, { email: 'abc' }).email).toBe(
      'Проверьте email: нужен адрес вида name@example.com',
    )
    expect(emailSchema.parse({ email: '  a@b.kz ' }).email).toBe('a@b.kz')
  })

  it('login accepts a short password but not an empty one', () => {
    expect(loginSchema.safeParse({ email: 'a@b.kz', password: 'abc' }).success).toBe(true)
    expect(messages(loginSchema, { email: 'a@b.kz', password: '' }).password).toBe('Введите пароль')
  })

  it('register: valid input passes, password and name rules apply', () => {
    const valid = { name: 'Нурбо', email: 'a@b.kz', password: '12345678', consent: true }
    expect(registerSchema.safeParse(valid).success).toBe(true)
    expect(messages(registerSchema, { ...valid, password: '1234567' }).password).toBe('Минимум 8 символов')
    expect(messages(registerSchema, { ...valid, password: 'x'.repeat(101) }).password).toBe(
      'Не больше 100 символов',
    )
    expect(messages(registerSchema, { ...valid, name: '   ' }).name).toBe('Введите имя')
  })

  it('reset: mismatch is reported on confirmPassword', () => {
    expect(resetPasswordSchema.safeParse({ newPassword: '12345678', confirmPassword: '12345678' }).success).toBe(
      true,
    )
    expect(messages(resetPasswordSchema, { newPassword: '12345678', confirmPassword: '1234567x' })).toEqual({
      confirmPassword: 'Пароли не совпадают',
    })
  })

  it('profile name, change password and delete account', () => {
    expect(messages(profileNameSchema, { name: '  ' }).name).toBe('Введите имя')
    expect(changePasswordSchema.safeParse({ currentPassword: 'a', newPassword: '12345678' }).success).toBe(true)
    expect(messages(changePasswordSchema, { currentPassword: '', newPassword: '1' })).toEqual({
      currentPassword: 'Введите текущий пароль',
      newPassword: 'Минимум 8 символов',
    })
    expect(messages(deleteAccountSchema, { password: '' }).password).toBe('Введите пароль')
  })
})
