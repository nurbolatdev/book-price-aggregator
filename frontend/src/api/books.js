import client from './client'

export const searchBooks = (query) =>
  client.get('/books/search', { params: { query } }).then((r) => r.data)

export const getBook = (id) =>
  client.get(`/books/${id}`).then((r) => r.data)

export const getBookOffers = (id) =>
  client.get(`/books/${id}/offers`).then((r) => r.data)

export const getBookPriceHistory = (id) =>
  client.get(`/books/${id}/price-history`).then((r) => r.data)
