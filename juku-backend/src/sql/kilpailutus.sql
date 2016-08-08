
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

-- name: select-all-kilpailutukset
select
  kilpailutus.id,
  kilpailutus.organisaatioid,
  organisaatio.nimi as organisaationimi,
  kilpailutus.kohdenimi,
  kilpailutus.sopimusmallitunnus,
  kilpailutus.kohdearvo,
  kilpailutus.kalusto,
  kilpailutus.selite,

  kilpailutus.julkaisupvm,
  kilpailutus.tarjouspaattymispvm,
  kilpailutus.hankintapaatospvm,
  kilpailutus.liikennointialoituspvm,
  kilpailutus.liikennointipaattymispvm,
  kilpailutus.hankittuoptiopaattymispvm,
  kilpailutus.optiopaattymispvm,

  kilpailutus.optioselite,
  kilpailutus.hilmalinkki,
  kilpailutus.asiakirjalinkki,
  kilpailutus.liikennoitsijanimi,
  kilpailutus.tarjousmaara,
  kilpailutus.tarjoushinta1,
  kilpailutus.tarjoushinta2,

  kilpailutus.luontitunnus,
  kilpailutus.luontiaika,
  kilpailutus.muokkaustunnus,
  kilpailutus.muokkausaika
from kilpailutus
  inner join organisaatio on kilpailutus.organisaatioid = organisaatio.id