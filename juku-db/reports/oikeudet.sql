
-- käyttöoikeusmatriisi --
select * from (
select * from (
    select kayttooikeus.nimi "Käyttooikeus", kayttooikeus.tunnus "Tunnus", kayttajarooli.tunnus rooli, 1 arvo
    from kayttajarooli 
    inner join kayttajaroolioikeus on 
        kayttajarooli.tunnus = kayttajaroolioikeus.kayttajaroolitunnus
    inner join kayttooikeus on
        kayttooikeus.tunnus = kayttajaroolioikeus.kayttooikeustunnus
)
pivot    (
    count(arvo)
    for rooli
    in ('HA' as    "Hakija",
            'AK' as    "Allekirjoittaja",
            'KA' as    "Käsittelijä",
            'PA' as    "Päätöksentekijä",
            'PK' as    "Pääkäyttäjä")
)
) order by "Hakija" desc, "Allekirjoittaja" desc, "Käsittelijä" desc, "Päätöksentekijä" desc, "Pääkäyttäjä" desc
;
