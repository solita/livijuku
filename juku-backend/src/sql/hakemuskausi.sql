
-- name: select-all-hakemuskaudet
select vuosi, tilatunnus, hakuohje_contenttype from hakemuskausi

-- name: select-hakemuskausi
select vuosi from hakemuskausi where vuosi = :vuosi

-- name: select-maararaha
select maararaha, ylijaama
from maararaha where vuosi = :vuosi and organisaatiolajitunnus = :organisaatiolajitunnus

-- name: update-hakemuskausi-set-hakuohje!
update hakemuskausi set hakuohje_sisalto = :sisalto, hakuohje_nimi = :nimi, hakuohje_contenttype = :contenttype
where vuosi = :vuosi

-- name: select-hakuohje-sisalto
select hakuohje_contenttype contenttype, hakuohje_sisalto sisalto from hakemuskausi where vuosi = :vuosi

-- name: update-hakemuskausi-set-tila!
update hakemuskausi set tilatunnus = :newtunnus
where vuosi = :vuosi and tilatunnus = :expectedtunnus

-- name: count-hakemustilat-for-vuosi-hakemustyyppi
select vuosi, hakemustyyppitunnus, count(*) 
from hakemus
group by vuosi, hakemustyyppitunnus, hakemustilatunnus

-- name: select-all-hakuajat
select vuosi, hakemustyyppitunnus, alkupvm, loppupvm
from hakuaika

-- name: insert-hakemuskausi-if-not-exists!
insert into hakemuskausi (vuosi)
select :vuosi from dual 
where not exists (select 1 from hakemuskausi where vuosi = :vuosi)