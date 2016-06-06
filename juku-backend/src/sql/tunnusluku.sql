
-- name: select-liikennevuositilasto
select kuukausi, lahdot, nousut, linjakilometrit
from fact_liikenne
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: insert-default-liikennevuositilasto-if-not-exists!
insert into fact_liikenne (vuosi, kuukausi, organisaatioid, sopimustyyppitunnus)
select :vuosi vuosi, column_value kuukausi,
       :organisaatioid organisaatioid,
       :sopimustyyppitunnus sopimustyyppitunnus
from table(numbers(1,12))
where not exists (
    select 1 from fact_liikenne l where l.vuosi = :vuosi and
                                        l.organisaatioid = :organisaatioid and
                                        l.sopimustyyppitunnus = :sopimustyyppitunnus
)
order by vuosi, kuukausi, organisaatioid, sopimustyyppitunnus

-- name: select-liikenneviikkotilasto
select viikonpaivaluokkatunnus, lahdot, nousut, linjakilometrit
from fact_liikenneviikko
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: insert-default-liikenneviikkotilasto-if-not-exists!
insert into fact_liikenneviikko (vuosi, viikonpaivaluokkatunnus, organisaatioid, sopimustyyppitunnus)
select :vuosi vuosi, tunnus viikonpaivaluokka,
       :organisaatioid organisaatioid,
       :sopimustyyppitunnus sopimustyyppitunnus
from viikonpaivaluokka
where not exists (
    select 1 from fact_liikenneviikko l
    where l.vuosi = :vuosi and
          l.organisaatioid = :organisaatioid and
          l.sopimustyyppitunnus = :sopimustyyppitunnus
)
order by vuosi, viikonpaivaluokka, organisaatioid, sopimustyyppitunnus

-- name: select-kalusto
select paastoluokkatunnus, lukumaara
from fact_kalusto
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: insert-default-kalusto-if-not-exists!
insert into fact_kalusto (vuosi, paastoluokkatunnus, organisaatioid, sopimustyyppitunnus)
  select :vuosi vuosi, tunnus paastoluokkatunnus,
         :organisaatioid organisaatioid,
         :sopimustyyppitunnus sopimustyyppitunnus
  from paastoluokka
  where not exists (
      select 1 from fact_kalusto l
      where l.vuosi = :vuosi and
            l.organisaatioid = :organisaatioid and
            l.sopimustyyppitunnus = :sopimustyyppitunnus
  )
  order by vuosi, paastoluokkatunnus, organisaatioid, sopimustyyppitunnus

-- name: select-lippuhinta
select vyohykemaara, kertalippuhinta, kausilippuhinta
from fact_lippuhinta
where vuosi = :vuosi and organisaatioid = :organisaatioid

-- name: insert-default-lippuhinta-if-not-exists!
insert into fact_lippuhinta (vuosi, vyohykemaara, organisaatioid)
  select :vuosi vuosi,
         column_value vyohykelukumaara,
         :organisaatioid organisaatioid
  from table(numbers(1,6))
  where not exists (
      select 1 from fact_lippuhinta l
      where l.vuosi = :vuosi and
            l.organisaatioid = :organisaatioid
  )
  order by vuosi, vyohykelukumaara, organisaatioid

-- name: select-lipputulo
select kuukausi, kertalipputulo, arvolipputulo, kausilipputulo, lipputulo
from fact_lipputulo
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: insert-default-lipputulo-if-not-exists!
insert into fact_lipputulo (vuosi, kuukausi, organisaatioid, sopimustyyppitunnus)
  select :vuosi vuosi,
         column_value kuukausi,
         :organisaatioid organisaatioid,
         :sopimustyyppitunnus sopimustyyppitunnus
  from table(numbers(1,12))
  where not exists (
      select 1 from fact_lipputulo l
      where l.vuosi = :vuosi and
            l.organisaatioid = :organisaatioid and
            l.sopimustyyppitunnus = :sopimustyyppitunnus
  )
  order by vuosi, kuukausi, organisaatioid, sopimustyyppitunnus

