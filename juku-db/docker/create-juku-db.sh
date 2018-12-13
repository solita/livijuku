#!/usr/bin/env bash
set -e

BASEDIR=$(cd $(dirname $0); /bin/pwd)
cd $BASEDIR

source db.sh

# run database
docker run -d -it \
      -v $BASEDIR/..:/opt/juku/ \
      -p 1521:1521 \
      --name juku-db \
      container-registry.oracle.com/database/enterprise:12.2.0.1

./wait-until-running.sh

# print listener status - this prints available sids:
db::exec "lsnrctl status"

# configuration
db::sqlplus "docker/config.sql"

# create tablespaces
db::sqlplus_pdb orclpdb1 "dba/tablespace.sql"

# create users
db::sqlplus_pdb orclpdb1 "dba/users.sql"

# add wm_concat
db::sqlplus_pdb orclpdb1 "dba/wm_concat_shim.sql"
