
-- ely hakemustyyppi --
insert into hakemustyyppi (tunnus, nimi) values ('ELY', 'ELY hakemus');

-- määrärahatarpeet --

create table maararahatarvetyyppi (
  tunnus varchar2(3 char),
  nimi varchar2(100 char),
  jarjestys number(3) default 1,
  kuvaus varchar2(2000 char),
  voimaantulovuosi number(4) default 0 not null,
  lakkaamisvuosi number(4) default 9999 not null,

  constraint maararahatarvetyypi_pk primary key (tunnus)
);

begin
  model.define_mutable(model.new_entity('maararahatarvetyyppi', 'Määrärahatarvetyyppi', 'MRTTYYPPI'));
end;
/

insert into maararahatarvetyyppi (tunnus, nimi, jarjestys) values ('BS', 'Bruttosopimus', 1);
insert into maararahatarvetyyppi (tunnus, nimi, jarjestys) values ('KK1', 'Käyttösopimuskorvaukset (alueellinen)', 2);
insert into maararahatarvetyyppi (tunnus, nimi, jarjestys) values ('KK2', 'Käyttösopimuskorvaukset (reitti)', 3);

create table maararahatarve (
  hakemusid not null references hakemus (id),
  maararahatarvetyyppitunnus not null references maararahatarvetyyppi (tunnus),

  sidotut number(12,2) default 0 not null,
  uudet number(12,2) default 0 not null,
  tulot number(12,2),
  kuvaus varchar2(2000 char),

  constraint maararahatarve_pk primary key (hakemusid, maararahatarvetyyppitunnus)
);

-- kehityshankkeet --

create table kehityshanke (
  hakemusid not null references hakemus (id),
  numero number(6),

  nimi varchar2(200 char),
  arvo number,
  kuvaus varchar2(200 char),

  constraint kehityshanke_pk primary key (hakemusid, numero)
);

begin
  model.define_mutable(model.new_entity('maararahatarve', 'Määrärahatarve', 'MRTARVE'));
  model.define_mutable(model.new_entity('kehityshanke', 'Kehityshanke', 'KHANKE'));
end;
/

-- Ely hakemuksen perustiedot --

alter table hakemus add (
  ely_siirtymaaikasopimukset number(12,2),
  ely_joukkoliikennetukikunnat number(12,2)
);