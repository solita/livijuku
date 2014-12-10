-- name: select-user
select tunnus, etunimi, sukunimi, nimi, organisaatioid, jarjestelma
from kayttaja where tunnus = :tunnus