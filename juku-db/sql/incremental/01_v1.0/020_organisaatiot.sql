
declare
  type OrganisaatioList is table of organisaatio%rowtype;

  function new_organisaatio(nimi varchar2, laji varchar2, exttunnus varchar2) return organisaatio%rowtype is
    o organisaatio%rowtype;
  begin
    o.nimi := nimi;
    o.lajitunnus := laji;
    o.exttunnus := exttunnus;
    return o;
  end;

  procedure save_osastot(organisaatiot OrganisaatioList) is
  begin
    forall i in organisaatiot.first .. organisaatiot.last
      insert into organisaatio (id, nimi, lajitunnus, exttunnus) values
        (organisaatio_seq.nextval, organisaatiot(i).nimi, organisaatiot(i).lajitunnus, organisaatiot(i).exttunnus);
  end;
begin

save_osastot(OrganisaatioList(
  -- toimivaltaiset viranomaiest --
  new_organisaatio('Helsingin seudun liikenne', 'KS1', 'helsingin-%'),
  new_organisaatio('Hämeenlinna', 'KS2', 'hämeenlinnan-%'),
  new_organisaatio('Joensuu', 'KS2', 'joensuun-%'),
  new_organisaatio('Jyväskylä', 'KS2', 'jyväskylän-%'),
  new_organisaatio('Kotka', 'KS2', 'kotkan-%'),
  new_organisaatio('Kouvola', 'KS2', 'kouvolan-%'),
  new_organisaatio('Kuopio', 'KS2', 'kuopion-%'),
  new_organisaatio('Lahti', 'KS2', 'lahden-%'),
  new_organisaatio('Lappeenranta', 'KS2', 'lappeenrannan-%'),
  new_organisaatio('Oulu', 'KS1', 'oulun-%'),
  new_organisaatio('Pori', 'KS2', 'porin-%'),
  new_organisaatio('Tampere', 'KS1', 'tampereen-%'),
  new_organisaatio('Turku', 'KS1', 'turun-%'),
  new_organisaatio('Vaasa', 'KS2', 'vaasan-%'),

  -- liikennevirasto --
  new_organisaatio('Liikennevirasto', 'LV', 'liikennevirasto%'),

  -- ELY:t --
  new_organisaatio('Uusimaa', 'ELY', 'ELY-1'),
  new_organisaatio('Varsinais-Suomi', 'ELY', 'ELY-2'),
  new_organisaatio('Kaakkois-Suomi', 'ELY', 'ELY-3'),
  new_organisaatio('Pirkanmaa', 'ELY', 'ELY-4'),
  new_organisaatio('Pohjois-Savo', 'ELY', 'ELY-8'),
  new_organisaatio('Keski-Suomi', 'ELY', 'ELY-9'),
  new_organisaatio('Etelä-Pohjanmaa', 'ELY', 'ELY-10'),
  new_organisaatio('Pohjois-Pohjanmaa', 'ELY', 'ELY-12'),
  new_organisaatio('Lappi', 'ELY', 'ELY-14')
));

end;
/

update organisaatio set pankkitilinumero = 'FI4250001510000023' where lajitunnus in ('KS1', 'KS2', 'ELY');