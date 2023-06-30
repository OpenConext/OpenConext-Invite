FROM httpd:2.4
ADD ./build/ /var/www/
COPY ./docker/httpd.conf /usr/local/apache2/conf/httpd.conf
COPY ./docker/appconf.conf /usr/local/apache2/conf/extra/

