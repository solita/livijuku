
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

create user juku 
    identified by &1
    default tablespace juku_data 
    quota unlimited on juku_data 
    quota unlimited on juku_indx 
    account unlock 
;

create user juku_app 
    identified by &2
    account unlock 
;

grant livi_schema to juku;
grant livi_application to juku_app;

-- set default schema to juku application
create or replace trigger juku_app.set_default_schema
after logon on juku_app.schema
begin
  execute immediate 'alter session set current_schema = juku';
end;
/

