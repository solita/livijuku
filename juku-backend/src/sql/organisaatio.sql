
-- name: select-organisaatiot
select id, nimi, lajitunnus, pankkitilinumero from organisaatio

-- name: select-organisaatio-like-exttunnus
select id from organisaatio where :tunnus like lower(exttunnus)