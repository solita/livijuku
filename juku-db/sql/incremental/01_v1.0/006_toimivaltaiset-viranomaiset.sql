
declare
  type OrganisaatioList is table of organisaatio%rowtype;

  function new_organisaatio(nimi varchar2) return organisaatio%rowtype is
    o organisaatio%rowtype;
  begin
    o.nimi := nimi;
    return o;
  end;

  procedure save_osastot(organisaatiot OrganisaatioList) is
  begin
    forall i in organisaatiot.first .. organisaatiot.last
      insert into organisaatio (id, nimi) values
        (organisaatio_seq.nextval, organisaatiot(i).nimi);
  end;
begin

save_osastot(OrganisaatioList(
  new_organisaatio('Helsingin seudun liikenne'),
  new_organisaatio('Hämeenlinna'),
  new_organisaatio('Joensuu'),
  new_organisaatio('Jyväskylä'),
  new_organisaatio('Kotka'),
  new_organisaatio('Kouvola'),
  new_organisaatio('Kuopio'),
  new_organisaatio('Lahti'),
  new_organisaatio('Lappeenranta'),
  new_organisaatio('Oulu'),
  new_organisaatio('Pori'),
  new_organisaatio('Tampere'),
  new_organisaatio('Turku'),
  new_organisaatio('Vaasa')
));

end;
/