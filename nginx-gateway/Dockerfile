# Official nginx image auto-runs envsubst on every file under
# /etc/nginx/templates/*.template at container start, writing the result to
# /etc/nginx/conf.d/ (stripping the .template suffix) before starting nginx.
# This is how ${PORT}, ${CORE_UPSTREAM}, etc. get filled in from Render's
# environment variables without any custom entrypoint script.
FROM nginx:1.25-alpine

COPY templates/default.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 80
