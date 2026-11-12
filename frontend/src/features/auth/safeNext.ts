export function safeNext(next: string | null): string {
  if (!next || !next.startsWith('/')) return '/'
  const second = next.charAt(1)
  if (second === '/' || second === '\\') return '/'
  // eslint-disable-next-line no-control-regex
  if (/[\u0000-\u001f\u007f]/.test(next)) return '/'
  return next
}
