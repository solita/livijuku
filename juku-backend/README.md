Liikennevirasto - Juku palvelut
===============================

Tämä projekti sisältää Liikenneviraston Juku -järjestelmän palveluiden toteutuksen.

Kehityskäyttö
-------------

Leiningen build-työkalun asennus: http://leiningen.org/#install

Palvelimen käynnistys lokaalisti: **lein ring server-headless**

Palvelimen käynnistäminen edellyttää tietokannan
* oletus: jdbc:oracle:thin:@localhost:1521:orcl

Palvelujen rajapintadokumentaatio: http://localhost:3000/api/ui/index.html

Lokaalin tietokannan käyttäminen vagrantilla ks. ../juku-db/vagrant/README.md

Tuotantokäyttö
--------------

Tuotantokäyttö edellyttää java 1.8 kehitysympäristön.

Käynnistäminen: **java -jar juku.jar**

Asetukset: **juku.properties** tiedosto, joka pitää olla ohjelman käynnistyshakemistossa.

Testaus
-------

Midje testit ajetaan komennolla **lein midje**