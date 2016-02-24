/* 
  Metamodel schema 
  -----------------
  Metamodel contains additional information about entities for model schema generation
  
*/

create table entity ( 
  table_name varchar2 (30 char) not null constraint entity_pk primary key, 
  entityname varchar2 (200 char) not null, 
  abbreviation varchar2 (200 char),
  
  constraint entity_abbreviation_u unique ( abbreviation )
);

create table entitytype ( 
   entitytypename varchar2 (200 char) not null constraint entitytype_pk primary key, 
   name varchar2 (200 char), 
   description varchar2 (2000 char)
);

create table entitytypeentity ( 
   entitytypename varchar2 (200 char) not null constraint entitytypeentity_type references entitytype (entitytypename),
   table_name varchar2 (30 char) not null constraint entitytypeentity_entity references entity (table_name),
   constraint entitytypeentity_pk primary key ( entitytypename, table_name ) 
);

-- Entity type definitions --

-- Localizable entity type:
insert into entitytype values ('localizable', 'Lokalisoitava', 'Lokalisoitavalla käsiteellä on lokalisoitu nimi.');

-- Localizable name entity type:
insert into entitytype values ('localized-name', 'Lokalisoitu nimi', 'Lokalisoidun käsitteen lokalisoitu nimi.');

-- Classification entity type:
insert into entitytype values ('class', 'Luokittelu', 'Luokittelu-taulut on tarkoitettu tiedon luokitteluun.');

-- State entity type:
insert into entitytype values ('state', 'Tila', 'Tila-käsitteellä ilmaistaan jonkun toisen käsitteen tai prosessin tilaa.');

-- Immutable entity type:
insert into entitytype values ('immutable', 'Ei muokattava', 'Nämä käsitteet eivät ole muokattavissa. Update-lausekkeet eivät ole sallittuja sovellustietokantakäyttäjille.');

-- Mutable entity type:
insert into entitytype values ('mutable', 'Muokattava', 'Nämä käsitteet ovat muokattavissa. Update-lausekkeet ovat sallittuja sovellustietokantakäyttäjille.');

-- Datetemporal entity type:
insert into entitytype values ('date-temporal', 'Aikaehdollinen', 'Näiden käsitteiden voimassaolo määritetään aikaehdolla.');







