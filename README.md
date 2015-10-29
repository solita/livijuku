LiviJuku
========

Liikennevirasto Joukkoliikenteen rahoitus-, kustannus- ja suoritetietojen keräys- ja seurantajärjestelmä

## Versiohallinta


Versiontikäytännöt perustuvat malliin: [http://nvie.com/posts/a-successful-git-branching-model/](http://nvie.com/posts/a-successful-git-branching-model/)

Kehitysympäristön käynnistäminen
--------------------------------

Asenna java, virtualbox, vagrant, leiningen.

Käynnistä [tietokanta](/juku-db/vagrant)

    cd juku-db/vagrant
    less README.md

Luo [skeema](/juku-db) tietokantaan

    cd juku-db
	lein with-profiles +test-data do clear-db, update-db

Käynnistä [backend-palvelu](/juku-backend)

    cd juku-backend
    lein ring server-headless


