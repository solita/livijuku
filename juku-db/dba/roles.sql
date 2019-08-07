create role livi_schema not identified;

grant alter session,
  alter system,
  create cluster,
  create database link,
  create indextype,
  create materialized view,
  create operator,
  create procedure,
  create sequence,
  create session,
  create synonym,
  create table,
  create trigger,
  create type,
  create view
to livi_schema
;

create role livi_application not identified;

grant create session to livi_application;