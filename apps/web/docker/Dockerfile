# The web app as static files, served by nginx without root rights on port 8080.
# Built by release.yml from apps/web after :apps:web:wasmJsBrowserDistribution.
FROM docker.io/nginxinc/nginx-unprivileged:1.30.6-alpine@sha256:f5116ed4606f3eb015dd4a8e28cb9b351476dc0ad4bc6e9f93b19cec130cdf03

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY build/dist/wasmJs/productionExecutable/ /usr/share/nginx/html/

EXPOSE 8080
