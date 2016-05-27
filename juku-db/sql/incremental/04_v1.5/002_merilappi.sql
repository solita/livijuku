
-- Kemin TVV-alue laajeni ja siihen kuuluu Kemin, Keminmaan, Simon, Tervolan ja Tornion kunnat.
-- Kemin toimivaltaisen viranomaisen nimi on jatkossa Meri-Lappi. Muutos tuli voimaan 2016 alusta.
-- Käyttäjillä OID:ssa on OAM_USER_ORGANIZATION = Kemi/Keminmaa/Simo/Tervola/Tornio ja OAM_USER_DEPARTMENT = meri-lappi

update organisaatio set nimi = 'Meri-Lappi', exttunnus = '%/meri-lappi' where nimi = 'Kemi';