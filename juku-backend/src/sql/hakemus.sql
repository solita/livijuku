
-- name: select-hakemuskausi
select vuosi, diaarinumero, tilatunnus from hakemuskausi where vuosi = :vuosi

-- name: select-organisaation-hakemukset
select id, diaarinumero, vuosi, hakemustyyppitunnus, hakemustilatunnus,
       organisaatioid, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus_view where organisaatioid = :organisaatioid

-- name: select-hakemus+
with 
kayttajanimi as (
  select tunnus, nvl(nimi, etunimi || ' ' || sukunimi) nimi from kayttaja
), 
sisalto as (
  select (select nimi from kayttajanimi where tunnus = muokkaustunnus) muokkaaja, muokkausaika
  from (
    select avustuskohde.muokkaustunnus, avustuskohde.muokkausaika from avustuskohde
    where avustuskohde.hakemusid = :hakemusid and avustuskohde.muokkausaika <> avustuskohde.luontiaika
    union all
    select liite.muokkaustunnus, liite.muokkausaika from liite
    where liite.hakemusid = :hakemusid
    order by muokkausaika desc) m 
  where rownum = 1
), 
lahetys as (
  select (select nimi from kayttajanimi where tunnus = luontitunnus) lahettaja, 
         luontiaika lahetysaika 
  from (
    select luontitunnus, luontiaika
    from hakemustilatapahtuma l where l.hakemusid = :hakemusid and l.hakemustilatunnus in ('V', 'TV')
    order by luontiaika desc)
  where rownum = 1
)
select id, diaarinumero, vuosi, hakemustyyppitunnus, hakemustilatunnus, tilinumero,
       organisaatioid, kasittelijanimi, hakuaika_alkupvm, hakuaika_loppupvm, selite, kasittelija, luontitunnus,
       sisalto.muokkaaja,
       case when sisalto.muokkausaika is null then hakemus.muokkausaika else sisalto.muokkausaika end muokkausaika,
       lahetys.*,
       ely_siirtymaaikasopimukset, ely_joukkoliikennetukikunnat -- ely hakemuksen perustiedot
from hakemus_view hakemus left join sisalto on (1=1) left join lahetys on (1=1)
where id = :hakemusid

-- name: select-hakemus
select id, diaarinumero, vuosi, hakemustyyppitunnus, hakemustilatunnus,
  organisaatioid, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus_view where id = :hakemusid

-- name: select-all-hakemukset
with sisalto as (
  select hakemusid, max(muokkausaika) muokkausaika
  from (
    select avustuskohde.hakemusid, avustuskohde.muokkausaika from avustuskohde
    union all
    select liite.hakemusid, liite.muokkausaika from liite)
  group by hakemusid
)
select id, diaarinumero, vuosi, hakemustyyppitunnus, hakemustilatunnus,
       greatest(hakemus.muokkausaika, coalesce(sisalto.muokkausaika, date'1-1-1')) muokkausaika,
       organisaatioid, kasittelijanimi, hakuaika_alkupvm, hakuaika_loppupvm
from hakemus_view hakemus left join sisalto on hakemus.id = sisalto.hakemusid

-- name: select-hakemussuunnitelmat
select id, diaarinumero, vuosi, hakemustyyppitunnus, hakemustilatunnus,
  organisaatioid, hakuaika_alkupvm, hakuaika_loppupvm,
  (select nvl(sum(avustuskohde.haettavaavustus), 0) from avustuskohde
  where hakemusid = hakemus.id) "haettu-avustus",
  nvl(suunniteltuavustus, 0) "myonnettava-avustus"
from hakemus_view hakemus
where vuosi = :vuosi and hakemustyyppitunnus = :hakemustyyppitunnus

-- name: select-avustuskohteet
select avustuskohde.hakemusid,
  avustuskohde.avustuskohdeluokkatunnus, avustuskohde.avustuskohdelajitunnus,
  avustuskohde.haettavaavustus, avustuskohde.omarahoitus
from avustuskohde
  inner join avustuskohdeluokka akluokka on akluokka.tunnus = avustuskohde.avustuskohdeluokkatunnus
  inner join avustuskohdelaji aklaji on aklaji.avustuskohdeluokkatunnus = avustuskohde.avustuskohdeluokkatunnus and aklaji.tunnus = avustuskohde.avustuskohdelajitunnus
where avustuskohde.hakemusid = :hakemusid
order by akluokka.jarjestys, aklaji.jarjestys

-- name: select-avustushakemus-kasittelija
select kasittelija from hakemus where vuosi = :vuosi and organisaatioid = :organisaatioid and hakemustyyppitunnus = 'AH0' and kasittelija is not null
order by muokkausaika desc, id desc

-- name: update-avustuskohde!
update avustuskohde set
  haettavaavustus = :haettavaavustus,
  omarahoitus = :omarahoitus
where hakemusid = :hakemusid and
      avustuskohdeluokkatunnus = :avustuskohdeluokkatunnus and
      avustuskohdelajitunnus = :avustuskohdelajitunnus

-- name: update-hakemustila!
update hakemus set hakemustilatunnus = :hakemustilatunnus
where id = :hakemusid and hakemustilatunnus in (:expectedhakemustilatunnus)

-- name: select-avustuskohdeluokat
select tunnus, nimi, jarjestys from avustuskohdeluokka

-- name: select-avustuskohdelajit
select avustuskohdeluokkatunnus, tunnus, nimi, jarjestys from avustuskohdelaji

-- name: update-hakemus-set-diaarinumero!
update hakemus set diaarinumero = :diaarinumero where vuosi = :vuosi and organisaatioid = :organisaatioid

-- name: insert-taydennyspyynto!
insert into taydennyspyynto (hakemusid, numero, maarapvm, selite)
values (:hakemusid,
        (select nvl(max(p.numero), 0) + 1 from taydennyspyynto p where p.hakemusid = :hakemusid),
        :maarapvm,
        :selite)

-- name: insert-hakemustila-event!
insert into hakemustilatapahtuma (hakemusid, hakemustilatunnus, jarjestysnumero)
values (:hakemusid, :hakemustilatunnus,
       (select nvl(max(p.jarjestysnumero), 0) + 1 from hakemustilatapahtuma p where p.hakemusid = :hakemusid))

-- name: insert-hakemustila-event+asiakirja!
insert into hakemustilatapahtuma (hakemusid, hakemustilatunnus, asiakirjapdf, jarjestysnumero)
values (:hakemusid, :hakemustilatunnus, :asiakirja,
       (select nvl(max(p.jarjestysnumero), 0) + 1 from hakemustilatapahtuma p where p.hakemusid = :hakemusid))

-- name: select-latest-hakemusasiakirja
select * from
  (select asiakirjapdf asiakirja
    from hakemustilatapahtuma
    where hakemusid = :hakemusid and hakemustilatunnus in ('V', 'TV')
    order by luontiaika desc)
where rownum = 1

-- name: select-latest-taydennyspyynto
select * from
  (select numero, maarapvm, selite
   from taydennyspyynto
   where hakemusid = :hakemusid
   order by numero desc)
where rownum = 1

-- name: select-other-hakemukset
select id, hakemustyyppitunnus from hakemus
where (vuosi, organisaatioid) = (select vuosi, organisaatioid from hakemus where id = :hakemusid) and
      id <> :hakemusid