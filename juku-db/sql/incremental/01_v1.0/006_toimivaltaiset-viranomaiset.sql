
declare
  type OsastoList is table of osasto%rowtype;

  function new_osasto(nimi varchar2) return osasto%rowtype is
    o osasto%rowtype;
  begin
    o.nimi := nimi;
    return o;
  end;

  procedure save_osastot(osastot OsastoList) is
  begin
    forall i in osastot.first .. osastot.last
      insert into osasto (id, nimi) values
        (osasto_seq.nextval, osastot(i).nimi);
  end;
begin

save_osastot(OsastoList(
  new_osasto('Helsingin seudun liikenne'),
  new_osasto('Hämeenlinna'),
  new_osasto('Joensuu'),
  new_osasto('Jyväskylä'),
  new_osasto('Kotka'),
  new_osasto('Kouvola'),
  new_osasto('Kuopio'),
  new_osasto('Lahti'),
  new_osasto('Lappeenranta'),
  new_osasto('Oulu'),
  new_osasto('Pori'),
  new_osasto('Tampere'),
  new_osasto('Turku'),
  new_osasto('Vaasa')
));

end;
/