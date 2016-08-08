
insert into kayttooikeus (tunnus, nimi) values ('view-liitteet',
                                                'Tämä oikeus on kailla, joilla on mikä tahansa liitteiden katseluoikeus.');

insert into kayttooikeus (tunnus, nimi) values ('view-omat-liitteet',
                                                'Omien liitteiden katseluoikeus.');
insert into kayttooikeus (tunnus, nimi) values ('view-kaikki-liitteet',
                                                'Kaikkien liitteiden katseluoikeus');

-- Pääkäyttäjän, käsittelijän ja päätöksentekijän oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
  select * from
    (select column_value from table(sys.odcivarchar2list('PK', 'PA', 'KA')))
    cross join
    (select * from table(sys.odcivarchar2list('view-kaikki-liitteet')));

-- Hakijan ja allekirjoittajan oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
  select * from
    (select column_value from table(sys.odcivarchar2list('HA', 'AK')))
    cross join
    (select * from table(sys.odcivarchar2list('view-omat-liitteet')));

insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
  select kayttajaroolitunnus, 'view-liitteet' from kayttajaroolioikeus
  where kayttooikeustunnus in ('view-omat-liitteet', 'view-kaikki-liitteet');