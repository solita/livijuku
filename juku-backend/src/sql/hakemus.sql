
-- name: select-organisaation-hakemukset
select id, diaarinumero, vuosi, hakemustyyppitunnus, hakemustilatunnus, muokkausaika,
       organisaatioid, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus_view where organisaatioid = :organisaatioid

-- name: select-hakemus
select id, diaarinumero, vuosi, hakemustyyppitunnus, hakemustilatunnus, muokkausaika,
       organisaatioid, hakuaika_alkupvm, hakuaika_loppupvm, selite
from hakemus_view where id = :hakemusid

-- name: select-all-hakemukset
select id, diaarinumero, vuosi, hakemustyyppitunnus, hakemustilatunnus, muokkausaika,
       organisaatioid, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus_view

-- name: select-hakemussuunnitelmat
select id, diaarinumero, vuosi, hakemustyyppitunnus, hakemustilatunnus, muokkausaika,
  organisaatioid, hakuaika_alkupvm, hakuaika_loppupvm,
  (select nvl(sum(avustuskohde.haettavaavustus), 0) from avustuskohde
  where hakemusid = hakemus.id) "haettu-avustus",
  nvl(suunniteltuavustus, 0) "myonnettava-avustus"
from hakemus_view
where vuosi = :vuosi and hakemustyyppitunnus = :hakemustyyppitunnus

-- name: select-avustuskohteet
select hakemusid, avustuskohdelajitunnus, haettavaavustus, omarahoitus
from avustuskohde where hakemusid = :hakemusid

-- name: update-avustuskohde!
update avustuskohde set
  haettavaavustus = :haettavaavustus,
  omarahoitus = :omarahoitus
where hakemusid = :hakemusid and avustuskohdelajitunnus = :avustuskohdelajitunnus

-- name: update-hakemustila!
update hakemus set hakemustilatunnus = :hakemustilatunnus
where id = :hakemusid