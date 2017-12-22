#!/bin/bash

# start backend server using leiningen ring with development settings
# the backend web server is jetty

JAVA_OPTS='-Djava.awt.headless=true' lein do clean, ring server-headless 8082
