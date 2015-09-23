-- name: select-user
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma, sahkoposti, sahkopostiviestit, kirjautumisaika
from kayttaja where tunnus = :tunnus

-- name: select-users-where-organization
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma, sahkoposti, sahkopostiviestit, kirjautumisaika,
  (select cast (collect (kayttajarooli.nimi) as sys.odcivarchar2list)
   from kayttajakayttajarooli inner join kayttajarooli on kayttajarooli.tunnus = kayttajakayttajarooli.kayttajaroolitunnus
   where kayttajatunnus = kayttaja.tunnus) roolit
from kayttaja where organisaatioid = :organisaatioid

-- name: select-all-human-users
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma, sahkoposti, sahkopostiviestit, kirjautumisaika,
  (select cast (collect (kayttajarooli.nimi) as sys.odcivarchar2list)
   from kayttajakayttajarooli inner join kayttajarooli on kayttajarooli.tunnus = kayttajakayttajarooli.kayttajaroolitunnus
   where kayttajatunnus = kayttaja.tunnus) roolit
from kayttaja where jarjestelma = 0

-- name: select-all-livi-users
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma, sahkoposti, sahkopostiviestit, kirjautumisaika,
  (select cast (collect (kayttajarooli.nimi) as sys.odcivarchar2list)
   from kayttajakayttajarooli inner join kayttajarooli on kayttajarooli.tunnus = kayttajakayttajarooli.kayttajaroolitunnus
   where kayttajatunnus = kayttaja.tunnus) roolit
from kayttaja
where kayttaja.organisaatioid = (select id from organisaatio where organisaatio.lajitunnus = 'LV')

-- name: select-oikeudet-where-roolit-in
select distinct kayttooikeus.tunnus from kayttooikeus 
  inner join kayttajaroolioikeus on kayttooikeus.tunnus = kayttajaroolioikeus.kayttooikeustunnus
where kayttajaroolioikeus.kayttajaroolitunnus in (:roolit)

-- name: select-roolitunnukset
select tunnus from kayttajarooli where ssonimi in (:ssogroup)

-- name: select-roles
select kayttajaroolitunnus from kayttajakayttajarooli where kayttajatunnus = :tunnus

-- name: select-rolenames
select kayttajarooli.nimi
from kayttajakayttajarooli inner join kayttajarooli on kayttajarooli.tunnus = kayttajakayttajarooli.kayttajaroolitunnus
where kayttajatunnus = :tunnus

-- name: insert-new-roles!
insert into kayttajakayttajarooli (kayttajatunnus, kayttajaroolitunnus)
(select :tunnus, tunnus from kayttajarooli
where
  not exists (select 1 from kayttajakayttajarooli kkrooli
      where kkrooli.kayttajatunnus = :tunnus and
            kkrooli.kayttajaroolitunnus = kayttajarooli.tunnus) and
  tunnus in (:roles))

-- name: delete-previous-roles!
delete from kayttajakayttajarooli where kayttajatunnus = :tunnus and kayttajaroolitunnus not in (:roles)

-- name: update-kayttaja-mark-deleted!
update kayttaja set poistettuaika = sysdate, poistaja = sys_context('userenv', 'CLIENT_IDENTIFIER')
where tunnus = :tunnus