-- roolikohtaiset testikäyttäjät livi-ympäristössä --
-- hakijaroolit: juku_hakija, juku_allekirjoittaja

--************************************************************
-- Kehitysympäristän OAM-testitunnusten mukaiset tunnukset   *
--************************************************************

-- HSL, KS1
insert into kayttaja (tunnus, etunimi, sukunimi, sahkoposti, organisaatioid, jarjestelma)
values ('juku_hakija', 'Harri', 'Helsinki', 'petri.sirkkala@solita.fi', (select id from organisaatio where nimi = 'Helsingin seudun liikenne'), 0);
insert into kayttaja (tunnus, etunimi, sukunimi, sahkoposti, organisaatioid, jarjestelma)
values ('juku_allekirjoittaja', 'Allu', 'Allekirjoittaja', 'petri.sirkkala@solita.fi', (select id from organisaatio where nimi = 'Helsingin seudun liikenne'), 0);

-- Tampere, KS1
insert into kayttaja (tunnus, etunimi, sukunimi, sahkoposti, organisaatioid, jarjestelma)
values ('juku_hakija_tampere', 'Tatu', 'Tampere', 'petri.sirkkala@solita.fi', (select id from organisaatio where nimi = 'Tampere'), 0);
-- Oulu, KS1
insert into kayttaja (tunnus, etunimi, sukunimi, sahkoposti, organisaatioid, jarjestelma)
values ('juku_hakija_oulu', 'Olli', 'Oulu', 'petri.sirkkala@solita.fi', (select id from organisaatio where nimi = 'Oulu'), 0);

-- Pori, KS2
insert into kayttaja (tunnus, etunimi, sukunimi, sahkoposti, organisaatioid, jarjestelma)
values ('juku_hakija_pori', 'Pekka', 'Pori', 'petri.sirkkala@solita.fi', (select id from organisaatio where nimi = 'Pori'), 0);
-- Kotka, KS2
insert into kayttaja (tunnus, etunimi, sukunimi, sahkoposti, organisaatioid, jarjestelma)
values ('juku_hakija_kotka', 'Kalle', 'Kotka', 'petri.sirkkala@solita.fi', (select id from organisaatio where nimi = 'Kotka'), 0);

-- Etelä-Pohjanmaa, ELY
insert into kayttaja (tunnus, etunimi, sukunimi, sahkoposti, organisaatioid, jarjestelma)
values ('juku_hakija_epo', 'Ellu', 'Etelä-Pohjanmaa', 'petri.sirkkala@solita.fi', (select id from organisaatio where nimi = 'Etelä-Pohjanmaa'), 0);
-- Lappi, ELY
insert into kayttaja (tunnus, etunimi, sukunimi, sahkoposti, organisaatioid, jarjestelma)
values ('juku_hakija_lappi', 'Liisa', 'Lappi', 'petri.sirkkala@solita.fi', (select id from organisaatio where nimi = 'Lappi'), 0);


-- käsittelijäroolit: juku_kasittelija, juku_paatoksentekija, LV
insert into kayttaja (tunnus, etunimi, sukunimi, sahkoposti, organisaatioid, jarjestelma)
values ('juku_kasittelija', 'Katri', 'Käsittelijä', 'petri.sirkkala@solita.fi', (select id from organisaatio where lajitunnus = 'LV'), 0);

insert into kayttaja (tunnus, etunimi, sukunimi, sahkoposti, organisaatioid, jarjestelma)
values ('juku_paatoksentekija', 'Päivi', 'Päätöksentekijä', 'petri.sirkkala@solita.fi', (select id from organisaatio where lajitunnus = 'LV'), 0);

-- pääkäyttäjä, LV
insert into kayttaja (tunnus, etunimi, sukunimi, sahkoposti, organisaatioid, jarjestelma)
values ('juku_paakayttaja', 'Jussi', 'Koskinen', 'petri.sirkkala@solita.fi', (select id from organisaatio where lajitunnus = 'LV'), 0);

insert into kayttajakayttajarooli (kayttajatunnus, kayttajaroolitunnus)
select tunnus, 'HA' from kayttaja where tunnus like '%hakija%';

insert into kayttajakayttajarooli (kayttajatunnus, kayttajaroolitunnus) values ('juku_allekirjoittaja', 'AK');
insert into kayttajakayttajarooli (kayttajatunnus, kayttajaroolitunnus) values ('juku_kasittelija', 'KA');
insert into kayttajakayttajarooli (kayttajatunnus, kayttajaroolitunnus) values ('juku_paatoksentekija', 'PA');
insert into kayttajakayttajarooli (kayttajatunnus, kayttajaroolitunnus) values ('juku_paakayttaja', 'PK');


--*******************************************************************
-- END OF Kehitysympäristön OAM-testitunnusten mukaiset tunnukset   *
--*******************************************************************
