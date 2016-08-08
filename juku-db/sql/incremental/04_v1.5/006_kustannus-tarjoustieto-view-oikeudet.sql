
-- https://issues.solita.fi/browse/LIVIJUKU-924
-- Tietojen näyttämisen rajoitukset guest-käyttäjälle

insert into kayttooikeus (tunnus, nimi) values ('view-tunnusluku-kustannus',
                                                'Ulkoistettujen kustannusten katseluoikeus.');

insert into kayttooikeus (tunnus, nimi) values ('view-kilpailutus-tarjoustieto',
                                                'Kilpailutusten tarjoustiedon katseluoikeus');

-- Em. oikeudet annetaan kaikille käyttäjäryhmille.
-- Huom! guest käyttäjällä ei ole yhtään käyttäjäryhmää
insert into kayttajaroolioikeus (kayttajaroolitunnus, kayttooikeustunnus)
  select * from
    (select tunnus from kayttajarooli)
    cross join
    (select * from table(sys.odcivarchar2list('view-tunnusluku-kustannus', 'view-kilpailutus-tarjoustieto')));