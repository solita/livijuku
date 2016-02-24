
-- name: select-avustuskohteet
select avustuskohde.hakemusid,
  avustuskohde.avustuskohdeluokkatunnus, avustuskohde.avustuskohdelajitunnus,
  avustuskohde.haettavaavustus, avustuskohde.omarahoitus
from avustuskohde
  inner join avustuskohdeluokka akluokka on akluokka.tunnus = avustuskohde.avustuskohdeluokkatunnus
  inner join avustuskohdelaji aklaji on aklaji.avustuskohdeluokkatunnus = avustuskohde.avustuskohdeluokkatunnus and aklaji.tunnus = avustuskohde.avustuskohdelajitunnus
where avustuskohde.hakemusid = :hakemusid
order by akluokka.jarjestys, aklaji.jarjestys

-- name: update-avustuskohde!
update avustuskohde set
  haettavaavustus = :haettavaavustus,
  omarahoitus = :omarahoitus
where hakemusid = :hakemusid and
      avustuskohdeluokkatunnus = :avustuskohdeluokkatunnus and
      avustuskohdelajitunnus = :avustuskohdelajitunnus

-- name: select-avustuskohdeluokat
select tunnus, nimi, jarjestys from avustuskohdeluokka

-- name: select-avustuskohdelajit
select avustuskohdeluokkatunnus, tunnus, nimi, jarjestys from avustuskohdelaji

-- name: select-hakemukset
select id, organisaatioid, hakemustilatunnus from hakemus where id in (:hakemusids)

