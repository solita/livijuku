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
  
  function list_to_char (l in sys.odcivarchar2list, separator in varchar2) 
  return varchar2 is
    r varchar2(32767 char) := case when l.count > 0 then l(1) else '' end;
  begin
    for i in 2..l.count loop
      r := r || separator || l(i);
    end loop;
    return r;
  end;
  
  procedure revert_to (restorepoint varchar2) is
    var_user_tables sys.odcivarchar2list;
  begin
    select cast(collect(to_char(table_name)) as sys.odcivarchar2list) into var_user_tables from user_tables where temporary = 'N';
    run('flashback table ' || list_to_char(var_user_tables, ', ') || ' to restore point ' || user || '_' || restorepoint);
  end;
  
end;
/

-- Usage: --
-- http://juku:juku@localhost:50000/juku/testing.create_restorepoint?restorepoint=test
-- http://juku:juku@localhost:50000/juku/testing.revert_to?restorepoint=test