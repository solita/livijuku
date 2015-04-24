
 
create table avustuskohdeluokka (
  tunnus varchar2(3 char) constraint avustuskohdeluokka_pk primary key,
  nimi varchar2(100 char),
  jarjetys number(3) default 1,
  description varchar2(100 char)
);

begin
  model.define_mutable(model.new_entity('avustuskohdeluokka', 'Avustuskohdeluokka', 'AKLUOKKA'));
end;
/

insert into avustuskohdeluokka (tunnus, nimi, jarjetys) values ('PSA', 'PSA:n mukaisen liikenteen hankinta', 1);
insert into avustuskohdeluokka (tunnus, nimi, jarjetys) values ('HK', 'Hintavelvoitteiden korvaaminen', 2);
insert into avustuskohdeluokka (tunnus, nimi, jarjetys) values ('K', 'Liikenteen suunnittelu ja kehittämishankkeet', 3);

create table avustuskohdelaji (
  avustuskohdeluokkatunnus not null references avustuskohdeluokka (tunnus),
  tunnus varchar2(3 char),
  nimi varchar2(100 char),
  jarjetys number(3) default 1, 
  description varchar2(100 char),
  voimaantulovuosi number(4) default 0 not null,
  lakkaamisvuosi number(4) default 9999 not null,
  
  constraint avustuskohdelaji_pk primary key (avustuskohdeluokkatunnus, tunnus)
);

begin
  model.define_mutable(model.new_entity('avustuskohdelaji', 'Avustuskohdelaji', 'AKLAJI'));
end;
/

insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('PSA', '1', 'Paikallisliikenne', 1);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('PSA', '2', 'Integroitupalvelulinja', 2);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('PSA', 'M', 'Muu PSA:n mukaisen liikenteen järjestäminen', 3);

insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('HK', 'SL', 'Seutulippu', 1);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('HK', 'KL', 'Kaupunkilippu tai kuntalippu', 2);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('HK', 'LL', 'Liityntälippu', 3);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('HK', 'TL', 'Työmatkalippu', 4);

insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('K', 'IM', 'Informaatio ja maksujärjestelmien kehittäminen', 1);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('K', 'MPK', 'Matkapalvelukeskuksen suunnittelu ja kehittäminen', 2);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('K', 'MK', 'Matkakeskuksen suunnittelu ja kehittäminen', 3);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('K', 'RT', 'Raitiotien suunnittelu', 4);
insert into avustuskohdelaji (avustuskohdeluokkatunnus, tunnus, nimi, jarjetys) values ('K', 'M', 'Muu hanke', 5);


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
