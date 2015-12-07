
-- name: select-hakemus-maararahatarve
select
  maararahatarvetyyppitunnus, sidotut, uudet, tulot, kuvaus
from maararahatarve where hakemusid = :hakemusid

-- name: select-maararahatarvetyypit
select tunnus, nimi, jarjestys from maararahatarvetyyppi


-- name: select-hakemus-kehityshanke
select numero, nimi, arvo, kuvaus
from kehityshanke where hakemusid = :hakemusid

-- name: delete-hakemus-kehityshanke!
delete from kehityshanke where hakemusid = :hakemusid

-- name: select-ely-hakemus
select ely_siirtymaaikasopimukset, ely_joukkoliikennetukikunnat from hakemus where id = :hakemusid

