
-- name: select-current-paatos
select
  hakemusid, paatosnumero, paattaja, myonnettyavustus,
  voimaantuloaika, poistoaika, selite
from paatos where hakemusid = :hakemusid and poistoaika is null

-- name: select-paatos
select
  hakemusid, paatosnumero, paattaja, myonnettyavustus,
  voimaantuloaika, poistoaika, selite
from paatos where hakemusid = :hakemusid and paatosnumero = :paatosnumero

-- name: update-paatos!
update paatos set selite = :selite, myonnettyavustus = :myonnettyavustus
where hakemusid = :hakemusid and poistoaika is null and voimaantuloaika is null

-- name: insert-paatos!
insert into paatos (hakemusid, paatosnumero, myonnettyavustus, selite)
values (:hakemusid,
        (select nvl(max(p.paatosnumero), 0) + 1 from paatos p where p.hakemusid = :hakemusid),
        :myonnettyavustus, :selite)

-- name: update-paatos-hyvaksytty!
update paatos set voimaantuloaika = sysdate, paattaja = sys_context('userenv', 'CLIENT_IDENTIFIER')
where hakemusid = :hakemusid and poistoaika is null and voimaantuloaika is null

-- name: update-paatos-hylatty!
update paatos set poistoaika = sysdate
where hakemusid = :hakemusid and poistoaika is null and voimaantuloaika is not null

-- name: select-latest-paatosasiakirja
select * from
  (select asiakirjapdf asiakirja
   from hakemustilatapahtuma
   where hakemusid = :hakemusid and hakemustilatunnus in ('P')
   order by luontiaika desc)
where rownum = 1

-- name: select-lahetys-pvm
select max(luontiaika) lahetyspvm
from hakemustilatapahtuma
where hakemusid = :hakemusid and hakemustilatunnus in ('V', 'TV')

-- name: select-latest-paattajanimi
select paattajanimi from (
  select paattajanimi from paatos where paattajanimi is not null order by muokkausaika desc
) where rownum = 1

-- name: select-hakemus-paatos
select hakemus.id hakemusid, p.voimaantuloaika, p.myonnettyavustus
from hakemus inner join paatos p
    on p.hakemusid = hakemus.id
where hakemus.vuosi = :vuosi and
      hakemus.organisaatioid = :organisaatioid and
      p.poistoaika is null and
      hakemus.hakemustyyppitunnus = :hakemustyyppitunnus