-- name: select-liikennointikorvaus
select kuukausi, korvaus, nousukorvaus, nousut, kuntakorvaus
from fact_liikennointikorvaus
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: insert-default-liikennointikorvaus-if-not-exists!
insert into fact_liikennointikorvaus (vuosi, kuukausi, organisaatioid, sopimustyyppitunnus)
  select :vuosi vuosi,
         column_value kuukausi,
         :organisaatioid organisaatioid,
         :sopimustyyppitunnus sopimustyyppitunnus
  from table(numbers(1,12))
  where not exists (
      select 1 from fact_liikennointikorvaus l
      where l.vuosi = :vuosi and
            l.organisaatioid = :organisaatioid and
            l.sopimustyyppitunnus = :sopimustyyppitunnus
  )
  order by vuosi, kuukausi, organisaatioid, sopimustyyppitunnus

-- name: select-alue
select
  kuntamaara,
  vyohykemaara,
  pysakkimaara,
  maapintaala,
  asukasmaara,
  tyopaikkamaara,
  henkilosto,
  
  pendeloivienosuus,
  henkiloautoliikennesuorite,
  autoistumisaste,
  asiakastyytyvaisyys,
  
  kustannus_asiakaspalvelu,
  kustannus_konsulttipalvelu,
  kustannus_lipunmyyntipalkkio,
  kustannus_jarjestelmat,
  kustannus_muutpalvelut,

  kommentti
from fact_alue where vuosi = :vuosi and organisaatioid = :organisaatioid

-- name: select-kommentti
select kommentti from tunnuslukukommentti
where vuosi = :vuosi and organisaatioid = :organisaatioid and sopimustyyppitunnus = :sopimustyyppitunnus

-- name: select-joukkoliikennetuki
select avustuskohdeluokkatunnus, tuki
from fact_joukkoliikennetuki
where vuosi = :vuosi and organisaatioid = :organisaatioid

-- name: insert-default-joukkoliikennetuki-if-not-exists!
insert into fact_joukkoliikennetuki (vuosi, organisaatioid, avustuskohdeluokkatunnus)
  select :vuosi vuosi,
         :organisaatioid organisaatioid,
         tunnus avustuskohdeluokkatunnus
  from avustuskohdeluokka
  where not exists (
      select 1 from fact_joukkoliikennetuki l
      where l.vuosi = :vuosi and
            l.organisaatioid = :organisaatioid
  )
  order by vuosi, organisaatioid, tunnus

-- name: select-tayttoaste-pisteet
select
  (select count(nousut) + count(lahdot) + count(linjakilometrit) from fact_liikenne
    where organisaatioid = :organisaatioid and vuosi = :vuosi) +
  (select count(nousut) + count(lahdot) + count(linjakilometrit) from fact_liikenneviikko
    where organisaatioid = :organisaatioid and vuosi = :vuosi) +
  (select count(korvaus) + count(nousukorvaus) + count(nousut) from fact_liikennointikorvaus
    where organisaatioid = :organisaatioid and vuosi = :vuosi) +
  (select count(kertalipputulo) + count(arvolipputulo) + count(kausilipputulo) + count(lipputulo) from fact_lipputulo
    where organisaatioid = :organisaatioid and vuosi = :vuosi) +
  (select count(tuki) from fact_joukkoliikennetuki
    where organisaatioid = :organisaatioid and vuosi = :vuosi) +
  (select decode(count(kertalippuhinta) + count(kausilippuhinta), 0, 0, 1) from fact_lippuhinta
    where organisaatioid = :organisaatioid and vuosi = :vuosi) +
  (select count(distinct sopimustyyppitunnus) from fact_kalusto
    where organisaatioid = :organisaatioid and vuosi = :vuosi and lukumaara is not null) +
  (select count(kuntamaara) + count(vyohykemaara) + count(pysakkimaara) +
          count(maapintaala) + count(asukasmaara) + count(tyopaikkamaara) +
          count(henkilosto) + count(pendeloivienosuus) + count(henkiloautoliikennesuorite) +
          count(autoistumisaste) + count(asiakastyytyvaisyys) +
          count(kustannus_asiakaspalvelu) + count(kustannus_konsulttipalvelu) + count(kustannus_lipunmyyntipalkkio) +
          count(kustannus_jarjestelmat) + count(kustannus_muutpalvelut) from fact_alue
   where organisaatioid = :organisaatioid and vuosi = :vuosi)
    as pisteet
