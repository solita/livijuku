
create or replace view hakemus_view as
select
  hakemus.id, 'LIVI/' || hakemus.id || '/07.00.01/' || hakemus.vuosi diaarinumero, 
  hakemus.vuosi, hakemus.hakemustyyppitunnus, hakemus.hakemustilatunnus, hakemus.muokkausaika,
  organisaatioid, hakuaika.alkupvm hakuaika_alkupvm, hakuaika.loppupvm hakuaika_loppupvm, 
  selite, hakemus.suunniteltuavustus
from hakemus 
inner join hakuaika 
  on hakemus.vuosi = hakuaika.vuosi and 
     hakemus.hakemustyyppitunnus = hakuaika.hakemustyyppitunnus
;