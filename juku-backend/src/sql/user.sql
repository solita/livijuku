-- name: select-user
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma, sahkoposti
from kayttaja where tunnus = :tunnus

-- name: select-users-where-organization
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma, sahkoposti
from kayttaja where organisaatioid = :organisaatioid

-- name: select-oikeudet-where-roolit-in
select distinct kayttooikeus.tunnus from kayttooikeus 
  inner join kayttajaroolioikeus on kayttooikeus.tunnus = kayttajaroolioikeus.kayttooikeustunnus
  inner join kayttajarooli on kayttajarooli.tunnus = kayttajaroolioikeus.kayttajaroolitunnus
where kayttajarooli.ssonimi in (:roolit)