
-- Vanha tunnus, jota ei pitäisi enää näkyä missään.
insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
  values ('harri', 'Harri Henrik', 'Hakija', 1, 0);

-- Vanha tunnus, jota ei pitäisi enää näkyä missään.
insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
  values ('katri', 'Katri Karoliina', 'Käsittelijä', (select id from organisaatio where lajitunnus = 'LV'), 0);

-- roolikohtaiset testikäyttäjät livi-ympäristössä --
-- hakijaroolit: juku_hakija, juku_allekirjoittaja

insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
values ('juku_hakija', 'Harri', 'Hakija', 1, 0);

insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
values ('juku_allekirjoittaja', 'Allu', 'Allekirjoittaja', 1, 0);

-- käsittelijäroolit: juku_kasittelija, juku_paatoksentekija

insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
values ('juku_kasittelija', 'Katri', 'Käsittelijä', (select id from organisaatio where lajitunnus = 'LV'), 0);

insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
values ('juku_paatoksentekija', 'Päivi', 'Päätöksentekijä', (select id from organisaatio where lajitunnus = 'LV'), 0);

insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
values ('juku_paakayttaja', 'Jussi', 'Koskinen', (select id from organisaatio where lajitunnus = 'LV'), 0);
