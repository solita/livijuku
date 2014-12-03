
begin
  model.new_classification('avustuskohdelaji', 'Avustuskohdelaji', 2, 'AKLAJI');
end;
/

create table avustuskohde (
  hakemusid not null references hakemus (id),
  avustuskohdelajitunnus references avustuskohdelaji (tunnus),
  haettavaavustus number(9,2),
  omarahoitus number(9,2),

  constraint avustuskohde_pk primary key (hakemusid, avustuskohdelajitunnus)
);

begin
  model.define_mutable(model.new_entity('avustuskohde', 'Avustuskohde', 'AK'));
end;
/

/*
 * kaksitasoinen luokittelu
 *
begin
  model.new_classification('avustuskohdeluokka', 'Avustuskohdeluokka', 3, 'AKLUOKKA');
end;
/

insert into avustuskohdeluokka (tunnus, nimi) values ('PSA', 'PSA:n mukaisen liikenteen hankinta');
insert into avustuskohdeluokka (tunnus, nimi) values ('HVK', 'Hintavelvoitteiden korvaaminen');
insert into avustuskohdeluokka (tunnus, nimi) values ('LSK', 'Liikenteen suunnittelu ja kehittämishankkeet');

create table avustuskohdelaji (
  avustuskohdeluokkatunnus not null references avustuskohdeluokka (tunnus),
  tunnus varchar2(3 char),
  nimi varchar2(100 char),
  description varchar2(100 char),

  constraint avustuskohdelaji_pk primary key (avustuskohdeluokkatunnus, tunnus)
);

declare
  e constant entity%rowtype := model.new_entity('avustuskohdelaji', 'Avustuskohdelaji', 'AKLAJI');
begin
  model.define_datetemporal(e);
  model.define_mutable(e);
end;
/

create table avustuskohde (
  hakemusid not null references hakemus (id),
  avustuskohdeluokkatunnus varchar2(3 char) not null,
  avustuskohdelajitunnus varchar2(3 char) not null,
  haettavaavustus number(9,2),
  omarahoitus number(9,2),
  
  constraint avustuskohde_pk primary key (hakemusid, avustuskohdeluokkatunnus, avustuskohdelajitunnus),
  constraint avustuskohde_aklaji_fk 
    foreign key (avustuskohdeluokkatunnus, avustuskohdelajitunnus) 
    references avustuskohdelaji (avustuskohdeluokkatunnus, tunnus)
);

begin
  model.define_mutable(model.new_entity('avustuskohde', 'Avustuskohde', 'AK'));
end;
/
*/