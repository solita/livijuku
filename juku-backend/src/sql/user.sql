-- name: select-user
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma, sahkoposti, sahkopostiviestit
from kayttaja where tunnus = :tunnus

-- name: select-users-where-organization
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma, sahkoposti, sahkopostiviestit
from kayttaja where organisaatioid = :organisaatioid

-- name: select-all-human-users
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma, sahkoposti, sahkopostiviestit
from kayttaja where jarjestelma = 0

-- name: select-oikeudet-where-roolit-in
select distinct kayttooikeus.tunnus from kayttooikeus 
  inner join kayttajaroolioikeus on kayttooikeus.tunnus = kayttajaroolioikeus.kayttooikeustunnus
  inner join kayttajarooli on kayttajarooli.tunnus = kayttajaroolioikeus.kayttajaroolitunnus
where kayttajarooli.ssonimi in (:roolit)