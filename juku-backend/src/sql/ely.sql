
-- name: select-hakemus-maararahatarve
select
  maararahatarvetyyppitunnus, sidotut, uudet, tulot, kuvaus
from maararahatarve where hakemusid = :hakemusid

-- name: select-maararahatarvetyypit
select tunnus, nimi, 1 jarjestys from maararahatarvetyyppi


-- name: select-hakemus-kehityshanke
select numero, nimi, arvo, kuvaus
from kehityshanke where hakemusid = :hakemusid

-- name: delete-hakemus-kehityshanke!
delete from kehityshanke where hakemusid = :hakemusid

