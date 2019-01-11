-- Define tablespace datafiles explicitly
-- This does not require that we are using Oracle Managed Files

define DATAFILE_PATH = '/home/oracle/app/oracle/oradata/orcl'

create tablespace juku_data datafile 
  '&DATAFILE_PATH/juku_data01.dbf' size 40 m reuse autoextend on next 1 m maxsize 2G
;

create tablespace juku_indx datafile 
  '&DATAFILE_PATH/juku_indx01.dbf' size 40 m reuse autoextend on next 1 m maxsize 2G
;