const COVER_CLASSES = [
  'bg-cover-0',
  'bg-cover-1',
  'bg-cover-2',
  'bg-cover-3',
  'bg-cover-4',
  'bg-cover-5',
] as const

export function coverColor(editionId: number): string {
  return COVER_CLASSES[Math.abs(editionId) % COVER_CLASSES.length] ?? COVER_CLASSES[0]
}
