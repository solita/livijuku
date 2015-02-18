
create table kayttajarooli (
  tunnus varchar2 (2 char) constraint kayttajarooli_pk primary key,
  nimi varchar2 (200 char) not null,
  ssonimi varchar2 (200 char) not null,
  description varchar2 (2000 char)
);

create table kayttooikeus (
  tunnus varchar2 (30 char) constraint kayttooikeus_pk primary key,
  nimi varchar2 (200 char) not null,
  description varchar2 (2000 char)
);

create table kayttajaroolioikeus (
  kayttajaroolitunnus references kayttajarooli(tunnus),
  kayttooikeustunnus references kayttooikeus(tunnus),
  
  constraint kayttajaroolioikeus_pk primary key (kayttajaroolitunnus, kayttooikeustunnus)
);

declare
  e entity%rowtype := model.new_entity('kayttajarooli', 'Käyttäjärooli', 'KAROOLI');
begin
  model.define_mutable(e);
  model.rename_fk_constraints(e);
end;
/
declare
  e entity%rowtype := model.new_entity('kayttooikeus', 'Käyttöoikeus', 'KOIKEUS');
begin
  model.define_mutable(e);
  model.rename_fk_constraints(e);
end;
/
declare
  e entity%rowtype := model.new_entity('kayttajaroolioikeus', 'Käyttäjäroolioikeus', 'KAROOLIOIKEUS');
begin
  model.define_mutable(e);
  model.rename_fk_constraints(e);
end;
/

insert into kayttajarooli (tunnus, nimi, ssonimi) values ('HA', 'Hakija', 'juku_hakija');
insert into kayttajarooli (tunnus, nimi, ssonimi) values ('AK', 'Allekirjoittaja', 'juku_allekirjoittaja');
insert into kayttajarooli (tunnus, nimi, ssonimi) values ('KA', 'Käsittelijä', 'juku_kasittelija');
insert into kayttajarooli (tunnus, nimi, ssonimi) values ('PA', 'Päätöksentekijä', 'juku_paatoksentekija');
insert into kayttajarooli (tunnus, nimi, ssonimi) values ('PK', 'Pääkäyttäjä', 'juku_paakayttaja');

insert into kayttooikeus (tunnus, nimi) values ('view-omat-hakemukset', 'Omien hakemusten katseluoikeus');
insert into kayttooikeus (tunnus, nimi) values ('view-kaikki-hakemukset', 'Kaikkien hakemusten katseluoikeus');
insert into kayttooikeus (tunnus, nimi) values ('modify-hakemuskausi', 'Hakemuskauden hallinnointioikeus');

insert into kayttooikeus (tunnus, nimi) values ('modify-oma-hakemus', 'Oman hakemuksen muokkausoikeus');
insert into kayttooikeus (tunnus, nimi) values ('paatosuunnittelu', 'Päätösten suunnitteluoikeus');

insert into kayttooikeus (tunnus, nimi) values ('allekirjoita-hakemus', 'Hakemuksen allekirjoitusoikeus');
insert into kayttooikeus (tunnus, nimi) values ('hyvaksy-paatos', 'Päätöksen hyväksymisoikeus');

-- Käsittelijäroolien oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
select * from 
(select column_value from table(sys.odcivarchar2list('PK', 'PA', 'KA')))
cross join 
(select tunnus from kayttooikeus where tunnus not in ('view-omat-hakemukset', 'modify-oma-hakemus'));

-- Hakijaroolien oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
select * from 
(select column_value from table(sys.odcivarchar2list('HA', 'AK')))
cross join 
(select tunnus from kayttooikeus where tunnus not in ('modify-hakemuskausi', 'paatosuunnittelu', 'hyvaksy-paatos'));