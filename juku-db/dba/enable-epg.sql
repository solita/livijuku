/*
 This script enables embedded pl/sql gateway for dynamic authentication
 The allowed authentication mechanism needs to be basic

 NOTE: this only inteded for development and testing environments
 */

-- check current container
select sys_context('USERENV', 'CON_NAME') name from dual;

-- EPG supports only basic authentication
-- Change authentication to basic:
declare
  v_cfg SYS.XMLType;
begin
  select XMLQuery('declare default element namespace "http://xmlns.oracle.com/xdb/xdbconfig.xsd";
                   copy $i := $cfg
                   modify (
                     replace value of node $i/xdbconfig/sysconfig/protocolconfig/httpconfig/authentication/allow-mechanism with ''basic''
                   )
                   return $i'
                  PASSING DBMS_XDB_CONFIG.cfg_get() AS "cfg"
                  RETURNING CONTENT)
    into v_cfg from dual;
  dbms_xdb_config.cfg_update(XMLType.createXML(v_cfg.getClobVal()));
end;
/

-- Set http port
begin
  DBMS_XDB_CONFIG.SETHTTPPORT(50000);
end;
/

-- Create database access description for epg/juku
begin
  dbms_epg.create_dad (
    dad_name => 'juku_admin_service',
    path     => '/juku/*');
end;
/