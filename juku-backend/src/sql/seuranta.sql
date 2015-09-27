
-- name: select-hakemus-liikennesuorite
select
  hakemusid, liikennetyyppitunnus, numero, suoritetyyppitunnus, nimi,
  linjaautot, taksit, ajokilometrit, matkustajamaara, asiakastulo, nettohinta, bruttohinta
from liikennesuorite where hakemusid = :hakemusid

-- name: select-hakemus-lippusuorite
select * from lippusuorite where hakemusid = :hakemusid

-- name: select-suoritetyypit
select tunnus, nimi, 1 jarjestys from suoritetyyppi

-- name: delete-hakemus-liikennesuorite!
delete from liikennesuorite where hakemusid = :hakemusid

-- name: delete-hakemus-lippusuorite!
delete from lippusuorite where hakemusid = :hakemusid