from dual

-- name: select-all-tunnusluvut
select 
  tunnusluku.tunnus, 
  tunnusluku.organisaatioid, organisaatio.nimi organisaationimi,
  tunnusluku.vuosi, tunnusluku.kuukausi, 
  tunnusluku.tyyppi1, tunnusluku.tyyppi2, tunnusluku.arvo
from (
select 'nousut' tunnus, organisaatioid, vuosi, kuukausi, sopimustyyppitunnus tyyppi1, null tyyppi2, nousut arvo from fact_liikenne
union all
select 'lahdot', organisaatioid, vuosi, kuukausi, sopimustyyppitunnus, null, lahdot from fact_liikenne
union all
select 'linjakilometrit', organisaatioid, vuosi, kuukausi, sopimustyyppitunnus, null, linjakilometrit from fact_liikenne
union all
select 'nousut-viikko', organisaatioid, vuosi, null, sopimustyyppitunnus, viikonpaivaluokkatunnus, nousut from fact_liikenneviikko
union all
select 'lahdot-viikko', organisaatioid, vuosi, null, sopimustyyppitunnus, viikonpaivaluokkatunnus, lahdot from fact_liikenneviikko
union all
select 'linjakilometrit-viikko', organisaatioid, vuosi, null, sopimustyyppitunnus, viikonpaivaluokkatunnus, linjakilometrit from fact_liikenneviikko
union all
select 'liikennointikorvaus', organisaatioid, vuosi, kuukausi, sopimustyyppitunnus, 'K', korvaus from fact_liikennointikorvaus
union all
select 'liikennointikorvaus', organisaatioid, vuosi, kuukausi, sopimustyyppitunnus, 'NK', nousukorvaus from fact_liikennointikorvaus
union all
select 'liikennointikorvaus', organisaatioid, vuosi, kuukausi, sopimustyyppitunnus, 'KK', kuntakorvaus from fact_liikennointikorvaus
union all
select 'lipputulo', organisaatioid, vuosi, kuukausi, sopimustyyppitunnus, lipputuloluokkatunnus, tulo from fact_lipputulo_unpivot_view
union all
select 'kalusto', organisaatioid, vuosi, null, sopimustyyppitunnus, paastoluokkatunnus, lukumaara from fact_kalusto
union all
select 'kustannukset', organisaatioid, vuosi, null, kustannuslajitunnus, null, kustannus from fact_kustannus_unpivot_view
union all
select 'lippuhinnat', organisaatioid, vuosi, null, to_char(vyohykemaara), lippuhintaluokkatunnus, hinta from fact_lippuhinta_unpivot_view
union all
select 'alue-kuntamaara', organisaatioid, vuosi, null, null, null, kuntamaara from fact_alue
union all
select 'alue-vyohykemaara', organisaatioid, vuosi, null, null, null, vyohykemaara from fact_alue
union all
select 'alue-pysakkimaara', organisaatioid, vuosi, null, null, null, pysakkimaara from fact_alue
union all
select 'alue-maapintaala', organisaatioid, vuosi, null, null, null, maapintaala from fact_alue
union all
select 'alue-asukasmaara', organisaatioid, vuosi, null, null, null, asukasmaara from fact_alue
union all
select 'alue-henkilosto', organisaatioid, vuosi, null, null, null, henkilosto from fact_alue
union all
select 'alue-pendeloivienosuus', organisaatioid, vuosi, null, null, null, pendeloivienosuus from fact_alue
union all
select 'alue-henkiloautoliikennesuorite', organisaatioid, vuosi, null, null, null, henkiloautoliikennesuorite from fact_alue
union all
select 'alue-autoistumisaste', organisaatioid, vuosi, null, null, null, autoistumisaste from fact_alue
union all
select 'alue-asiakastyytyvaisyys', organisaatioid, vuosi, null, null, null, asiakastyytyvaisyys from fact_alue
) tunnusluku inner join organisaatio on organisaatio.id = tunnusluku.organisaatioid
where arvo is not null


