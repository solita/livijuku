
-- Kilpailutusten käyttöoikeudet
insert into kayttooikeus (tunnus, nimi) values ('view-kilpailutus',
                                                'Kaikkien kilpailutusten katseluoikeus.');

insert into kayttooikeus (tunnus, nimi) values ('modify-omat-kilpailutukset',
                                                'Omien kilpailutusten hallintaoikeus.');
insert into kayttooikeus (tunnus, nimi) values ('modify-kaikki-kilpailutukset',
                                                'Kaikkien kilpailutusten hallintaoikeus');

-- Pääkäyttäjän, käsittelijän ja päätöksentekijän oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
  select * from
    (select column_value from table(sys.odcivarchar2list('PK', 'PA', 'KA')))
    cross join
    (select * from table(sys.odcivarchar2list('modify-kaikki-kilpailutukset', 'view-kilpailutus')));

-- Hakijan ja allekirjoittajan oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
  select * from
    (select column_value from table(sys.odcivarchar2list('HA', 'AK')))
    cross join
    (select * from table(sys.odcivarchar2list('modify-omat-kilpailutukset', 'view-kilpailutus')));
