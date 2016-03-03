
-- name: select-avustus-ah0-group-by-vuosi
select avustustyyppi, vuosi, sum(rahamaara) from (
  select 'H' as avustustyyppi, hakemus.vuosi, haettavaavustus as rahamaara
  from avustuskohde
    inner join hakemus on hakemus.id = avustuskohde.hakemusid
    inner join organisaatio on organisaatio.id = hakemus.organisaatioid
  where hakemus.hakemustyyppitunnus = 'AH0' and
        (organisaatio.lajitunnus = :organisaatiolajitunnus or :organisaatiolajitunnus = 'ALL')
  union all
  select
    'H' as avustustyyppi, hakemus.vuosi,
    (select nvl(sum(maararahatarve.sidotut + maararahatarve.uudet - nvl(maararahatarve.tulot, 0)), 0)
     from maararahatarve where hakemusid = hakemus.id) +
    (select nvl(sum(kehityshanke.arvo), 0) from kehityshanke where hakemusid = hakemus.id) +
    nvl(hakemus.ely_kaupunkilipputuki, 0) + nvl(hakemus.ely_seutulipputuki, 0) +
    nvl(hakemus.ely_ostot, 0) + nvl(hakemus.ely_kehittaminen, 0) as rahamaara
  from hakemus
  where hakemus.hakemustyyppitunnus = 'ELY' and
        :organisaatiolajitunnus in ('ELY', 'ALL')
  union all
  select 'M' as avustustyyppi, hakemus.vuosi, myonnettyavustus as rahamaara
  from hakemus
    inner join organisaatio on organisaatio.id = hakemus.organisaatioid
    left join paatos on hakemus.id = paatos.hakemusid and voimaantuloaika is not null and poistoaika is null
  where hakemus.hakemustyyppitunnus in ('AH0', 'ELY') and
        (organisaatio.lajitunnus = :organisaatiolajitunnus or :organisaatiolajitunnus = 'ALL')
)
group by avustustyyppi, vuosi
order by vuosi asc, avustustyyppi asc

-- name: select-avustus-ah0-group-by-organisaatio-vuosi
with myonnetty_avustus as (
    select organisaatio.id organisaatioid,
      hakemus.vuosi, paatos.myonnettyavustus
    from hakemus
      inner join organisaatio on organisaatio.id = hakemus.organisaatioid
      left join paatos on hakemus.id = paatos.hakemusid and voimaantuloaika is not null and poistoaika is null
    where hakemus.hakemustyyppitunnus = 'AH0' and
          (organisaatio.lajitunnus = :organisaatiolajitunnus or :organisaatiolajitunnus = 'ALL')
),
    haettuavustus as (
      select organisaatio.id organisaatioid,
        hakemus.vuosi, sum(avustuskohde.haettavaavustus) as haettavaavustus
      from avustuskohde
        inner join hakemus on hakemus.id = avustuskohde.hakemusid
        inner join organisaatio on organisaatio.id = hakemus.organisaatioid
      where hakemus.hakemustyyppitunnus = 'AH0' and
            (organisaatio.lajitunnus = :organisaatiolajitunnus or :organisaatiolajitunnus = 'ALL')
      group by organisaatio.id, hakemus.vuosi
  )
select haettuavustus.organisaatioid, haettuavustus.vuosi,
  haettuavustus.haettavaavustus,  myonnetty_avustus.myonnettyavustus
from haettuavustus
  left join myonnetty_avustus on
    haettuavustus.organisaatioid = myonnetty_avustus.organisaatioid and
    haettuavustus.vuosi = myonnetty_avustus.vuosi

-- name: select-avustus-asukastakohti-ah0-group-by-organisaatio-vuosi
select organisaatio.id organisaatioid,
  hakemus.vuosi,
       paatos.myonnettyavustus / alue.asukasmaara myonnettyavustus_asukastakohti
from hakemus
  inner join organisaatio on organisaatio.id = hakemus.organisaatioid
  left join paatos on hakemus.id = paatos.hakemusid and voimaantuloaika is not null and poistoaika is null
  left join fact_alue alue on
                             alue.organisaatioid = hakemus.organisaatioid and
                             alue.vuosi = hakemus.vuosi
where hakemus.hakemustyyppitunnus = 'AH0' and
      (organisaatio.lajitunnus = :organisaatiolajitunnus or :organisaatiolajitunnus = 'ALL')