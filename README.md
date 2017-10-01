###Download Data:

https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-all-titles-in-ns0.gz

###Setup Mysql Docker Container:

1) docker pull mysql
2) docker run --name mysql -p3307:3306 -e MYSQL_ROOT_PASSWORD=testadmin -e MYSQL_USR=wikitest -e MYSQL_PASSWORD=wikitest -e MYSQL_DATABASE=WIKI_TEST -d mysql
3) mysql -h127.0.0.1 -uroot --port=3307 -ptestadmin

###Load Data Into Mysql:

1) CREATE TABLE TITLES (PAGE_TITLE VARCHAR(1000) PRIMARY KEY);
2) ALTER TABLE TITLES ADD FULLTEXT INDEX FT_IDX (PAGE_TITLE);
3) load data local infile '<data_path>/enwiki-latest-all-titles-in-ns0' 
   into table WIKI_TEST.TITLES 
   fields terminated by ',' 
   enclosed by '"' 
   lines terminated by '\n' 
   IGNORE 1 LINES   
   (PAGE_TITLE);
   
###How to run the Server:

1) mvn clean package
2) java -jar target/server-0.1.0-fat.jar

###REST Resource:

1) curl -i 'http://localhost:8080/wikiTitle?name=Thread'
2) curl -i 'http://localhost:8080/wikiPage/Thread(OS)'