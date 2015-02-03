
-- name: update-hakemuskausi-set-hakuohje!
update hakemuskausi set hakuohje_sisalto = :sisalto, hakuohje_nimi = :nimi, hakuohje_contenttype = :contenttype
where vuosi = :vuosi