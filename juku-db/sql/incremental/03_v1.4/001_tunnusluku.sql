/* Dimensio-taulut */

begin
  model.new_classification('sopimustyyppi', 'Sopimustyyppi', 3, 'SOTYYPPI');
  model.new_classification('paastoluokka', 'Päästöluokka', 2, 'PLUOKKA');
  model.new_classification('lippuluokka', 'Lippuluokka', 2, 'LLUOKKA');
  model.new_classification('viikonpaivaluokka', 'Viikonpäiväluokka', 2, 'VLUOKKA');
end;
/

insert into sopimustyyppi (tunnus, nimi) values ('BR', 'PSA brutto');
insert into sopimustyyppi (tunnus, nimi) values ('KOS', 'PSA KOS');
insert into sopimustyyppi (tunnus, nimi) values ('SA', 'Siirtymäajan liikenne');
insert into sopimustyyppi (tunnus, nimi) values ('ME', 'Markkinaehtoinen liikenne');

insert into viikonpaivaluokka (tunnus, nimi) values ('A', 'Arkiviikonpäivä');
insert into viikonpaivaluokka (tunnus, nimi) values ('LA', 'Lauantai');
insert into viikonpaivaluokka (tunnus, nimi) values ('SU', 'Sununtai');

-- https://fi.wikipedia.org/wiki/Euro-p%C3%A4%C3%A4st%C3%B6luokitukset
insert into paastoluokka (tunnus, nimi) values ('E0', 'Euro 0');
insert into paastoluokka (tunnus, nimi) values ('E1', 'Euro 1');
insert into paastoluokka (tunnus, nimi) values ('E2', 'Euro 2');
insert into paastoluokka (tunnus, nimi) values ('E3', 'Euro 3');
insert into paastoluokka (tunnus, nimi) values ('E4', 'Euro 4');
insert into paastoluokka (tunnus, nimi) values ('E5', 'Euro 5');
insert into paastoluokka (tunnus, nimi) values ('E6', 'Euro 6');

insert into lippuluokka (tunnus, nimi) values ('KE', 'Kertalippu');
insert into lippuluokka (tunnus, nimi) values ('AR', 'Arvolippu');
insert into lippuluokka (tunnus, nimi) values ('KA', 'Kausilippu');
insert into lippuluokka (tunnus, nimi) values ('0', 'Mikä tahansa lippu');

/* Faktataulut */

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

create table fact_liikenneviikko (
  vuosi number(4),
  organisaatioid not null references organisaatio (id),
  sopimustyyppitunnus not null references sopimustyyppi (tunnus),
  viikonpaivaluokkatunnus not null references viikonpaivaluokka (tunnus),

  nousut number(9),
  lahdot number(9),
  linjakilometrit number(12, 2),

  constraint fact_liikenneviikko_pk primary key (vuosi, organisaatioid, sopimustyyppitunnus,  viikonpaivaluokkatunnus)
);

create table fact_lipputulo (
  vuosi number(4),
  kuukausi number(2),
  organisaatioid not null references organisaatio (id),
  sopimustyyppitunnus not null references sopimustyyppi (tunnus),
  lippuluokkatunnus not null references lippuluokka (tunnus),

  tulo number(12, 2),
  constraint fact_lipputulo_pk primary key (vuosi, kuukausi, organisaatioid, sopimustyyppitunnus, lippuluokkatunnus)
);

create table fact_liikennointikorvaus (
  vuosi number(4),
  kuukausi number(2),
  organisaatioid not null references organisaatio (id),
  sopimustyyppitunnus not null references sopimustyyppi (tunnus),

  korvaus number(12, 2),
  nousukorvaus number(12, 2),
  constraint fact_liikennointikorvause_pk primary key (vuosi, kuukausi, organisaatioid, sopimustyyppitunnus)
);

create table fact_lippuhinta (
  vuosi number(4),
  organisaatioid not null references organisaatio (id),
  sopimustyyppitunnus not null references sopimustyyppi (tunnus),
  lippuluokkatunnus not null references lippuluokka (tunnus),
  vyohykelukumaara number(1),
  hinta number(12, 2),
  constraint fact_lippuhinta_pk primary key (vuosi, organisaatioid, sopimustyyppitunnus, lippuluokkatunnus, vyohykelukumaara)
);

create table fact_kalusto (
  vuosi number(4),
  organisaatioid not null references organisaatio (id),
  sopimustyyppitunnus not null references sopimustyyppi (tunnus),
  paastoluokkatunnus not null references paastoluokka (tunnus),

  lukumaara number(9),

  constraint fact_kalusto_pk primary key (vuosi, organisaatioid, sopimustyyppitunnus,  paastoluokkatunnus)
);

begin
  model.define_mutable(model.new_entity('fact_liikenne', 'Liikennevuosi fakta', 'FCTLIKK'));
  model.define_mutable(model.new_entity('fact_liikenneviikko', 'Liikenneviikko fakta', 'FCTLIVIIKKO'));
  
  model.define_mutable(model.new_entity('fact_lipputulo', 'Lipputulo fakta', 'FCTLTULO'));
  model.define_mutable(model.new_entity('fact_liikennointikorvaus', 'Liikennäintikorvaus fakta', 'FCTLKORVAUS'));
  model.define_mutable(model.new_entity('fact_lippuhinta', 'Lippuhinta fakta', 'FCTLHINTA'));
  model.define_mutable(model.new_entity('fact_kalusto', 'Kalusto fakta', 'FCTKALUSTO'));
end;
/