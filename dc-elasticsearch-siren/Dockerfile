FROM openjdk:8-jre
MAINTAINER João M. da Silva <joao.silva@helsinki.fi>

# Need to update the package repo first
RUN apt-get -qq update

# Install Required Packages
# RUN DEBIAN_FRONTEND=noninteractive apt-get install -y -qq adduser \
#    openjdk-7-jre-headless

# Download Elasticsearch Package
ADD  https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-1.3.4.deb /tmp/elasticsearch.deb

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

ENV APIPlugin_version 1.0-SNAPSHOT

# Copy custom plugins
COPY build/attx-api-plugin-${APIPlugin_version}.jar /tmp/attx-api-plugin.jar


# Exposed Volumes
VOLUME ["/data"]

# Expose ports
EXPOSE  9200 9300

RUN /usr/share/elasticsearch/bin/plugin install elasticsearch/elasticsearch-analysis-icu/2.3.0
RUN /usr/share/elasticsearch/bin/plugin install com.sindicetech.siren/siren-elasticsearch/1.4
RUN /usr/share/elasticsearch/bin/plugin --url file:///tmp/attx-api-plugin.jar --install attx-es-api

CMD ["/usr/share/elasticsearch/bin/elasticsearch"]
