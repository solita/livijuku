
-- setting http port
SELECT DBMS_XDB.gethttpport FROM dual;

-- palvelua varten varattu portti letolla:
begin
  DBMS_XDB.SETHTTPPORT(50000);
end;
/

create user juku_users identified by juku account lock;
grant create user to juku_users;
grant drop user to juku_users;
grant livi_schema to juku_users with admin option;
grant livi_application to juku_users with admin option;
grant create any trigger to juku_users;

-- test package
create or replace package juku_users.testing authid definer as
  procedure create_users (username varchar2);
  procedure drop_users (username varchar2);
end;
/

create or replace package body juku_users.testing as
  user_allready_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT (user_allready_exists, -01920);
  
  procedure run(s varchar2) is
    dryrun constant boolean := false;
  begin
    if dryrun = false
    then
      begin
         execute immediate s;
      exception when others then
         dbms_output.put_line('FAILED: '|| s);
         raise;
      end;
    else dbms_output.put_line(s ||';'|| chr(10));
    end if;
  end;

  procedure create_users (username varchar2) is
    juku constant varchar2(30 char) := 'juku_' || username;
    juku_app constant varchar2(30 char) := juku || '_app';
    create_juku constant varchar2(1000 char) :=
      'create user ' || juku || ' identified by juku default tablespace juku_data quota unlimited on juku_data quota unlimited on juku_indx account unlock';
    create_juku_app constant varchar2(1000 char) :=
      'create user ' || juku_app || ' identified by juku account unlock';
  begin
    run(create_juku);
    run(create_juku_app);
    run('grant livi_schema to ' || juku);
    run('grant livi_application to ' || juku_app);
    
    run('create or replace trigger ' || juku_app || '.set_default_schema ' ||
        'after logon on juku_app.schema ' ||
        'begin execute immediate ''alter session set current_schema = ' || juku || '''; end;');
  exception
    when user_allready_exists then return;
  end;
  
  procedure drop_users (username varchar2) is
    juku constant varchar2(30 char) := 'juku_' || username;
    juku_app constant varchar2(30 char) := juku || '_app';
    
  begin
    run('drop user ' || juku || ' cascade');
    run('drop user ' || juku_app || ' cascade');
  end;
  
end;
/

grant execute on juku_users.testing to juku;

begin
  dbms_epg.create_dad (
    dad_name => 'juku_admin_service',
    path     => '/juku/*');
    
  dbms_epg.authorize_dad (
    dad_name => 'juku_admin_service',
    user     => 'JUKU_USERS');
end;
/


-- Usage:
-- http://juku:juku@letto.solita.fi:50000/juku/juku_users.testing.create_users?username=test
-- http://juku:juku@letto.solita.fi:50000/juku/juku_users.testing.drop_users?username=test


-- Testing --

begin
  juku_users.testing.create_users('test');
end;
/
begin
  juku_users.testing.drop_users('test');
end;
/

-- Cleanup
/*
begin
  dbms_epg.drop_dad ('juku_admin_service'); -- Cleanup the DAD.
end;
/

drop user juku_users cascade;
*/


