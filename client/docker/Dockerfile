FROM ghcr.io/openconext/openconext-basecontainers/apache2:latest
ADD ./dist/ /var/www/
RUN rm -rf /etc/apache2/sites-enabled/*.conf
# mod_substitute is used to rewrite root-relative links to /roles/ in responses
RUN a2enmod substitute
COPY ./docker/appconf.conf /etc/apache2/sites-enabled/

