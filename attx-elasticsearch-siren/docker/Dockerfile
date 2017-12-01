FROM openjdk:8-jre
MAINTAINER João M. da Silva <joao.silva@helsinki.fi>

# Need to update the package repo first
RUN apt-get -qq update

# grab gosu for easy step-down from root
ENV GOSU_VERSION 1.10
RUN set -x \
	&& wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture)" \
	&& wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture).asc" \
	&& export GNUPGHOME="$(mktemp -d)" \
	&& gpg --keyserver ipv4.pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
	&& gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
	&& rm -r "$GNUPGHOME" /usr/local/bin/gosu.asc \
	&& chmod +x /usr/local/bin/gosu \
	&& gosu nobody true

# Install Required Packages
# RUN DEBIAN_FRONTEND=noninteractive apt-get install -y -qq adduser \
#    openjdk-7-jre-headless

# Download Elasticsearch Package
ADD https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-1.3.4.deb /tmp/elasticsearch.deb

# Install the Elasticsearch Package
RUN  dpkg -i /tmp/elasticsearch.deb

# Set the clustername and Mountpoint
RUN sed -i 's/^\(\#cluster.name: elasticsearch\)$/\1\ncluster.name: elasticsearch/' /etc/elasticsearch/elasticsearch.yml && \
    sed -i 's/^\(\#path.data: \/path\/to\/data\)$/\1\npath.data: \/data/' /etc/elasticsearch/elasticsearch.yml && \
    sed -i -e '$a\script.disable_dynamic: true' /etc/elasticsearch/elasticsearch.yml && \
    sed -i 's/^\(\#MAX_OPEN_FILES=.\+\)$/\1\nMAX_OPEN_FILES=65535/' /etc/default/elasticsearch
#    ln -s /etc/elasticsearch /usr/share/elasticsearch/config

# Clean Up packages
RUN apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
    find /var/log -type f | while read f; do echo -ne '' > $f; done;

# Copy configs
COPY elasticsearch.yml /usr/share/elasticsearch/config/elasticsearch.yml
COPY logging.yml /usr/share/elasticsearch/config/logging.yml

ARG APIPlugin_version

# Copy custom plugins
COPY build/attx-api-plugin-${APIPlugin_version}.jar /tmp/attx-api-plugin.jar

RUN /usr/share/elasticsearch/bin/plugin install elasticsearch/elasticsearch-analysis-icu/2.3.0
RUN /usr/share/elasticsearch/bin/plugin install com.sindicetech.siren/siren-elasticsearch/1.4
RUN /usr/share/elasticsearch/bin/plugin --url file:///tmp/attx-api-plugin.jar --install attx-es-api

ENV PATH /usr/share/elasticsearch/bin:$PATH

WORKDIR /usr/share/elasticsearch

RUN set -ex \
	&& for path in \
		./data \
		./logs \
        ./config \
		./plugin \
        ./plugins \
	; do \
		mkdir -p "$path"; \
		chown -R elasticsearch:elasticsearch "$path"; \
	done

COPY docker-entrypoint.sh /

RUN mkdir -p /data \
    && chown -R elasticsearch:elasticsearch /tmp/attx-api-plugin.jar \
    && chown -R elasticsearch:elasticsearch /data \
    && chmod +x /docker-entrypoint.sh

# Exposed Volumes
VOLUME ["/data"]

# Expose ports
EXPOSE  9200 9300

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["elasticsearch"]
