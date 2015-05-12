
create or replace view hakemus_view as
select
  hakemus.id, hakemus.diaarinumero,
  hakemus.vuosi, hakemus.hakemustyyppitunnus,
  case
    when (hakemus.hakemustilatunnus = 'K' and sysdate < hakuaika.alkupvm) then '0'
    else hakemus.hakemustilatunnus
  end hakemustilatunnus,
  hakemus.muokkausaika,
  organisaatioid, hakuaika.alkupvm hakuaika_alkupvm, hakuaika.loppupvm hakuaika_loppupvm, 
  selite, hakemus.kasittelija, hakemus.luontitunnus, hakemus.suunniteltuavustus
from hakemus 
inner join hakuaika 
  on hakemus.vuosi = hakuaika.vuosi and 
     hakemus.hakemustyyppitunnus = hakuaika.hakemustyyppitunnus
;