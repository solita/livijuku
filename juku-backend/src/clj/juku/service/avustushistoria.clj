(ns juku.service.avustushistoria
  (:require [common.map :as m]))

(def avustus-data-2010-2015
  "Avustushistoria sisältää aikaisempien vuosien avustustiedon vuositasolla.
  Avustushistoriassa on vuosien 2010 - 2015 haetut ja myönnetyt avustukset organisaatiolajeille: KS1, KS2 ja
  ELY:jen tapauksessa on myös mukana vuosi 2016.

  Haetut ja myönnetyt rahamäärät ovat omissa taulukoissa, jossa yksi rivi kuvaa tietyn organisaation vuosittaisia
  haettuja tai myönnettyjä rahamääriä. Sarake sisältää siis tiedot ko. lajin organisaatioista tietyltä vuodelta.

  Taulukot on lisäksi ryhmitelty organisaatiolajeittain.
  Organisaatiot tieto kuvaa mitä organisaatioita on tietyssä lajissa ja lisäksi se kuvaa rivien järjestyksen,
  jossa organisaatiokohtaiset rivit ovat taulukoissa."

  {
  "KS1" {
      ;; Oulu, HSL, Tampere, Turku
      :organisaatiot
      [10, 1, 12, 13],
      :haetut
      (partition 6
                 [320500  790000  988990  2653000 3649759 5600000
                  5213000 6230000 5553495 8000000 8015000 5540000
                  998530  1990872 2265000 3355000 3430000 3358000
                  831311  989371  1457016 3357290 4400000 6875000])
      :myonnetyt
      (partition 6
                 [320500  790000  913990  1314775 1751255 1339000
                  5213309 6229319 5553495 5821548 6096802 4700000
                  998530  1990872 1751951 2538098 2805226 2129000
                  831311  989371  1457016 2377717 2069717 1582000])}

  "KS2" {
      ;; Hämeenlinna, Joensuu, Jyväskylä, Kotka, Kouvola, Kuopio, Lahti, Lappeenranta, Pori, Vaasa
      :organisaatiot
      [2, 3, 4, 5, 6, 7, 8, 9, 11, 14],
      :haetut
      (partition 6
                 [1359492 1477181 1592264 1693099 1459000 1621130
                  363542  437110  540702  539238  741300  972500
                  1990000 1965000 1951500 2041050 5059250 1399000
                  1180000 1111000 1174000 977000  1278000 1358000
                  646800  966500  1123441 982000  1069000 1064600
                  823313  1135340 1059000 1249100 1249100 1336500
                  2187624 2389200 2110350 2679170 6775500 3873300
                  700000  754330  838045  662160  1108829 1211000
                  1180419 1323489 1375860 1622061 1439534 1483436
                  732526  839377  914610  1031826 1281000 1250717])
      :myonnetyt
      (partition 6
                 [838383  951391 1073901 985000 985000 946000
                  338622  377650 457208 515000 515000 505000
                  1294000 1266067 1115100 1238000 1238000 1213000
                  864739  703223 702635 775000 775000 728000
                  560004  625653 500230 555000 555000 544000
                  722005  834432 1152741 1249100 1249100 1174000
                  1009696 1074844 1228289 1380000 1380000 1352000
                  479562  567670 558468 662160 662160 636000
                  959244  960700 997200 1162000 1162000 1092000
                  490000  482000 350000 395000 395000 390000])}

  "ELY" {
      ;; Pohjois-Pohjanmaa, Pohjois-Savo, Varsinais-Suomi, Uudenmaa, Etelä-Pohjanmaa, Kaakkois-Suomi, Keski-Suomi, Lappi, Pirkanmaa
      :organisaatiot
      [23, 20, 17, 16, 22, 18, 21, 24, 19],
      :haetut
      (partition 7
                 [4726300 6190000  5740000 6026000  0 5083361 4272000
                  8946000 10225000 9525222 7826094  0 6775000 6750000
                  5044972 5885000  6900000 6985000  0 2980000 2620000
                  5949922 8301474  11100000 6461517 0 9590918 11720374
                  3589635 4308816  4340000 4113803  0 3664548 4208284
                  3940058 4419818  5440000 1450669  0 1705803 1331000
                  3425700 4405000  3960000 2535000  0 1764000 1903150
                  3416000 3980000  3990000 4165000  0 3987000 3737782
                  2988258 4392405  4120000 3762000  0 2415000 2744314])
      :myonnetyt
      (partition 7
                 [4577000 4423000 4209000 4100000 3879000 3986000 3924000
                  8653000 8594000 8250000 6900000 6695000 6420000 6325000
                  4756000 4802000 4803000 2800000 2586000 2557000 2702000
                  6272000 6225000 6303000 4600000 4569000 4818000 5147000
                  3264000 3579000 3507000 3450000 3192000 3156000 3551000
                  3539000 3662000 3457000 1450000 1365000 1350000 1301000
                  3186000 3183000 3003000 1900000 1684000 1590000 1760000
                  3284000 3293000 3324000 3350000 3138000 3053000 3086000
                  3073000 3036000 3141000 2200000 1985000 1963000 2033000])}})

(def vuodet (range 2010 2017))

(def avustus-data+all
  (assoc avustus-data-2010-2015 "ALL" (apply m/deep-merge-with #(apply concat %) (vals avustus-data-2010-2015))))
