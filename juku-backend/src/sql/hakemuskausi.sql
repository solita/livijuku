
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

-- name: insert-hakemukset-for-kausi!
insert into hakemus (id, vuosi, hakemustyyppitunnus, organisaatioid)
select hakemus_seq.nextval, hakuaika.vuosi, hakuaika.hakemustyyppitunnus, organisaatio.id organisaatioid
from hakuaika
  inner join organisaatio
    on (organisaatio.lajitunnus = 'ELY' and hakuaika.hakemustyyppitunnus = 'ELY') OR
       (hakuaika.hakemustyyppitunnus in ('AH0', 'MH1', 'MH2') and organisaatio.lajitunnus in ('KS1', 'KS2'))
where hakuaika.vuosi = :vuosi

-- name: insert-avustuskohteet-for-kausi!
insert into avustuskohde (hakemusid, avustuskohdeluokkatunnus, avustuskohdelajitunnus)
select hakemus.id,
  avustuskohdelaji.avustuskohdeluokkatunnus,
  avustuskohdelaji.tunnus
from hakemus
  inner join organisaatio on organisaatio.id = hakemus.organisaatioid
  cross join avustuskohdelaji
where hakemus.vuosi = :vuosi and
      hakemus.hakemustyyppitunnus in ('AH0', 'MH1', 'MH2') and
      avustuskohdelaji.voimaantulovuosi <= :vuosi and
      avustuskohdelaji.lakkaamisvuosi > :vuosi and
      (avustuskohdelaji.avustuskohdeluokkatunnus != 'K' or
       avustuskohdelaji.tunnus != 'RT' or
       organisaatio.lajitunnus = 'KS1')

-- name: insert-maararahatarpeet-for-kausi!
insert into maararahatarve (hakemusid, maararahatarvetyyppitunnus, tulot)
select hakemus.id,
  maararahatarvetyyppi.tunnus,
  decode(maararahatarvetyyppi.tunnus, 'BS', 0, null)
from hakemus cross join maararahatarvetyyppi
where hakemus.vuosi = :vuosi and
      hakemus.hakemustyyppitunnus = 'ELY' and
      maararahatarvetyyppi.voimaantulovuosi <= :vuosi and
      maararahatarvetyyppi.lakkaamisvuosi > :vuosi and
      hakemus.hakemustyyppitunnus = 'ELY'

-- name: sulje-kaikki-hakemuskauden-hakemukset!
update hakemus set hakemustilatunnus = 'S' where vuosi = :vuosi

-- name: sulje-kaikki-hakemuskauden-hakemukset!
update hakemus set hakemustilatunnus = 'S' where vuosi = :vuosi
