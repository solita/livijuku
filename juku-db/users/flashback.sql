-- Suorita juku_dba tunnuksella

-- juku_raider_front
begin
  dbms_epg.authorize_dad (
    dad_name => 'juku_admin_service',
    user     => 'JUKU_RAIDER_FRONT');
end;
/

grant flashback any table to JUKU_RAIDER_FRONT;

-- juku_raider_front_release
begin
  dbms_epg.authorize_dad (
    dad_name => 'juku_admin_service',
    user     => 'JUKU_RAIDER_FRONT_RELEASE');
end;
/

grant flashback any table to juku_raider_front_release;
