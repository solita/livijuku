
-- name: select-hakemus-liikennesuorite
select
  liikennetyyppitunnus, numero, suoritetyyppitunnus, nimi,
  linjaautot, taksit, ajokilometrit, matkustajamaara, lipputulo, nettohinta
from liikennesuorite where hakemusid = :hakemusid

-- name: select-suoritetyypit
select tunnus, nimi, 1 jarjestys from suoritetyyppi

-- name: delete-hakemus-liikennesuorite!
delete from liikennesuorite where hakemusid = :hakemusid



-- name: select-hakemus-lippusuorite
select
  lipputyyppitunnus, numero,
  myynti, matkat, asiakashinta, keskipituus,
  lipputulo, julkinenrahoitus, seutulippualue
from lippusuorite where hakemusid = :hakemusid

-- name: select-lipputyypit
select tunnus, nimi, 1 jarjestys from lipputyyppi

-- name: delete-hakemus-lippusuorite!
delete from lippusuorite where hakemusid = :hakemusid

