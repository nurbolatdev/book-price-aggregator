import client from './client'

export const getFavorites = () =>
  client.get('/favorites').then((r) => r.data)

export const addFavorite = (bookId) =>
  client.post(`/favorites/${bookId}`).then((r) => r.data)

export const removeFavorite = (bookId) =>
  client.delete(`/favorites/${bookId}`)

export const getFavoriteStatus = (bookId) =>
  client.get(`/favorites/${bookId}/status`).then((r) => r.data)
