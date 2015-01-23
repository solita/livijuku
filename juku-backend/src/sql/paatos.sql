
-- name: select-paatos
select
  hakemusid, paatosnumero, paattaja, myonnettyavustus,
  voimaantuloaika, poistoaika, selite
from paatos where hakemusid = :hakemusid and poistoaika is null

-- name: update-paatos!
update paatos set selite = :selite, myonnettyavustus = :myonnettyavustus
where hakemusid = :hakemusid and poistoaika is null and voimaantuloaika is null

-- name: insert-paatos!
insert into paatos (hakemusid, paatosnumero, myonnettyavustus, selite)
values (:hakemusid,
        (select nvl(max(p.paatosnumero), 0) + 1 from paatos p where p.hakemusid = :hakemusid),
        :myonnettyavustus, :selite)

-- name: update-paatos-hyvaksytty!
update paatos set voimaantuloaika = sysdate
where hakemusid = :hakemusid and poistoaika is null and voimaantuloaika is null