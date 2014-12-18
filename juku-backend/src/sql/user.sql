-- name: select-user
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma
from kayttaja where tunnus = :tunnus

-- name: select-users-where-organization
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma
from kayttaja where organisaatioid = :organisaatioid