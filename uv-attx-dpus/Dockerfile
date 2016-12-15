FROM debian:jessie

MAINTAINER João M. da Silva <joao.silva@helsinki.fi> 
ADD dpus /dpus
VOLUME /dpus
ENV MASTER_USER=master MASTER_PASSWORD=commander
RUN apt-get update && apt-get install -y curl
ADD startup.sh add-dpu.sh /usr/local/bin/
CMD ["/bin/bash", "/usr/local/bin/startup.sh"]

