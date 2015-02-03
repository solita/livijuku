
insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
  values ('harri', 'Harri', 'Hakija', 1, 0);
  
insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
  values ('katri', 'Katri', 'Käsittelijä', (select id from organisaatio where lajitunnus = 'LV'), 0);

-- roolikohtaiset testikäyttäjät livi-ympäristössä --
-- hakijaroolit: juku_hakija, juku_allekirjoittaja

insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
values ('juku_hakija', 'Tommi', 'Rantanen', 1, 0);

insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
values ('juku_allekirjoittaja', 'Kimmo', 'Rantala', 1, 0);

-- käsittelijäroolit: juku_kasittelija, juku_paatoksentekija

insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
values ('juku_kasittelija', 'Tapio', 'Saarela', (select id from organisaatio where lajitunnus = 'LV'), 0);

insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
values ('juku_paatoksentekija', 'Riikka', 'Taiminen', (select id from organisaatio where lajitunnus = 'LV'), 0);

insert into kayttaja (tunnus, etunimi, sukunimi, organisaatioid, jarjestelma)
values ('juku_paakayttaja', 'Jussi', 'Koskinen', (select id from organisaatio where lajitunnus = 'LV'), 0);