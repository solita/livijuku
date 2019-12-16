
-- name: select-all-asiakirjamallit
select
  id, asiakirjalajitunnus, voimaantulovuosi,
  hakemustyyppitunnus, organisaatiolajitunnus,
  poistoaika
from asiakirjamalli

-- name: select-asiakirjamalli-by-id
select
  id, asiakirjalajitunnus, voimaantulovuosi,
  hakemustyyppitunnus, organisaatiolajitunnus,
  sisalto, poistoaika
from asiakirjamalli
where id = :id

-- name: select-asiakirjamalli-sisalto
select sisalto
from asiakirjamalli
where
  voimaantulovuosi <= :vuosi and
  asiakirjalajitunnus = :asiakirjalajitunnus and
  poistoaika is null and
  (organisaatiolajitunnus = :organisaatiolajitunnus or organisaatiolajitunnus is null) and
  (hakemustyyppitunnus = :hakemustyyppitunnus or hakemustyyppitunnus is null)
order by
  voimaantulovuosi desc,
  organisaatiolajitunnus asc nulls last,
  hakemustyyppitunnus asc nulls last
fetch first row only

-- name: insert-asiakirjamalli!
insert into asiakirjamalli (
  id, asiakirjalajitunnus, voimaantulovuosi,
  hakemustyyppitunnus, organisaatiolajitunnus, sisalto)
values (asiakirjamalli_seq.nextval,
  :asiakirjalajitunnus, :voimaantulovuosi,
  :hakemustyyppitunnus, :organisaatiolajitunnus,
  :sisalto)

-- name: update-asiakirjamalli!
update asiakirjamalli set
    asiakirjalajitunnus = :asiakirjalajitunnus,
    voimaantulovuosi = :voimaantulovuosi,
    hakemustyyppitunnus = :hakemustyyppitunnus,
    organisaatiolajitunnus = :organisaatiolajitunnus,
    sisalto = :sisalto
where id = :id

-- name: update-asiakirjamalli-mark-deleted!
update asiakirjamalli set
  poistoaika = sysdate
where id = :id