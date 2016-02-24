#!/bin/bash
LOGBACK_CONFIGURATIONFILE=../../livijuku-env/livi/livisovt111l/filesys/etc/sysconfig/livijukubackend-logback.xml
JAVA_OPTS="-Djava.awt.headless=true -Dlogback.configurationFile=$LOGBACK_CONFIGURATIONFILE" lein ring server-headless 8082
