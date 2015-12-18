#!/bin/bash
JAVA_OPTS='-Djava.awt.headless=true' lein do clean, ring server-headless 8082
