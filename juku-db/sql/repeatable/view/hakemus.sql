
create or replace view hakemus_view as
select
  hakemus.id, hakemus.diaarinumero,
  hakemus.vuosi, hakemus.hakemustyyppitunnus,
  case
    when (hakemus.hakemustilatunnus = 'K' and sysdate < hakuaika.alkupvm) then '0'
    else hakemus.hakemustilatunnus
  end hakemustilatunnus, hakemus.muokkausaika, hakemus.organisaatioid,
  hakuaika.alkupvm hakuaika_alkupvm, hakuaika.loppupvm hakuaika_loppupvm, selite,
  hakemus.kasittelija,
  case
    when hakemus.kasittelija is null then 'Ei määritelty'
    else concat(concat(kayttaja.etunimi, ' '), kayttaja.sukunimi)
  end kasittelijanimi,
  hakemus.luontitunnus, hakemus.suunniteltuavustus
from hakemus 
inner join hakuaika 
  on hakemus.vuosi = hakuaika.vuosi and 
     hakemus.hakemustyyppitunnus = hakuaika.hakemustyyppitunnus
left join kayttaja
  on hakemus.kasittelija = kayttaja.tunnus
;
