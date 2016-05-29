
begin
  model.new_classification('sopimusmalli', 'Sopimusmalli', 4, 'SOMALLI');
end;
/

insert into sopimusmalli (tunnus, nimi, jarjestys) values ('BR', 'Bruttomalli', 1);
insert into sopimusmalli (tunnus, nimi, jarjestys) values ('KK', 'Kysyntäkannustemalli', 2);
insert into sopimusmalli (tunnus, nimi, jarjestys) values ('KOS1', 'Käyttöoikeussopimus (alue)', 3);
insert into sopimusmalli (tunnus, nimi, jarjestys) values ('KOS2', 'Käyttöoikeussopimus (reitti)', 4);

alter table kilpailutus add sopimusmallitunnus constraint kilpailutus_sopimusmalli_fk references sopimusmalli (tunnus);
