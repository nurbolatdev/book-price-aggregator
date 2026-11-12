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
export type BestOffer = Schemas['BestOffer']
export type OfferInfo = Schemas['OfferInfo']
export type OtherEdition = Schemas['OtherEdition']
export type PriceVerdict = Schemas['PriceVerdict']
export type PriceHistoryResponse = Schemas['PriceHistoryResponse']
export type PricePoint = Schemas['Point']
export type PricePeriod = NonNullable<PriceHistoryResponse['period']>
export type SimilarEdition = Schemas['SimilarEdition']
export type BookResponse = Schemas['BookResponse']
export type EditionResponse = Schemas['EditionResponse']
export type BeaconResponse = Schemas['BeaconResponse']
export type RegisterRequest = Schemas['RegisterRequest']
export type RegisterResponse = Schemas['RegisterResponse']
export type UpdateProfileRequest = Schemas['UpdateProfileRequest']
export type NotifyFrequency = NonNullable<UserResponse['notifyFrequency']>
