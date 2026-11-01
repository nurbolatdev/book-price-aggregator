// Fails if the charting library ends up in the initial chunks (entry + edition page chunk).
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const chunks = JSON.parse(readFileSync(resolve(root, 'dist/chunk-report.json'), 'utf8'))
const byName = new Map(chunks.map((c) => [c.fileName, c]))
const FORBIDDEN = /node_modules\/(recharts|victory-vendor|d3-[^/]+)\//

const fail = (message) => {
  console.error(`check-chunks: ${message}`)
  process.exit(1)
}

const holders = chunks.filter((c) => c.moduleIds.some((id) => /node_modules\/recharts\//.test(id)))
if (holders.length === 0) fail('no chunk contains recharts, the check would be vacuous')

const pageChunk = chunks.find((c) => c.facadeModuleId?.endsWith('/src/pages/EditionPage.tsx'))
if (!pageChunk) fail('edition page chunk not found')

const roots = [...chunks.filter((c) => c.isEntry), pageChunk]
const initial = new Set()
const visit = (name) => {
  if (initial.has(name)) return
  const chunk = byName.get(name)
  if (!chunk) return
  initial.add(name)
  chunk.imports.forEach(visit)
}
roots.forEach((c) => visit(c.fileName))

const offenders = []
for (const name of initial) {
  const bad = byName.get(name).moduleIds.find((id) => FORBIDDEN.test(id))
  if (bad) offenders.push(`${name} contains ${bad}`)
}
if (offenders.length > 0) fail(`Recharts is in the initial chunks:\n  ${offenders.join('\n  ')}`)

console.log('Initial chunks (no Recharts):')
for (const name of [...initial].sort()) console.log(`  ${name}`)
console.log(`Recharts lives in: ${holders.map((c) => c.fileName).join(', ')}`)
