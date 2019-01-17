
alter session set container=&1;

select sys_context ('userenv', 'con_name') as container_name from dual;