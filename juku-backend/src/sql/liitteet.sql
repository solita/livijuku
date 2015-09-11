
-- name: select-liitteet
select hakemusid, liitenumero, nimi, contenttype, dbms_lob.getlength(sisalto) bytesize
from liite where hakemusid = :hakemusid and poistoaika is null

-- name: select-liite-sisalto
select nimi, contenttype, sisalto from liite where hakemusid = :hakemusid and liitenumero = :liitenumero

-- name: select-liitteet+sisalto
select hakemusid, liitenumero, nimi, contenttype, sisalto
from liite where hakemusid = :hakemusid and poistoaika is null

-- name: insert-liite!
insert into liite (hakemusid, liitenumero, nimi, contenttype, sisalto)
values (:hakemusid,
        (select nvl(max(p.liitenumero), 0) + 1 from liite p where p.hakemusid = :hakemusid),
        :nimi, :contenttype, :sisalto)

-- name: update-liite-set-poistoaika!
update liite set poistoaika = sysdate
where hakemusid = :hakemusid and liitenumero = :liitenumero

-- name: update-liite-set-nimi!
update liite set nimi = :nimi
where hakemusid = :hakemusid and liitenumero = :liitenumero

-- name: select-sum-liitekoko
select nvl(sum(dbms_lob.getlength(sisalto)), 0) bytesize
from liite where hakemusid = :hakemusid and poistoaika is null

-- name: select-hakemus-for-update
select id, organisaatioid, hakemustilatunnus from hakemus where id = :hakemusid for update