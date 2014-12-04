
-- name: select-organisaation-hakemukset
select id, vuosi, hakemustyyppitunnus, hakemustilatunnus, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus where organisaatioid = :organisaatioid

-- name: select-hakemus
select id, vuosi, hakemustyyppitunnus, hakemustilatunnus, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus where id = :hakemusid

-- name: select-avustuskohteet
select hakemusid, avustuskohdelajitunnus, haettavaavustus, omarahoitus
from avustuskohde where hakemusid = :hakemusid

-- name: update-avustuskohde!
update avustuskohde set
  haettavaavustus = :haettavaavustus,
  omarahoitus = :omarahoitus
where hakemusid = :hakemusid and avustuskohdelajitunnus = :avustuskohdelajitunnus