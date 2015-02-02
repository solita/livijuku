
/**
 * Mallipaketin rajapinta
 * ----------------------
 *
 * Metamalli sisältää ylimääräistä metatietoa tietokannan tauluista.
 * Tietoa käytetän dokumentoitiin ja tietokannan rakenteiden automaattiseen generointiin.
 * Metamallin skeema määritetään metamodel.sql tiedostossa.
 *
 * Tämä paketti sisältää yleiset palvelut ko. metatiedon ylläpitämiseen 
 * ja yleisten tietokantarakenteiden generointiin.
 */
create or replace package model authid current_user as

  /**
   * Luo uuden entiteetin malliin. Entiteettitiedot voidaan syöttää ennen tai jälkeen taulun luontia - luontijärjestyksellä ei ole väliä.
   */
  function new_entity (tablename varchar2, entityname varchar2 default null, abbreviation varchar2 default null) return entity%rowtype;

  /**
   * Hakee olemassaolevan entiteetin tiedot kannasta.
   */
  function get_entity (tablename varchar2) return entity%rowtype;

  /**
   * Määrittää ko. käsitteen aikaehdolliseksi. Tauluun lisätään aikaehdollisuuteen liittyvät kentät.
   */
  procedure define_datetemporal(e entity%rowtype);

  /**
   * Määrittää ko. käsitteelle lokalisoitavan nimen. Lisää ko. käsitteelle nimi-taulun, jossa lokalisoidut nimet ovat.
   */
  procedure define_localizable(e entity%rowtype);

  /**
   * Määrittää ko. käsitteen päivitettäväksi. Tauluun lisätään luonti- ja muokkauskirjanpitotiedot.
   */
  procedure define_mutable(e entity%rowtype);

  /**
   * Luo uuden luokittelun. Tämä palvelu luo sekä taulut ja niihin liittyvän metatiedon malliin.
   */
  procedure new_classification (tablename varchar2, entityname varchar2 default null, tunnuksenpituus number default 2, abbreviation varchar2 default null);

  /**
   * Lisää rajoitteen tietokantaan. Rajoitteen nimen muodostamisessa käytetään yleistä nimeämiskäytäntöä. 
   */
  procedure add_constraint(e entity%rowtype, constraint_name varchar2, expression varchar2, postfix varchar2 default 'chk');
  
  /**
   * Lisää kommentin sarakkeelle. 
   */
  procedure comment_column(table_name varchar2, column_name varchar2, comment_txt varchar2);

  /**
   * Antaa käsittteelle nimen, joka lyhyempi kuin annettu maksimipituus. Käsitteen nimeksi tulee koko nimi tai lyhenne.
   */
  function select_name(max_length number, entity entity%rowtype) return varchar2 deterministic;
  
  /**
   * Dryrun-tilassa kaikki sql-lausekkeet tulostetaan dbms_output:iin ja niitä ei suoriteta.
   * Dryrun on tarkoitettu vikojen tutkimiseen.
   */
  procedure set_dryrun(val boolean);
  
  /**
   * Suorittaa annetun lausekkeen tai tulostaa ko. lausekkeen dbms_output:iin
   */
  procedure putline_or_execute(s varchar2);
  
  /**
   * Poistaa rivinvaihdot kommentista.
   */
  function oneline(doc varchar2) return varchar2 deterministic;
  
  /**
   * Tämä funktio tuottaa create table -lausekkeessa käytettävän 
   * tyyppimäärityksen annetuilla parametreilla.
   */
  function datatype_definition(
    data_type varchar2,
    data_precision number,
    data_scale number,
    char_used varchar2,
    data_length number,
    char_length number) 
  return varchar2 deterministic;
  
  procedure rename_fk_constraints(e entity%rowtype);
end model;
/

/**
 * Mallipaketin toteutus
 */
