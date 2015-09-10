#!/bin/bash
lein do clean, uberjar
JAVA_OPTS='-Djava.awt.headless=true' lein ring server-headless 8082
