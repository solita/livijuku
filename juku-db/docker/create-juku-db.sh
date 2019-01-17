#!/usr/bin/env bash
set -e

BASEDIR=$(cd $(dirname $0); /bin/pwd)
cd $BASEDIR

source db.sh

# run database
docker run -d -it \
      -v $BASEDIR/..:/opt/juku/ \
      -p 1521:1521 \
      -p 50000:50000 \
      --name juku-db \
      container-registry.oracle.com/database/enterprise:12.2.0.1

./wait-until-running.sh

# configuration
db::sqlplus "docker/config.sql"

# create tablespaces
db::sqlplus_pdb orclpdb1 "dba/tablespace.sql"

# create users
db::sqlplus_pdb orclpdb1 "dba/users.sql"

# add wm_concat
db::sqlplus_pdb orclpdb1 "dba/wm_concat_shim.sql"

# enable epg
db::sqlplus_pdb orclpdb1 "dba/enable-epg.sql"

# check epg status
db::sqlplus_pdb orclpdb1 '$ORACLE_HOME/rdbms/admin/epgstat.sql'
