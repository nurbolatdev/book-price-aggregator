import type { components } from './schema'

type Schemas = components['schemas']

export type AuthResponse = Schemas['AuthResponse']
export type UserResponse = Schemas['UserResponse']
export type ErrorResponse = Schemas['ErrorResponse']
export type FieldError = Schemas['FieldError']
export type ErrorCode = NonNullable<ErrorResponse['code']>
export type Role = NonNullable<UserResponse['role']>
export type SearchItem = Schemas['SearchItem']
export type SearchPage = Schemas['PageResponseSearchItem']
export type SearchFacets = Schemas['SearchFacets']
export type StoreFacet = Schemas['StoreFacet']
export type ValueFacet = Schemas['ValueFacet']
export type DropItem = Schemas['DropItem']
export type StoreResponse = Schemas['StoreResponse']
export type StoreCode = NonNullable<StoreResponse['code']>
export type Binding = NonNullable<SearchItem['binding']>
export type Suggestion = Schemas['Suggestion']
export type EditionDetailResponse = Schemas['EditionDetailResponse']
export type OfferChip = Schemas['OfferChip']
export type BestPrice = Schemas['BestPrice']
