
-- name: select-organisaatio-emails
select sahkoposti from kayttaja where organisaatioid = :organisaatioid and sahkopostiviestit = 1