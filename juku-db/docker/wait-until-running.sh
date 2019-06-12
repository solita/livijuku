#!/usr/bin/env bash
set -e

fail () {
  echo 'Connection to oracle database failed.'
  exit 1
}
container=$1

execution_counter=1
echo "Database health check"
until [[ $(docker ps -q --filter "health=healthy" --filter "name=$container") ]]
do
  ((execution_counter++))
  ((execution_counter<20)) || fail
  sleep 10
  echo "Retry $execution_counter"
done
echo "Oracle database server health check successful"

source db.sh
execution_counter=1
echo "Database listener status check"
until db::exec $container "lsnrctl status" &> /dev/null;
do
  ((execution_counter++))
  ((execution_counter<20)) || fail
  sleep 10
  echo "Retry $execution_counter"
done
db::exec $container "lsnrctl status"