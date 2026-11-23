# The web app as static files, served by nginx without root rights on port 8080.
# Built by release.yml from apps/web after :apps:web:wasmJsBrowserDistribution.
FROM docker.io/nginxinc/nginx-unprivileged:1.31.7-alpine@sha256:b5047b9b76845eac124358373c6852377c52e4b05834a47647e955dd91cb8f1e

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY build/dist/wasmJs/productionExecutable/ /usr/share/nginx/html/

EXPOSE 8080
