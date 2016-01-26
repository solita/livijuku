
insert into maararahatarvetyyppi (tunnus, nimi, jarjestys) values ('M', 'Muu PSA:n mukainen liikenne', 4);
insert into maararahatarvetyyppi (tunnus, nimi, jarjestys) values ('HK', 'Hintavelvoitteiden korvaaminen', 5);

-- Ely hakemuksen perustiedot --

alter table hakemus drop (ely_siirtymaaikasopimukset, ely_joukkoliikennetukikunnat);

alter table hakemus add (
  ely_kaupunkilipputuki number(12,2),
  ely_seutulipputuki number(12,2),
  ely_ostot number(12,2),
  ely_kehittaminen number(12,2)
);