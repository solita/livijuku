Dokumenttipohjat
----------------

Tämä hakemisto sisältää hakemus- ja päätösdokumenttien luonnissa käytettävät dokumenttipohjat. 
Dokumenttipohja kuvaa dokumentin staattisen osan, johon dokumenttia muodostettaessa liitetään dynaamiset tiedot.
Dokumenttipohjan dynaamiset osat ovat muotoa {tiedonnimi} esim. {vireillepvm}. Tämä kohta korvataan ko. tiedolla dokumentin muodostuksessa.

Pohjien nimeämiskäytäntö on seuraava: [dokumenttityyppi]-[hakemustyyppi]-[voimaantulovuosi].txt

Avustushakemuksen päätöksen tapauksessa: paatos-ah0-[organisaatiolaji]-[voimaantulovuosi].txt

Dokumenttityyppejä ovat hakemus tai päätös.

Hakemustyyppejä ovat:
* **ah0** – avustushakemus
* **mh1** – 1. maksatushakemus
* **mh2** – 2. maksatushakemus

Voimaantulovuosi tarkoittaa hakemuskautta, jolloin ko. pohja astuu voimaan. Pohja on niin kauan voimassa kunnes seuraava pohja korvaa sen.

Organisaatiolajit ovat:
* **ks1** - Suuri kaupunkiseutu
* **ks2** - Keskisuuri kaupunkiseutu

Dokumenttipohjan tiedostomuoto on utf-8 (ilman BOM-merkintää) ja rivinvaihtomerkkeinä käytetään linefeed-merkkiä unix-käytännön mukaisesti.

Dokumenttipohja tukee hyvin yksinkertaista tekstin muotoilua:
* Lihavointi - Lihavoiturivi aloitetaan *-merkillä. Yksittäistä sanaa ei voi lihavoida.
* Automaattinen rivitys - Kappaleen automaattinen rivittäminen. Kappaleet erotetaan toisistaan rivinvaihtomerkillä. 
* Sisennys - Sisennys tehdään tabulaattori-merkillä. Yksi tabulaattori sisentää aina vakiopituuden.
 
