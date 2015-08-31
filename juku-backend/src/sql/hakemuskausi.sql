
-- name: select-all-hakemuskaudet
select vuosi, tilatunnus, hakuohje_contenttype from hakemuskausi

-- name: select-maararaha
select maararaha, ylijaama
from maararaha where vuosi = :vuosi and organisaatiolajitunnus = :organisaatiolajitunnus

-- name: update-hakemuskausi-set-hakuohje!
update hakemuskausi set hakuohje_sisalto = :sisalto, hakuohje_nimi = :nimi, hakuohje_contenttype = :contenttype
where vuosi = :vuosi

-- name: update-hakemuskausi-set-diaarinumero!
update hakemuskausi set diaarinumero = :diaarinumero where vuosi = :vuosi

-- name: select-hakuohje-sisalto
select hakuohje_contenttype contenttype, hakuohje_sisalto sisalto from hakemuskausi where vuosi = :vuosi and hakuohje_sisalto is not null

-- name: update-hakemuskausi-set-tila!
update hakemuskausi set tilatunnus = :newtunnus
where vuosi = :vuosi and tilatunnus = :expectedtunnus

-- name: count-hakemustilat-for-vuosi-hakemustyyppi
select vuosi, hakemustyyppitunnus, hakemustilatunnus, count(*) count
from hakemus
group by vuosi, hakemustyyppitunnus, hakemustilatunnus

-- name: select-all-hakuajat
select vuosi, hakemustyyppitunnus, alkupvm hakuaika_alkupvm, loppupvm hakuaika_loppupvm
from hakuaika

-- name: select-hakuajat-by-vuosi
select hakemustyyppitunnus, alkupvm, loppupvm
from hakuaika where vuosi = :vuosi

-- name: insert-hakemuskausi-if-not-exists!
insert into hakemuskausi (vuosi)
select :vuosi from dual 
where not exists (select 1 from hakemuskausi where vuosi = :vuosi)

-- name: insert-avustuskohteet-for-kausi!
insert into avustuskohde (hakemusid, avustuskohdeluokkatunnus, avustuskohdelajitunnus)
select hakemus.id,
  avustuskohdelaji.avustuskohdeluokkatunnus,
  avustuskohdelaji.tunnus
from hakemus
  inner join organisaatio on organisaatio.id = hakemus.organisaatioid
  cross join avustuskohdelaji
where hakemus.vuosi = :vuosi and
      avustuskohdelaji.voimaantulovuosi <= :vuosi and
      avustuskohdelaji.lakkaamisvuosi > :vuosi and
      (avustuskohdelaji.avustuskohdeluokkatunnus != 'K' or
       avustuskohdelaji.tunnus != 'RT' or
       organisaatio.lajitunnus = 'KS1')

-- name: sulje-kaikki-hakemuskauden-hakemukset!
update hakemus set hakemustilatunnus = 'S' where vuosi = :vuosi
