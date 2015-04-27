
-- name: select-max-vuosi-from-hakemuskausi
select nvl(max(vuosi), 100) next from hakemuskausi

-- name: select-akohde-amounts-broup-by-organisaatiolaji
select lajitunnus, min(amount) akohdeamount, count(distinct amount) distinctvalues
from (
  select hakemus.id, organisaatio.lajitunnus, count(*) amount from hakemus
    inner join avustuskohde on hakemus.id = avustuskohde.hakemusid
    inner join organisaatio on organisaatio.id = hakemus.organisaatioid
  group by hakemus.id, organisaatio.lajitunnus)
group by lajitunnus

-- name: select-count-akohdelaji
select count(*) amount from avustuskohdelaji