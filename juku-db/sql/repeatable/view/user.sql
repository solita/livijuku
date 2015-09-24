
create or replace view kayttaja_view as
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma, sahkoposti, sahkopostiviestit, kirjautumisaika, poistettuaika,
  (select cast (collect (kayttajarooli.nimi) as sys.odcivarchar2list)
   from kayttajakayttajarooli inner join kayttajarooli on kayttajarooli.tunnus = kayttajakayttajarooli.kayttajaroolitunnus
   where kayttajatunnus = kayttaja.tunnus) roolit
from kayttaja;