
-- name: select-avustus-ah0-group-by-vuosi
select hakemus.vuosi, 0 as myonnetty, sum(haettavaavustus) as rahamaara
from avustuskohde
  inner join hakemus on hakemus.id = avustuskohde.hakemusid
  inner join organisaatio on organisaatio.id = hakemus.organisaatioid
where hakemus.hakemustyyppitunnus = 'AH0' and
      (organisaatio.lajitunnus = :organisaatiolajitunnus or :organisaatiolajitunnus = 'ALL')
group by hakemus.vuosi
union all
select hakemus.vuosi, 1 as myonnetty, sum(myonnettyavustus)
from hakemus
  inner join organisaatio on organisaatio.id = hakemus.organisaatioid
  left join paatos on hakemus.id = paatos.hakemusid and voimaantuloaika is not null and poistoaika is null
where hakemus.hakemustyyppitunnus = 'AH0' and
      (organisaatio.lajitunnus = :organisaatiolajitunnus or :organisaatiolajitunnus = 'ALL')
group by hakemus.vuosi

-- name: select-myonnettyavustus-ah0
select hakemus.vuosi, organisaatio.id, paatos.myonnettyavustus
from hakemus
  inner join organisaatio on organisaatio.id = hakemus.organisaatioid
  left join paatos on hakemus.id = paatos.hakemusid and voimaantuloaika is not null and poistoaika is null
where hakemus.hakemustyyppitunnus = 'AH0' and
      (organisaatio.lajitunnus = :organisaatiolajitunnus or :organisaatiolajitunnus = 'ALL')