#!/usr/bin/env bash
set -e

cd $(dirname $0)
BASEDIR=$(/bin/pwd)
juku_db=${1:-"juku-db"}
db_port=${2:-"1521"}
epg_port=${3:-"50000"}

source db.sh

# run database
docker run -d -it \
      -v $BASEDIR/..:/opt/juku/ \
      -p 127.0.0.1:1521:1521 \
      -p 127.0.0.1:50000:50000 \
      --name $juku_db \
      container-registry.oracle.com/database/enterprise:12.2.0.1

./wait-until-running.sh $juku_db

# configuration
db::sqlplus $juku_db "docker/config.sql"

# create tablespaces
db::sqlplus_pdb $juku_db orclpdb1 "dba/tablespace.sql"

# create roles
db::sqlplus_pdb $juku_db orclpdb1 "dba/roles.sql"

# create users
db::sqlplus_pdb $juku_db orclpdb1 "dba/users.sql" juku juku

# add wm_concat
db::sqlplus_pdb $juku_db orclpdb1 "dba/wm_concat_shim.sql"

# enable epg
db::sqlplus_pdb $juku_db orclpdb1 "dba/enable-epg.sql"

# enable flashback for development environment
db::sqlplus_pdb $juku_db orclpdb1 "dba/flashback.sql"

# check epg status
db::sqlplus_pdb $juku_db orclpdb1 '$ORACLE_HOME/rdbms/admin/epgstat.sql'