create or replace package body model as
  dryrun boolean := false;
  
  procedure set_dryrun(val boolean) is
  begin
    dryrun := val;
  end;
  
  function oneline(doc varchar2) 
  return varchar2 deterministic is
  begin
    return translate(doc, '_' || chr(10), '_');
  end;
  
  procedure putline_or_execute(s varchar2) is
  begin
    if dryrun = false
    then
      begin
         execute immediate s;
      exception when others then
         dbms_output.put_line('FAILED: '|| s);
         raise;
      end;
    else dbms_output.put_line(s ||';'|| chr(10));
    end if;
  end putline_or_execute;
  
  function datatype_definition(
    data_type varchar2,
    data_precision number,
    data_scale number,
    char_used varchar2,
    data_length number,
    char_length number) 
  return varchar2 deterministic is
  begin
    return data_type ||
      case when data_precision is not null 
        then '(' || data_precision || ',' || data_scale || ')' end ||
      case when char_used = 'C' 
        then '(' || char_length || ' char)' end || 
      case when char_used = 'B' 
        then '(' || char_length || ')' end
      ;
  end datatype_definition;

  procedure create_localization_table(classification entity%rowtype) as
    localization_name constant entity%rowtype := new_entity(
      select_name(25, classification) || 'KIELI',
      initcap(classification.entityname) || ' lokalisoitu nimi',
      classification.abbreviation || 'KIELI');

    primarykeys varchar2(2000 char);
    primarykeys_and_types varchar2(2000 char);
    create_table varchar2(2000 char);
  begin

    select
      wm_concat(cc.column_name),
      wm_concat(cc.column_name || ' ' ||
        datatype_definition(tc.data_type, tc.data_precision,
                            tc.data_scale, tc.char_used,
                            tc.data_length, tc.char_length)
      )
      into primarykeys, primarykeys_and_types

    from user_constraints c, user_cons_columns cc, user_tab_columns tc
    where cc.constraint_name = c.constraint_name
      and tc.table_name=cc.table_name
      and tc.column_name=cc.column_name
      and c.constraint_type = 'P'
      and c.table_name = classification.table_name;

    -- lokalisoidun nimi -taulun (kielitaulut) luontilauseke
    create_table :=
    'create table ' || localization_name.table_name || ' ( '||
      'kielitunnus constraint ' || select_name(25, localization_name) || '_K_FK references kieli, ' ||
      primarykeys_and_types || ', ' ||
      'nimi varchar2(200 char) constraint ' || select_name(22, localization_name) || '_NIMI_NN not null, '||
      'constraint '|| select_name(27, localization_name) || '_PK primary key ('|| primarykeys ||', KIELITUNNUS), ' ||
      'constraint '|| select_name(25, localization_name) || '_L_FK foreign key ('|| primarykeys ||') references ' || classification.table_name || '('|| primarykeys ||')'||
    ') organization index compress 1';

    putline_or_execute(create_table);
    insert into entitytypeentity (table_name, entitytypename) values (localization_name.table_name, 'localized-name');
    
    define_mutable(localization_name);
  end;

  function get_entity (tablename varchar2) return entity%rowtype as
    e entity%rowtype;
  begin
    select * into e from entity where table_name = tablename;
    return e;
  exception
    when NO_DATA_FOUND then
      raise_application_error(-20000,
        'Entity: "' || tablename || '" does not exists.');
  end;

  procedure add_constraint(e entity%rowtype, constraint_name varchar2, expression varchar2, postfix varchar2 default 'chk') as
    tablename constant varchar2(30 char) := select_name(30 - length(constraint_name) - length(postfix) - 1, e);
  begin
    putline_or_execute(
      'alter table '|| e.table_name ||' add constraint ' ||
        tablename || '_' || constraint_name || '_' || postfix ||
        ' check (' || expression || ') initially immediate enable validate');
  end;

  procedure comment_column(table_name varchar2, column_name varchar2, comment_txt varchar2) as
  begin
    putline_or_execute('comment on column ' || table_name || '.' || column_name || ' is ''' || comment_txt || '''');
  end;

  function new_entity(tablename varchar2, entityname varchar2 default null, abbreviation varchar2 default null) return entity%rowtype as
    created_entity entity%rowtype;
  begin
    created_entity.table_name := upper(tablename);
    created_entity.entityname := nvl(entityname, initcap(tablename));
    created_entity.abbreviation := upper(nvl(abbreviation, tablename));

    insert into entity values created_entity;
    return created_entity;
  end;

  procedure define_datetemporal(e entity%rowtype) as
    tablename constant varchar2(20 char) := select_name(20, e);
  begin
    insert into entitytypeentity (table_name, entitytypename) values (e.table_name, 'date-temporal');

    putline_or_execute(
    'alter table ' || e.table_name || ' add (' ||
        'VOIMASSAALKUPVM DATE DEFAULT to_date(''01011600'',''DDMMYYYY'') constraint ' || tablename || '_VAPVM_NN NOT NULL ,' ||
        'VOIMASSALOPPUPVM DATE DEFAULT to_date(''01019999'',''DDMMYYYY'')  constraint ' || tablename || '_VLPVM_NN NOT NULL)');

    -- Voimassaolon alkupäivämäärä on annettu päivämäärän tarkkuudella
    add_constraint(e, 'VAD', 'TRUNC(VOIMASSAALKUPVM)=VOIMASSAALKUPVM');
    -- Voimassaolon loppupäivämäärä on annettu päivämäärän tarkkuudella
    add_constraint(e, 'VLD', 'TRUNC(VOIMASSALOPPUPVM)=VOIMASSALOPPUPVM');

    add_constraint(e, 'VAAA', 'VOIMASSAALKUPVM >= TO_DATE(''01011600'',''DDMMYYYY'')');
    add_constraint(e, 'VLAA', 'VOIMASSALOPPUPVM >= TO_DATE(''01011600'',''DDMMYYYY'')');

    comment_column(e.table_name, 'VOIMASSAALKUPVM',
      oneline(
'Voimassaolon alkupäivämäärä (inklusiivinen). Tästä päivästä alkaen tämä käsite on voimassa.'));

    comment_column(e.table_name, 'VOIMASSALOPPUPVM',
      oneline(
'Voimassaolon loppupäivämäärä (eksklusiivinen). Tästä päivästä alkaen tämä käsite ei ole enää voimassa.'));

  end;

  procedure define_localizable(e entity%rowtype) as
  begin
    insert into entitytypeentity (table_name, entitytypename) values (e.table_name, 'localizable');
    create_localization_table(e);
  end;

  procedure define_mutable(e entity%rowtype) as
    shortname constant varchar2(20 char) := select_name(20, e);
  begin
    insert into entitytypeentity (table_name, entitytypename) values (e.table_name, 'mutable');
    putline_or_execute(
    'alter table ' || e.table_name || ' add ('||
        'luontitunnus varchar2(30 char) constraint ' || shortname
            || '_LTNNUS_NN not null constraint ' || shortname || '_A_FK REFERENCES KAYTTAJA, ' ||
        'luontiaika date constraint ' || shortname || '_LAIKA_NN not null, ' ||
        'muokkaustunnus varchar2(30 char) constraint '|| shortname
            || '_MTNNUS_NN not null constraint '|| shortname ||'_E_FK REFERENCES KAYTTAJA, ' ||
        'muokkausaika date constraint ' || shortname || '_MAIKA_NN not null)');

    comment_column(e.table_name, 'luontitunnus', 'Käyttäjä, joka on lisännyt (insert) tämän rivin tietokantaan.');
    comment_column(e.table_name, 'luontiaika', 'Ajanhetki, jolloin tämä rivi on lisätty (insert) tietokantaan.');
    comment_column(e.table_name, 'muokkaustunnus', 'Käyttäjä, joka on viimeksi päivittänyt (update) tätä riviä tietokannassa.');
    comment_column(e.table_name, 'muokkausaika', 'Ajanhetki, jolloin tätä riviä on viimeksi päivitetty (update) tietokannassa');

    putline_or_execute(
    'CREATE OR REPLACE TRIGGER ' || shortname || 'BRIU' || chr(10) ||
          ' BEFORE INSERT OR UPDATE ON ' || e.table_name || ' FOR EACH ROW' || chr(10) ||
          'BEGIN' || chr(10) ||
          ' p_paivitaLeimat(:old.luontitunnus, :old.luontiaika, :new.luontitunnus, :new.luontiaika, :new.muokkaustunnus, :new.muokkausaika);' || CHR(10) ||
          'END;');

  end;

  procedure new_classification (tablename varchar2, entityname varchar2 default null, tunnuksenpituus number default 2, abbreviation varchar2 default null) as

    classification constant entity%rowtype := new_entity(tablename, entityname, abbreviation);

    shortname constant varchar2(20 char) := select_name(20, classification);
    tunnus constant varchar2(30 char) := 'TUNNUS';
    tunnus_declaration constant varchar2(2000 char) := tunnus || ' varchar2 (' || tunnuksenpituus || ' char) not null';

    create_table_stm constant varchar2(2000 char) :=
      'create table ' || tablename || ' ( ' ||
         tunnus_declaration || ', ' ||
         'nimi varchar2 (200 char), ' ||
         'kuvaus varchar2 (2000 char), ' ||
         'constraint ' || tablename || '_PK primary key (' || tunnus || ') ' ||
      ') logging';

  begin
    putline_or_execute(create_table_stm);

    define_datetemporal(classification);
    --define_localizable(classification);
    define_mutable(classification);
  end;

  -- max 20 characters tablename or abbreviation
  function select_name(max_length number, entity entity%rowtype)
      return varchar2 deterministic is
    begin
      return case when length(entity.table_name) > max_length
                  then entity.abbreviation
                  else entity.table_name end;
    end select_name;
  
  function first_valid_oracle_name(n1 varchar2, n2 varchar2, n3 varchar2 default null) return varchar2 deterministic is
    objectname constant varchar2(30 char) := 
            case when n1 is not null and length(n1) <= 30 then n1
                when n2 is not null and length(n2) <= 30 then n2
                when n3 is not null and length(n3) <= 30 then n3
            end;
  begin
    if objectname is null then raise_application_error(-20000, 'Mikään annetuista nimistä ei ole sopiva nimi oracle objektille: ' || n1 || ', ' || n2 || ', ' || n3); end if;
    return objectname;
  end;
  
  procedure rename_fk_constraints(e entity%rowtype) as
  begin
    for i in (
      select 
            gc.constraint_name, 
            source.table_name || '_' || target.table_name || '_FK' n1,
            source.table_name || '_' || target.abbreviation || '_FK' n2,
            source.abbreviation || '_' || target.abbreviation || '_FK' n3
        from user_constraints gc
        inner join user_constraints rc on gc.r_constraint_name = rc.constraint_name
        inner join entity source on source.table_name = gc.table_name
        inner join entity target on target.table_name = rc.table_name
      where gc.generated = 'GENERATED NAME' and gc.table_name = e.table_name
    )
    loop
      putline_or_execute('alter table ' || e.table_name || ' rename constraint ' || i.constraint_name || ' to ' || first_valid_oracle_name(i.n1, i.n2, i.n3));
    end loop;
  end;
end model;
/