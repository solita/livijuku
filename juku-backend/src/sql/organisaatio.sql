
-- name: select-organisaatiot
select id, nimi, lajitunnus, pankkitilinumero from organisaatio
order by decode(lajitunnus, 'KS1', 1, 'KS2', 2, 'KS3', 3, 'ELY', 4, 0), nimi

-- name: select-organisaatio-like-exttunnus
select id from organisaatio where :tunnus like lower(exttunnus)