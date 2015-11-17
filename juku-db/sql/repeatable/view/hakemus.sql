
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
    when hakemus.kasittelija is null then null
    else coalesce(kasittelija.nimi, kasittelija.etunimi || ' ' || kasittelija.sukunimi)
  end kasittelijanimi,
  hakemus.luontitunnus, hakemus.suunniteltuavustus,
  ely_siirtymaaikasopimukset, ely_joukkoliikennetukikunnat
from hakemus 
inner join hakuaika 
  on hakemus.vuosi = hakuaika.vuosi and 
     hakemus.hakemustyyppitunnus = hakuaika.hakemustyyppitunnus
left join kayttaja kasittelija
  on hakemus.kasittelija = kasittelija.tunnus
;
