-- tunnuslukujen käyttöoikeudet
insert into kayttooikeus (tunnus, nimi) values ('view-omat-tunnusluvut',
                                                'Omien tunnuslukujen katseluoikeus.');
insert into kayttooikeus (tunnus, nimi) values ('view-kaikki-tunnusluvut',
                                                'Kaikkien tunnuslukujen katseluoikeus');

insert into kayttooikeus (tunnus, nimi) values ('modify-omat-tunnusluvut',
                                                'Omien tunnuslukujen hallintaoikeus.');
insert into kayttooikeus (tunnus, nimi) values ('modify-kaikki-tunnusluvut',
                                                'Kaikkien tunnuslukujen hallintaoikeus');

-- Pääkäyttäjän, käsittelijän ja päätöksentekijän oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
  select * from
    (select column_value from table(sys.odcivarchar2list('PK', 'PA', 'KA')))
    cross join
    (select * from table(sys.odcivarchar2list('modify-kaikki-tunnusluvut')));

-- Hakijan ja allekirjoittajan oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
  select * from
    (select column_value from table(sys.odcivarchar2list('HA', 'AK')))
    cross join
    (select * from table(sys.odcivarchar2list('modify-omat-tunnusluvut')));