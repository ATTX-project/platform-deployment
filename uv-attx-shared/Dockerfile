FROM tenforce/unified-views-shared

MAINTAINER João M. da Silva <joao.silva@helsinki.fi>


ENV UNIFIED_VIEWS_VERSION 2.3.0

ADD config.properties /config

VOLUME /unified-views/scripts
RUN chmod 777 /unified-views/scripts

RUN chmod +x /usr/local/bin/env-to-java-properties-file.sh 
ENTRYPOINT ["/usr/local/bin/env-to-java-properties-file.sh"]
# RUN /usr/local/bin/env-to-java-properties-file.sh
VOLUME /config
