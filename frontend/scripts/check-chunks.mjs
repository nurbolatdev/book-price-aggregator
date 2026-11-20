// Fails if the charting library ends up in the initial chunks (entry + edition page chunk),
// or if admin code ends up in a chunk reachable without the admin entry.
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

const ADMIN = /\/src\/(pages|features)\/admin\/|\/src\/api\/admin\.ts$|node_modules\/@tanstack\/(react-)?virtual/
// The facade is null once another chunk imports a module hoisted into this one, so match by module.
const adminChunk = chunks.find((c) => c.moduleIds.some((id) => id.endsWith('/src/pages/admin/AdminRoutes.tsx')))
if (!adminChunk) fail('admin chunk not found, the check would be vacuous')

const userRoots = [
  ...chunks.filter((c) => c.isEntry),
  ...chunks.filter((c) => c.isDynamicEntry && c !== adminChunk && !(c.facadeModuleId && ADMIN.test(c.facadeModuleId))),
]
const userReach = new Set()
const walk = (name) => {
  if (userReach.has(name)) return
  const chunk = byName.get(name)
  if (!chunk) return
  userReach.add(name)
  chunk.imports.forEach(walk)
}
userRoots.forEach((c) => walk(c.fileName))

const leaks = []
for (const name of userReach) {
  const bad = byName.get(name).moduleIds.find((id) => ADMIN.test(id))
  if (bad) leaks.push(`${name} contains ${bad}`)
}
if (leaks.length > 0) fail(`admin code is in user chunks:\n  ${leaks.join('\n  ')}`)

console.log('Initial chunks (no Recharts):')
for (const name of [...initial].sort()) console.log(`  ${name}`)
console.log(`Recharts lives in: ${holders.map((c) => c.fileName).join(', ')}`)
console.log(`Admin code lives in: ${adminChunk.fileName}`)
