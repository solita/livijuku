#!/usr/bin/env bash

function db::exec() {
  container=$1
  docker exec -w /opt/juku/ -it $container bash -c "source /home/oracle/.bashrc; $2"
}

function db::sqlplus() {
  container=$1
  filename=$2
  db::exec $container "(echo @$filename; printf '\n'; echo exit) | sqlplus system/Oradoc_db1"
}

function db::sqlplus_pdb() {
  container=$1
  pdb=$2
  filename=$3
  db::exec $container "(echo @docker/connect-pdb.sql $pdb; printf '\n'; echo @$filename; printf '\n'; echo exit) | sqlplus system/Oradoc_db1"
}
