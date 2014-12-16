LiviJuku
========

Liikennevirasto Joukkoliikenteen rahoitus-, kustannus- ja suoritetietojen keräys- ja seurantajärjestelmä

## Git branching model
Tarkoituksena on käyttää Branching modelia: [http://nvie.com/posts/a-successful-git-branching-model/](http://nvie.com/posts/a-successful-git-branching-model/)

Kehitysympäristön käynnistäminen
--------------------------------

Asenna clojure, leiningen, vagrant.

Käynnistä [tietokanta](/solita/livijuku/tree/develop/juku-db/vagrant)

    cd juku-db/vagrant
    less README.md

Luo [skeema](/solita/livijuku/tree/develop/juku-db) tietokantaan

    cd juku-db
    less README.md

Käynnistä [backend-palvelu](/solita/livijuku/tree/develop/juku-backend)

    cd juku-backend
    less README.md


