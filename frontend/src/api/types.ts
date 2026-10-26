import type { components } from './schema'

type Schemas = components['schemas']

export type AuthResponse = Schemas['AuthResponse']
export type UserResponse = Schemas['UserResponse']
export type ErrorResponse = Schemas['ErrorResponse']
export type FieldError = Schemas['FieldError']
export type ErrorCode = NonNullable<ErrorResponse['code']>
export type Role = NonNullable<UserResponse['role']>
