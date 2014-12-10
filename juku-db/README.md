Juku tietokanta
===============

Tämä projekti sisältää tietokantapäivitykset Oracle Juku-tietokannan skeeman päivitykseen ja luontiin tyhjästä.

Päivitysten hallintaan käytetään dbmaintain työkalua: http://www.dbmaintain.org/overview.html

Tätä työkalua käytetään (leiningen asennustyökalulla): http://leiningen.org

Oletukset
---------

Päivitykset olettavat että tietokannassa on valmiina tietokantakäyttäjä: **juku**,
jolla tietokantaobjektit luodaan ja jolla on riittävät oikeudet muutosten tekemiseen.

Kaikki tietoa varaavat kantaobjektit luodaan juku-käyttäjän oletustaulualueeseen: **juku_data**.
Oletustaulualueen lisäksi tarvitaan taulualue indekseille: **juku_indx**.

Indeksitaulualue valitaan siten että sen nimi on juku%_indx, johon juku käyttäjällä on varattu tilaa (quota).

Tarvittavien taulualueiden luontiin löytyy esimerkit: users/tablespace.sql

Tarvittavien käyttäjien luontiin löytyy esimerkit: users/users.sql

Kehityskäyttö
-------------

Leiningen build-työkalun asennus: http://leiningen.org/#install

Tietokannan päivitys: **lein update-db**

Tietokannan tyhjentäminen: **lein clear-db**

Tyhjennys/päivitys: **lein do clear-db, update-db**

Ohjeet tietokantapalvelimen käyttämiseen vagrant-työkalulla löytyy: vagrant/README.md

Testidata: (test/sql/repeatable.test) on mahdollista lisätä tietokantaan
päivityksen yhteydessä käyttämällä profiilia **test-data**

**lein with-profiles +test-data do clear-db, update-db**

Tuotantoasennus
---------------

TODO