
declare
  type OrganisaatioList is table of organisaatio%rowtype;

  function new_organisaatio(nimi varchar2, laji varchar2) return organisaatio%rowtype is
    o organisaatio%rowtype;
  begin
    o.nimi := nimi;
    o.lajitunnus := laji;
    return o;
  end;

  procedure save_osastot(organisaatiot OrganisaatioList) is
  begin
    forall i in organisaatiot.first .. organisaatiot.last
      insert into organisaatio (id, nimi, lajitunnus) values
        (organisaatio_seq.nextval, organisaatiot(i).nimi, organisaatiot(i).lajitunnus);
  end;
begin

save_osastot(OrganisaatioList(
  -- toimivaltaiset viranomaiest --
  new_organisaatio('Helsingin seudun liikenne', 'KS1'),
  new_organisaatio('Hämeenlinna', 'KS2'),
  new_organisaatio('Joensuu', 'KS2'),
  new_organisaatio('Jyväskylä', 'KS2'),
  new_organisaatio('Kotka', 'KS2'),
  new_organisaatio('Kouvola', 'KS2'),
  new_organisaatio('Kuopio', 'KS2'),
  new_organisaatio('Lahti', 'KS2'),
  new_organisaatio('Lappeenranta', 'KS2'),
  new_organisaatio('Oulu', 'KS1'),
  new_organisaatio('Pori', 'KS2'),
  new_organisaatio('Tampere', 'KS1'),
  new_organisaatio('Turku', 'KS1'),
  new_organisaatio('Vaasa', 'KS2'),

  -- liikennevirasto --
  new_organisaatio('Liikennevirasto', 'LV')
));

end;
/

update organisaatio set pankkitilinumero = 'FI4250001510000023' where lajitunnus in ('KS1', 'KS2');