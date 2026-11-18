import type { components, operations } from './schema'

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
export type CreateBeaconRequest = Schemas['CreateBeaconRequest']
export type UpdateBeaconRequest = Schemas['UpdateBeaconRequest']
export type UnsubscribeResponse = Schemas['UnsubscribeResponse']
export type BeaconStatus = NonNullable<BeaconResponse['status']>
export type ConditionType = CreateBeaconRequest['conditionType']

export type AdminStats = Schemas['AdminStatsResponse']
export type AdminHealth = Schemas['AdminHealthResponse']
export type AdminParser = Schemas['AdminParserResponse']
export type AdminActivityDay = Schemas['AdminActivityDay']
// The generated `details` type has `Record<string, never>` values, which no real payload fits.
export type AdminEvent = Omit<Schemas['AdminEventResponse'], 'details'> & {
  details?: Record<string, unknown>
}
export type AdminEventPage = Omit<Schemas['PageResponseAdminEventResponse'], 'content'> & {
  content?: AdminEvent[]
}
export type AdminUser = Schemas['AdminUserResponse']
export type AdminUserPage = Schemas['PageResponseAdminUserResponse']
export type AdminNotification = Schemas['AdminNotificationResponse']
export type AdminNotificationPage = Schemas['PageResponseAdminNotificationResponse']
export type ReviewOffer = Schemas['ReviewOfferResponse']
export type ReviewOfferPage = Schemas['PageResponseReviewOfferResponse']
export type QueueStats = Schemas['QueueStats']
export type RequeueResponse = Schemas['RequeueResponse']
export type SearchQueryStat = Schemas['SearchQueryStat']
export type StoreState = Schemas['StoreState']
export type ParserRunQueued = Schemas['ParserRunQueuedResponse']
export type AdminPeriod = '24h' | '7d' | '30d'
export type EventLevel = NonNullable<AdminEvent['level']>
type NotificationQuery = NonNullable<operations['list_1']['parameters']['query']>
export type NotificationStatus = NonNullable<NotificationQuery['status']>
export type NotificationDelivery = NonNullable<NotificationQuery['delivery']>
