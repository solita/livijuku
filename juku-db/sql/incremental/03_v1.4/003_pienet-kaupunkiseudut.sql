
insert into organisaatiolaji (tunnus, nimi) values ('KS3', 'Pieni kaupunkiseutu');

declare
  type OrganisaatioList is table of organisaatio%rowtype;

  function new_organisaatio(nimi varchar2, exttunnus varchar2) return organisaatio%rowtype is
    o organisaatio%rowtype;
    begin
      o.nimi := nimi;
      o.lajitunnus := 'KS3';
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
 -- pienet toimivaltaiset viranomaiest --
     new_organisaatio('Rovaniemi', 'Rovaniemen-%'),
     new_organisaatio('Kajaani', 'Kajaanin-%'),
     new_organisaatio('Kemi', 'Kemin-%'),
     new_organisaatio('Kokkola', 'Kokkolan-%'),
     new_organisaatio('Seinäjoki', 'Seinäjoen-%'),
     new_organisaatio('Rauma', 'Rauman-%'),
     new_organisaatio('Salo', 'Salon-%'),
     new_organisaatio('Hyvinkää', 'Hyvinkään-%'),
     new_organisaatio('Riihimäki', 'Riihimäen-%'),
     new_organisaatio('Mikkeli', 'Mikkelin-%'),
     new_organisaatio('Savonlinna', 'Savonlinnan-%'),
     new_organisaatio('Imatra', 'Imatran-%')
 ));


end;
/