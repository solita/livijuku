begin
  model.new_classification('liikennetyyppi', 'Liikennetyyppi', 3, 'LNETYYPPI');
  model.new_classification('suoritetyyppi', 'Suoritetyyppi', 2, 'STYYPPI');
  model.new_classification('lipputyyppi', 'Lipputyyppi', 2, 'LPUTYYPPI');
end;
/

create table liikennesuorite (
  hakemusid not null references hakemus (id),
  liikennetyyppitunnus not null references liikennetyyppi (tunnus),
  numero number(6),

  suoritetyyppitunnus not null references suoritetyyppi (tunnus),
  nimi varchar2(200 char) not null,
  linjaautot number(12) not null,
  taksit number(12) not null,
  ajokilometrit number(12, 2) not null,
  matkustajamaara number(12) not null,
  lipputulo number(12, 2) not null,
  nettohinta number(12, 2) not null,

  constraint liikennesuorite_pk primary key (hakemusid, liikennetyyppitunnus, numero)
);

create table lippusuorite (
  hakemusid not null references hakemus (id),
  lipputyyppitunnus not null references lipputyyppi (tunnus),
  numero number(6),

  myynti number(12) not null,
  matkat number(12) not null,
  asiakashinta number(12, 2) not null,
  keskipituus number(12, 2) not null,
  lipputulo number(12, 2) not null,
  julkinenrahoitus number(12, 2) not null,

  seutulippualue varchar2(200 char),

  constraint lippusuorite_pk primary key (hakemusid, lipputyyppitunnus, numero)
);

begin
  model.define_immutable(model.new_entity('liikennesuorite', 'Liikennesuorite', 'LNESUORITE'));
  model.define_immutable(model.new_entity('lippusuorite', 'Lippusuorite', 'LPUSUORITE'));
end;
/

insert into liikennetyyppi (tunnus, nimi) values ('PSA', 'Paikallisliikenne tai muu PSA:n mukainen liikenne');
insert into liikennetyyppi (tunnus, nimi) values ('PAL', 'Palveluliikenne');

insert into suoritetyyppi (tunnus, nimi) values ('LS', 'Linjasuorite');
insert into suoritetyyppi (tunnus, nimi) values ('SS', 'Sopimussuorite');
insert into suoritetyyppi (tunnus, nimi) values ('KS', 'Kokonaissuorite');

insert into lipputyyppi (tunnus, nimi) values ('30', '30-päivän kausikortti');
insert into lipputyyppi (tunnus, nimi) values ('VL', 'Vuosilippu');
insert into lipputyyppi (tunnus, nimi) values ('SL', 'Sarjalippu');
insert into lipputyyppi (tunnus, nimi) values ('KL', 'Kaupunkilippu');
insert into lipputyyppi (tunnus, nimi) values ('NL', 'Nuorisolipppu');
insert into lipputyyppi (tunnus, nimi) values ('M', 'Muu kunnan tukema lippu');
insert into lipputyyppi (tunnus, nimi) values ('SE', 'Seutulippu');