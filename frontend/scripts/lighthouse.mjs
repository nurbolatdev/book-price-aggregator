// Runs Lighthouse (mobile defaults) against a running frontend and prints the scores.
// Usage: npm run lighthouse -- [baseUrl]   (default http://localhost)
// Lighthouse is fetched at run time with npx and is not a project dependency.
import { spawnSync } from 'node:child_process'
import { mkdirSync, readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from '@playwright/test'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const base = (process.argv[2] ?? 'http://localhost').replace(/\/$/, '')
const outDir = join(root, 'lighthouse')
const THRESHOLD = 90
const QUERY = 'Мастер и Маргарита'

async function findEditionId() {
  try {
    const res = await fetch(`${base}/api/v1/search?q=${encodeURIComponent(QUERY)}&size=1`)
    if (res.ok) {
      const body = await res.json()
      const id = body?.items?.[0]?.editionId ?? body?.content?.[0]?.editionId
      if (id) return String(id)
    }
  } catch {
    // fall through to the mock fixture id
  }
  return '41'
}

const editionId = await findEditionId()
const pages = [
  { name: 'home', path: '/' },
  { name: 'search', path: `/search?q=${encodeURIComponent(QUERY)}` },
  { name: 'edition', path: `/editions/${editionId}` },
]

mkdirSync(outDir, { recursive: true })
const chromePath = chromium.executablePath()
const rows = []
let failed = false

for (const page of pages) {
  const outBase = join(outDir, `${page.name}.report`)
  const run = spawnSync(
    'npx',
    [
      '-y',
      'lighthouse@12',
      `${base}${page.path}`,
      '--only-categories=performance,accessibility,best-practices,seo',
      '--chrome-flags=--headless=new',
      '--output=html',
      '--output=json',
      `--output-path=${outBase}`,
      '--quiet',
    ],
    { env: { ...process.env, CHROME_PATH: chromePath }, stdio: ['ignore', 'inherit', 'inherit'] },
  )
  if (run.status !== 0) {
    console.error(`Lighthouse failed for ${page.path} (exit ${run.status})`)
    process.exit(1)
  }
  const report = JSON.parse(readFileSync(`${outBase}.report.json`, 'utf8'))
  const score = (key) => Math.round((report.categories[key]?.score ?? 0) * 100)
  const row = {
    page: page.name,
    performance: score('performance'),
    accessibility: score('accessibility'),
    'best-practices': score('best-practices'),
    seo: score('seo'),
    LCP: report.audits['largest-contentful-paint']?.displayValue,
    CLS: report.audits['cumulative-layout-shift']?.displayValue,
  }
  if ([row.performance, row.accessibility, row['best-practices'], row.seo].some((s) => s < THRESHOLD)) {
    failed = true
  }
  rows.push(row)
}

console.table(rows)
if (failed) {
  console.error(`Some scores are below ${THRESHOLD}`)
  process.exit(1)
}
