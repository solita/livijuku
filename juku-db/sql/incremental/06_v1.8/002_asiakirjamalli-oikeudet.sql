-- Asiakirjamallien käyttöoikeudet
insert into kayttooikeus (tunnus, nimi) values ('view-asiakirjamalli',
                                                'Kaikkien asiakirjamallien katseluoikeus.');

insert into kayttooikeus (tunnus, nimi) values ('modify-asiakirjamalli',
                                                'Kaikkien asiakirjamallien hallintaoikeus.');

-- Pääkäyttäjän, käsittelijän ja päätöksentekijän oikeudet
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
select * from
  (select column_value from table(sys.odcivarchar2list('PK', 'PA', 'KA'))) cross join
  (select * from table(sys.odcivarchar2list('view-asiakirjamalli', 'modify-asiakirjamalli')));
