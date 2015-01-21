-- test data management package
create or replace package testing authid current_user as
  procedure create_restorepoint (restorepoint varchar2);
  procedure revert_to (restorepoint varchar2);
end;
/

create or replace package body testing as
  restorepoint_already_exists EXCEPTION;
  PRAGMA EXCEPTION_INIT (restorepoint_already_exists, -38778);
  
  procedure run(s varchar2) is
  begin
    model.putline_or_execute(s);
  end;

  procedure create_restorepoint (restorepoint varchar2) is
  begin
    run('create restore point ' || user || '_' || restorepoint);
  exception
    when restorepoint_already_exists then 
    begin
      run('drop restore point ' || user || '_' || restorepoint);
      create_restorepoint(restorepoint);
    end;
  end;
  
  procedure revert_to (restorepoint varchar2) is
    var_user_tables varchar(4000 char);
  begin
    select listagg(table_name, ',') within group (order by table_name) into var_user_tables from user_tables where temporary = 'N';
    run('flashback table ' || var_user_tables || ' to restore point ' || user || '_' || restorepoint);
  end;
  
end;
/

-- Usage: --
-- http://juku:juku@localhost:50000/juku/testing.create_restorepoint?restorepoint=test
-- http://juku:juku@localhost:50000/juku/testing.revert_to?restorepoint=test