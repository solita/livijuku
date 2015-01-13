#!/bin/bash

usage() { echo "Usage: $0 <DB_USER> <DB_PASSWORD>" 1>&2; exit 1; }

DB_USER=$1
DB_PASSWORD=$2

if [ -z $DB_USER ]; then usage; fi
if [ -z $DB_PASSWORD ]; then usage; fi

DB_URL="jdbc:oracle:thin:@$CONNECTION"

CONNECTION="(DESCRIPTION=(LOAD_BALANCE=ON)(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=livirac01n1l-vip)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=livirac01n2l-vip)(PORT=1521))(LOAD_BALANCE=yes)(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=JUKUTEST)))"

DB_URL=$DB_URL DB_USER=$DB_USER DB_PASSWORD=$DB_PASSWORD lein with-profiles +test-data do clear-db, update-db
echo dbmaintain RETURN VALUE: $?

echo -e "\n==============================================================================="
echo "Tarkistetaan tietokannasta tuliko käännösvirheitä"
echo -e "===============================================================================\n"
echo "select * from user_errors; exit /" | sqlplus -L "$DB_USER/$DB_PASSWORD@$CONNECTION"
echo sqlplus RETURN VALUE: $?
