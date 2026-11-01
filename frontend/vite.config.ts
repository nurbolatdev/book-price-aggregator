/// <reference types="vitest/config" />
import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'

// Analyze mode only: emits a JSON description of every output chunk for scripts/check-chunks.mjs.
function chunkReport(): Plugin {
  return {
    name: 'chunk-report',
    apply: 'build',
    generateBundle(_options, bundle) {
      const chunks = Object.values(bundle).flatMap((item) =>
        item.type === 'chunk'
          ? [
              {
                fileName: item.fileName,
                isEntry: item.isEntry,
                isDynamicEntry: item.isDynamicEntry,
                facadeModuleId: item.facadeModuleId,
                imports: item.imports,
                dynamicImports: item.dynamicImports,
                moduleIds: item.moduleIds,
              },
            ]
          : [],
      )
      this.emitFile({ type: 'asset', fileName: 'chunk-report.json', source: JSON.stringify(chunks) })
    },
  }
}

export default defineConfig(({ mode }) => ({
  plugins: [react(), ...(mode === 'analyze' ? [chunkReport()] : [])],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
    css: false,
    restoreMocks: true,
    // Newer Node versions expose an experimental global localStorage that shadows jsdom's; disable it in test workers.
    pool: 'forks',
    poolOptions: { forks: { execArgv: ['--no-experimental-webstorage'] } },
  },
}))
