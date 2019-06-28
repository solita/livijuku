#!/usr/bin/env bash

function db::exec() {
  container=$1
  docker exec -w /opt/juku/ -it $container bash -c "source /home/oracle/.bashrc; $2"
}

function db::sqlplus() {
  container=$1
  filename=$2
  db::exec $container "printf \"@$filename ${*:3}\nexit\n\" | sqlplus system/Oradoc_db1"
}

function db::sqlplus_pdb() {
  container=$1
  pdb=$2
  filename=$3
  db::exec $container "printf \"@docker/connect-pdb.sql $pdb\n@$filename ${*:4}\nexit\n\" | sqlplus system/Oradoc_db1"
}
