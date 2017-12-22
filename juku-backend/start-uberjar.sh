#!/bin/bash

# start the backend server in a similar way as in production using development settings
# the backend web server is same as in production: httpkit

lein do clean, uberjar
JAVA_OPTS='-Djava.awt.headless=true' java -jar target/juku.jar
