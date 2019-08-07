
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

