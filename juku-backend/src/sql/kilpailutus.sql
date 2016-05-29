
-- name: select-kilpailutus
select
  id,
  organisaatioid,
  kohdenimi,
  sopimusmallitunnus,
  kohdearvo,
  kalusto,
  selite,

  julkaisupvm,
  tarjouspaattymispvm,
  hankintapaatospvm,
  liikennointialoituspvm,
  liikennointipaattymispvm,
  hankittuoptiopaattymispvm,
  optiopaattymispvm,

  optioselite,

  hilmalinkki,
  asiakirjalinkki,

  liikennoitsijanimi,
  tarjousmaara,
  tarjoushinta1,
  tarjoushinta2
from kilpailutus where id = :kilpailutusid

-- name: delete-kilpailutus-where-id!
delete kilpailutus where id = :kilpailutusid

-- name: select-sopimusmallit
select tunnus, nimi, jarjestys from sopimusmalli order by jarjestys