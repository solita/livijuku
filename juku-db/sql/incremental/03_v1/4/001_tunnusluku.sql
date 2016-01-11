begin
  model.new_classification('sopimustyyppi', 'Sopimustyyppi', 3, 'SOTYYPPI');
  model.new_classification('paastoluokka', 'Päästöluokka', 3, 'PLUOKKA');
  model.new_classification('lippuluokka', 'Lippuluokka', 3, 'LLUOKKA');
  model.new_classification('viikonpaivaluokka', 'Viikonpäiväluokka', 2, 'VLUOKKA');
end;
/

insert into sopimustyyppi (tunnus, nimi) values ('BR', 'PSA brutto');
insert into sopimustyyppi (tunnus, nimi) values ('KOS', 'PSA KOS');
insert into sopimustyyppi (tunnus, nimi) values ('SA', 'Siirtymäajan liikenne');
insert into sopimustyyppi (tunnus, nimi) values ('ME', 'Markkinaehtoinen liikenne');

create table fact_liikenne (
  vuosi number(4),
  kuukausi number(2),
  organisaatioid not null references organisaatio (id),
  sopimustyyppitunnus not null references sopimustyyppi (tunnus),

  nousut number(9),
  lahdot number(9),
  linjakilometrit number(12, 2),

  constraint fact_liikenne_pk primary key (vuosi, kuukausi, organisaatioid, sopimustyyppitunnus)
);


begin
  model.define_mutable(model.new_entity('fact_liikenne', 'Liikenne fakta', 'FCTLI'));
end;
/