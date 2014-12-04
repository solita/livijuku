(defproject juku-db "0.1.0-SNAPSHOT"
	:dependencies [[oracle/ojdbc6 "11.2.0.3.0"]]
	
	:plugins [[lein-dbmaintain "0.1.3"] [oracle/ojdbc6 "11.2.0.3.0"]]

	:dbmaintain {
			 :driver "oracle.jdbc.driver.OracleDriver"
			 :url "jdbc:oracle:thin:@localhost:1521:orcl"
			 :user-name "juku"
			 :password "juku"
			 :schemas "juku"
			 :scripts "sql"
			 :dialect "oracle"}
				 
	:repositories [["solita" {:url "http://mvn.solita.fi/archiva/repository/solita/" :snapshots true}]]

	:profiles {:test-data {:dbmaintain {:scripts "sql, test/sql"}}}
)