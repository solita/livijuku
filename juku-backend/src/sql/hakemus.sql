
-- name: select-organisaation-hakemukset
select id, vuosi, hakemustyyppitunnus, hakemustilatunnus,
       organisaatioid, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus where organisaatioid = :organisaatioid

-- name: select-hakemus
select id, vuosi, hakemustyyppitunnus, hakemustilatunnus,
       organisaatioid, hakuaika_alkupvm, hakuaika_loppupvm, selite
from hakemus where id = :hakemusid

-- name: select-all-hakemukset
select id, vuosi, hakemustyyppitunnus, hakemustilatunnus,
       organisaatioid, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus

-- name: select-avustuskohteet
select hakemusid, avustuskohdelajitunnus, haettavaavustus, omarahoitus
from avustuskohde where hakemusid = :hakemusid

-- name: select-hakemussuunnitelmat
select id, vuosi, hakemustyyppitunnus, hakemustilatunnus,
  organisaatioid, hakuaika_alkupvm, hakuaika_loppupvm,
  (select nvl(sum(avustuskohde.haettavaavustus), 0) from avustuskohde
  where hakemusid = hakemus.id) "haettu-avustus",
  nvl(suunniteltuavustus, 0) "myonnettava-avustus"
from hakemus
where vuosi = :vuosi and hakemustyyppitunnus = :hakemustyyppitunnus

-- name: update-avustuskohde!
update avustuskohde set
  haettavaavustus = :haettavaavustus,
  omarahoitus = :omarahoitus
where hakemusid = :hakemusid and avustuskohdelajitunnus = :avustuskohdelajitunnus

-- name: update-hakemustila!
update hakemus set hakemustilatunnus = :hakemustilatunnus
where id = :hakemusid