import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react'
import svgr from "vite-plugin-svgr";
import tailwindcss from "@tailwindcss/vite";

// The server redirects to /roles after login, but the Vite dev server only serves the base URL with a trailing slash
const redirectToBase = () => ({
    name: "redirect-to-base",
    apply: "serve",
    configureServer(server) {
        server.middlewares.use((req, res, next) => {
            if (/^\/roles(\?|$)/.test(req.url)) {
                res.writeHead(302, {Location: req.url.replace("/roles", "/roles/")});
                res.end();
            } else {
                next();
            }
        });
    }
});

// https://vite.dev/config/
export default defineConfig({
    base: "/roles/",
    resolve: {
        preserveSymlinks: true,
    },
    plugins: [redirectToBase(), react(), tailwindcss(), svgr(
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
            '/roles/api/v1': {
                target: 'http://localhost:8888',
                rewrite: path => path.replace(/^\/roles/, ''),
                changeOrigin: false,
                secure: false
            },
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
                silenceDeprecations: ['legacy-js-api'],
            },
        },
    },
})
