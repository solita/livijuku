-- Proseduuri luonti/päivitystunnusten ja aikaleimojen päivittämiseen
create or replace procedure p_paivitaleimat 
 (i_oldpernimi in varchar2
 ,i_oldperpvm in date
 ,io_newpernimi in out varchar2
 ,io_newperpvm in out date
 ,o_newpainimi out varchar2
 ,o_newpaipvm out date
 )
 is
begin
declare
 m_painimi  varchar2(30);
 m_pvm   date    := sysdate;
 m_user  varchar2(30) := user;
 m_client_id varchar2(64);
begin
--select SYS_CONTEXT('userenv', 'CLIENT_IDENTIFIER') into m_client_id from dual;
m_client_id := sys_context('userenv', 'CLIENT_IDENTIFIER');
if m_client_id is not null 
 then m_painimi := m_client_id;
 else m_painimi := m_user;
end if;
if inserting then
 io_newpernimi := m_painimi;
 io_newperpvm := m_pvm;
 o_newpainimi := m_painimi;
 o_newpaipvm := m_pvm;
end if;
if updating then
 if io_newpernimi <> i_oldpernimi then
  io_newpernimi := i_oldpernimi;
 end if;
 if io_newperpvm <> i_oldperpvm then
  io_newperpvm := i_oldperpvm;
 end if;
 o_newpainimi := m_painimi;
 o_newpaipvm := m_pvm;
end if;
end;
end p_paivitaleimat;
/

-- Proseduuri luontitunnusten ja aikaleimojen asettamiseen
create or replace procedure p_luoleimat (
  io_newluontitunnus in out varchar2,
  io_newluontiaika in out date)
is
  begin
    declare
      m_client_id varchar2(64);
    begin
      m_client_id := sys_context('userenv', 'CLIENT_IDENTIFIER');
      io_newluontitunnus := nvl(m_client_id, user);
      io_newluontiaika := sysdate;
    end;
  end;
/

create table kieli ( 
   tunnus varchar2 (2 char) not null constraint kieli_pk primary key, 
   nimi varchar2 (200 char) not null
);

create table kayttaja ( 
  tunnus varchar2 (30 char)  not null constraint kayttaja_pk primary key,
  etunimi varchar2 (200 char),
  sukunimi varchar2 (200 char),
  nimi varchar2 (200 char),
  sahkoposti varchar2 (200 char),
  organisaatioid number,
  jarjestelma number (1) default 0 not null check ( jarjestelma in (0, 1))
);

begin
  model.define_mutable(model.new_entity('kayttaja', 'Käyttäjä', 'KA'));
  model.define_mutable(model.new_entity('kieli'));
end;
/

-- Tietokanta käyttäjä joka omistaa skeeman --
insert into kayttaja (tunnus, nimi, jarjestelma) values (user, user || ' tietokanta', 1);
insert into kayttaja (tunnus, nimi, jarjestelma) values (user || '_APP', user || ' sovellustietokantakäyttäjä', 1);

-- Tuetut kielet --
insert into kieli (tunnus, nimi) values ('fi', 'Suomi');
insert into kieli (tunnus, nimi) values ('sv', 'Ruotsi');