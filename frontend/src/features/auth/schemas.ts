import { z } from 'zod'

export const PASSWORD_HINT = 'Минимум 8 символов'

const email = z
  .string()
  .trim()
  .min(1, { error: 'Введите email' })
  .pipe(z.email({ error: 'Проверьте email: нужен адрес вида name@example.com' }))

const newPassword = z
  .string()
  .min(8, { error: 'Минимум 8 символов' })
  .max(100, { error: 'Не больше 100 символов' })

const name = z
  .string()
  .trim()
  .min(1, { error: 'Введите имя' })
  .max(100, { error: 'Не больше 100 символов' })

export const loginSchema = z.object({
  email,
  password: z.string().min(1, { error: 'Введите пароль' }),
})

export const registerSchema = z.object({
  name,
  email,
  password: newPassword,
  consent: z.boolean(),
})

export const emailSchema = z.object({ email })

export const resetPasswordSchema = z
  .object({
    newPassword,
    confirmPassword: z.string(),
  })
  .refine((value) => value.newPassword === value.confirmPassword, {
    error: 'Пароли не совпадают',
    path: ['confirmPassword'],
  })

export const profileNameSchema = z.object({ name })

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, { error: 'Введите текущий пароль' }),
  newPassword,
})

export const deleteAccountSchema = z.object({
  password: z.string().min(1, { error: 'Введите пароль' }),
})
