#!/bin/bash

lein with-profiles +test-data uberjar
export DB_URL=localhost:1521/orcl
export DB_USER=juku
export DB_PASSWORD=juku
java -jar target/juku-db.jar clear-db
java -jar target/juku-db.jar update-db
