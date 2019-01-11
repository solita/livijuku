
select sys_context('USERENV', 'CON_NAME') name from duaL;

declare
 dad_list  dbms_epg.varchar2_table;
 path_list dbms_epg.varchar2_table;

 name_list dbms_epg.varchar2_table;
 vals_list dbms_epg.varchar2_table;
begin
  dbms_epg.get_dad_list(dad_list);
  for i in 1..dad_list.count loop
    dbms_output.put_line(dad_list(i));
  end loop;
  
  dbms_epg.get_all_dad_mappings('juku_admin_service', path_list);
  for i in 1..path_list.count loop
    dbms_output.put_line(path_list(i));
  end loop;

  dbms_epg.get_all_dad_attributes('juku_admin_service', name_list, vals_list);
  for i in 1..name_list.count loop
    dbms_output.put_line(name_list(i) || ' - ' || vals_list(i));
  end loop;
end;
/

select * from dba_epg_dad_authorization
where dad_name = 'juku_admin_service';

-- read xdb configuration:
select DBMS_XDB_CONFIG.cfg_get() from dual;