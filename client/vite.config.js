import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import svgr from "vite-plugin-svgr";

// https://vite.dev/config/
export default defineConfig({
    plugins: [react(), svgr(
        {
            // svgr options: https://react-svgr.com/docs/options/
            svgrOptions: { exportType: "default", ref: true, svgo: false, titleProp: true },
            // include: "**/*.svg?react", // only use SVGR when you explicitly add ?react
            include: "**/*.svg",
        }
    )],
    test: {
        environment: 'jsdom',
        globals: true
    },
    build: {
        chunkSizeWarningLimit: 1000
    },
    server: {
        port: 3000,
        open: true,
        proxy: {
            '/api/v1': {
                target: 'http://localhost:8888',
                changeOrigin: false,
                secure: false
            },
            '/config': {
                target: 'http://localhost:8888',
                changeOrigin: false,
                secure: false
            }
        }

    },
    css: {
        preprocessorOptions: {
            scss: {
                // ToDo fix?
                silenceDeprecations: ["mixed-decls"],
            },
        },
    },
})
