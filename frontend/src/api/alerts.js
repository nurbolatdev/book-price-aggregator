import client from './client'

export const getAlerts = () =>
  client.get('/alerts').then((r) => r.data)

export const createAlert = (bookId, targetPrice) =>
  client.post(`/alerts/${bookId}`, { targetPrice }).then((r) => r.data)

export const deleteAlert = (bookId) =>
  client.delete(`/alerts/${bookId}`)
