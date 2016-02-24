#!/bin/bash

CONNECTION="letto.solita.fi:1521/ldev.solita.fi"

DB_URL=$CONNECTION lein with-profiles +test-data do clear-db, update-db
echo dbmaintain RETURN VALUE: $?
