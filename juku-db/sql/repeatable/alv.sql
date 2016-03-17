
create or replace package alv as
  function plus_alv (rahamaara_alv0 number, alv number) return number deterministic;
  function effective_alv(avustuskohdeluokkatunnus varchar2) return number deterministic;
  function avustusrahamaara (rahamaara_alv0 number, avustuskohdeluokkatunnus varchar2) return number deterministic;
end alv;
/

create or replace package body alv as

  function plus_alv (rahamaara_alv0 number, alv number) return number deterministic is
  begin
    return rahamaara_alv0 * (1 + alv / 100);
  end;
  
  function effective_alv(avustuskohdeluokkatunnus varchar2) return number deterministic is
  begin
    return case when avustuskohdeluokkatunnus = 'HK' then 10 else 0 end;
  end;
  
  function avustusrahamaara (rahamaara_alv0 number, avustuskohdeluokkatunnus varchar2) return number deterministic is
  begin
    return round(plus_alv(rahamaara_alv0, effective_alv(avustuskohdeluokkatunnus)), 2);
  end;
end alv;
/