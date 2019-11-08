LiviJuku
========

Liikennevirasto Joukkoliikenteen rahoitus-, kustannus- ja suoritetietojen keräys- ja seurantajärjestelmä

Jukun käyttöliittymät
----------------------

Viranomaiskäyttöliittymä: https://github.com/solita/livijuku-front

Julkinen käyttöliittymä: https://github.com/solita/livijuku-public-front

Kehitysympäristön käynnistäminen
--------------------------------

Asenna java, docker ja leiningen.

Käynnistä [tietokanta](/juku-db/docker)

    cd juku-db/docker
    less README.md

Luo [skeema](/juku-db) tietokantaan

    cd juku-db
	lein with-profiles +test-data do clear-db, update-db

Käynnistä [backend-palvelu](/juku-backend)

    cd juku-backend
    lein ring server-headless



