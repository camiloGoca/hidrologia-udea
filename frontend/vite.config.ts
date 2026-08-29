import { fileURLToPath, URL } from 'node:url'

import { defineConfig, type PluginOption } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import vueDevTools from 'vite-plugin-vue-devtools'

const serveOnly = (plugin: PluginOption): PluginOption => {
  if (Array.isArray(plugin)) {
    return plugin.map(serveOnly)
  }

  if (plugin && typeof plugin === 'object' && 'name' in plugin) {
    return { ...plugin, apply: 'serve' }
  }

  return plugin
}

// https://vite.dev/config/
export default defineConfig({
  envDir: fileURLToPath(new URL('..', import.meta.url)),
  plugins: [vue(), tailwindcss(), serveOnly(vueDevTools())],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
