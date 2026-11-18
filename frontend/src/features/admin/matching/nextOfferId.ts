export function nextOfferId(list: readonly { id?: number }[], id: number): number | null {
  const index = list.findIndex((item) => item.id === id)
  if (index < 0) return list[0]?.id ?? null
  const neighbour = list[index + 1] ?? list[index - 1]
  return neighbour?.id ?? null
}
