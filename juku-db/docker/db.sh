#!/usr/bin/env bash

function db::exec() {
  docker exec -w /opt/juku/ -it juku-db bash -c "source /home/oracle/.bashrc; $1"
}

function db::sqlplus() {
  filename=$1
  db::exec "(echo @$filename; printf '\n'; echo exit) | sqlplus / as sysdba"
}

function db::sqlplus_pdb() {
  pdb=$1
  filename=$2
  db::exec "(echo @docker/connect-pdb.sql $pdb; printf '\n'; echo @$filename; printf '\n'; echo exit) | sqlplus / as sysdba"
}
