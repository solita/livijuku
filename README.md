LiviJuku
========

Liikennevirasto Joukkoliikenteen rahoitus-, kustannus- ja suoritetietojen keräys- ja seurantajärjestelmä

## Git branching model
Tarkoituksena on käyttää Branching modelia: [http://nvie.com/posts/a-successful-git-branching-model/](http://nvie.com/posts/a-successful-git-branching-model/)

Kehitysympäristön käynnistäminen
--------------------------------

Asenna clojure, leiningen, vagrant.

Käynnistä tietokanta

    cd livijuku/juku-db/vagrant
    less README.md

Käynnistä backend palvelu

    lein ring server


