
create table kayttajarooli (
  tunnus varchar2 (2 char) constraint kayttajarooli_pk primary key,
  nimi varchar2 (200 char) not null,
  ssonimi varchar2 (200 char) not null,
  kuvaus varchar2 (2000 char)
);

create table kayttajakayttajarooli (
  kayttajatunnus references kayttaja(tunnus),
  kayttajaroolitunnus references kayttajarooli(tunnus),

  constraint kayttajakayttajarooli_pk primary key (kayttajatunnus, kayttajaroolitunnus)
);

create table kayttooikeus (
  tunnus varchar2 (50 char) constraint kayttooikeus_pk primary key,
  nimi varchar2 (200 char) not null,
  kuvaus varchar2 (2000 char)
);

create table kayttajaroolioikeus (
  kayttajaroolitunnus references kayttajarooli(tunnus),
  kayttooikeustunnus references kayttooikeus(tunnus),
  
  constraint kayttajaroolioikeus_pk primary key (kayttajaroolitunnus, kayttooikeustunnus)
);

begin
  model.define_mutable(model.new_entity('kayttajarooli', 'Käyttäjärooli', 'KAROOLI'));
  model.define_mutable(model.new_entity('kayttooikeus', 'Käyttöoikeus', 'OIKEUS'));
  model.define_immutable(model.new_entity('kayttajaroolioikeus', 'Käyttäjäroolioikeus', 'KAROOLIOIKEUS'));
  model.define_immutable(model.new_entity('kayttajakayttajarooli', 'Käyttäjän käyttäjärooli', 'KKROOLI'));
end;
/

insert into kayttajarooli (tunnus, nimi, ssonimi) values ('HA', 'Hakija', 'juku_hakija');
insert into kayttajarooli (tunnus, nimi, ssonimi) values ('AK', 'Allekirjoittaja', 'juku_allekirjoittaja');
insert into kayttajarooli (tunnus, nimi, ssonimi) values ('KA', 'Käsittelijä', 'juku_kasittelija');
insert into kayttajarooli (tunnus, nimi, ssonimi) values ('PA', 'Päätöksentekijä', 'juku_paatoksentekija');
insert into kayttajarooli (tunnus, nimi, ssonimi) values ('PK', 'Pääkäyttäjä', 'juku_paakayttaja');

insert into kayttooikeus (tunnus, nimi) values ('view-hakemus', 'Hakemuksen perustiedon katseluoikeus');

insert into kayttooikeus (tunnus, nimi) values ('view-oma-hakemus',
                                                'Omien hakemusten sisältötietojen katseluoikeus.');
insert into kayttooikeus (tunnus, nimi) values ('view-kaikki-hakemukset',
                                                'Kaikkien hakemusten sisältötietojen katseluoikeus');
insert into kayttooikeus (tunnus, nimi) values ('view-kaikki-lahetetyt-hakemukset',
                                                'Kaikkien lähetettyjen (ei keskeneräisten) hakemusten sisältötietojen katseluoikeus');

insert into kayttooikeus (tunnus, nimi) values ('view-hakemuskausi', 'Hakemuskauden katseluoikeus');

insert into kayttooikeus (tunnus, nimi) values ('modify-hakemuskausi', 'Hakemuskauden hallinnointioikeus');
insert into kayttooikeus (tunnus, nimi) values ('modify-oma-hakemus', 'Oman hakemuksen muokkausoikeus');
insert into kayttooikeus (tunnus, nimi) values ('kasittely-hakemus', 'Hakemuksien käsittely- ja päätösten valmisteluoikeus');

insert into kayttooikeus (tunnus, nimi) values ('allekirjoita-oma-hakemus', 'Oman hakemuksen allekirjoitusoikeus');
insert into kayttooikeus (tunnus, nimi) values ('hyvaksy-paatos', 'Päätöksen hyväksymisoikeus');

insert into kayttooikeus (tunnus, nimi) values ('delete-kayttaja', 'Oikeus merkitä käyttäjä poistetuksi');

insert into kayttooikeus (tunnus, nimi) values ('view-non-livi-kayttaja', 'Oikeus nähdä muiden kuin liikenneviraston käyttäjien tietoja');


-- Käsittelijän oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
select 'KA', column_value from table(sys.odcivarchar2list('view-kaikki-hakemukset', 'view-hakemuskausi', 'modify-hakemuskausi', 'kasittely-hakemus', 'delete-kayttaja', 'view-non-livi-kayttaja'))
;

-- Pääkäyttäjän ja päätöksentekijän oikeudet oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
select * from 
(select column_value from table(sys.odcivarchar2list('PK', 'PA')))
cross join 
(select kayttooikeustunnus from kayttajaroolioikeus ka_oikeudet where ka_oikeudet.kayttajaroolitunnus = 'KA'
union all select * from table(sys.odcivarchar2list('hyvaksy-paatos')))
;

-- Hakijan oikeudet --
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
select 'HA', column_value from table(sys.odcivarchar2list('view-kaikki-lahetetyt-hakemukset', 'view-oma-hakemus', 'modify-oma-hakemus', 'allekirjoita-oma-hakemus'));

-- Allekirjoittajan oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
select 'AK', kayttooikeustunnus 
from kayttajaroolioikeus ha_oikeudet where ha_oikeudet.kayttajaroolitunnus = 'HA'
--union all select 'AK', column_value from table(sys.odcivarchar2list('allekirjoita-oma-hakemus'))
;

-- Kaikkien oikeudet --

insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
select * from 
(select tunnus from kayttajarooli)
cross join
(select * from table(sys.odcivarchar2list('view-hakemus')));

