
begin
  dbms_epg.authorize_dad (
    dad_name => 'juku_admin_service',
    user     => 'JUKU_RAIDER_FRONT');
end;
/

grant flashback any table to JUKU_RAIDER_FRONT;