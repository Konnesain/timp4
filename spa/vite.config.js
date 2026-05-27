import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const apiTarget = process.env.API_PROXY_TARGET || 'http://localhost:2001';

const csp = [
  "default-src 'self'",
  "script-src 'self'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data:",
  "connect-src 'self'",
  "font-src 'self'",
  "frame-ancestors none",
  "base-uri 'self'",
  "form-action 'self'",
].join('; ');

function cspPlugin() {
  return {
    name: 'csp-headers',
    configureServer(server) {
      server.middlewares.use((_req, res, next) => {
        res.setHeader('Content-Security-Policy', csp);
        next();
      });
    },
    configurePreviewServer(server) {
      server.middlewares.use((_req, res, next) => {
        res.setHeader('Content-Security-Policy', csp);
        next();
      });
    }
  };
}

export default defineConfig({
  plugins: [react(), cspPlugin()],
  server: {
    port: 2000,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      }
    }
  },
  preview: {
    port: 2000,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      }
    }
  }
})
