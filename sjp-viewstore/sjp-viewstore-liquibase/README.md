# Instructions for creating the database

1. Install Postgres 9.4 or later
2. Create a database sjpcppdb with following commands:

		1.Open command prompt

		2.cd D:\software\PostgreSQL\9.4\bin

		3.Connect to the database: 
		a.psql -U postgres postgres

		Note:  The above command will prompt for a password, enter the password you used to setup the database. 

		4.Create user and database by entering the following queries:

		a.CREATE USER sjpcpp WITH PASSWORD 'password';

		b.CREATE DATABASE sjpcppdb OWNER sjpcpp;


		5.Quit the postgres database by entering:
		a. /q or CTRL+C

		6.Login to cppsiviewdb database:
		a.psql -U sjpcpp sjpcppdb
		Note: The above command will prompt for a password, enter the password "password". 

		7.Create a new schema in sjpdb database by entering the following query:

		a.CREATE SCHEMA sjpcppschema;

		8.Provide relevant access to user/schema/database by entering the following commands:

		a.ALTER DATABASE sjpcppdb SET search_path TO sjpcppschema;
		b.ALTER user sjpcpp SET search_path TO sjpcppschema;
		c.GRANT ALL ON SCHEMA sjpcppschema TO sjpcpp ;

		9.
		Quit the sjpdb database
		 /q or CTRL+C
 
5. Run with the following command:
   mvn -Dliquibase.url=jdbc:postgresql://localhost:5432/sjpviewstore -Dliquibase.username=sjp -Dliquibase.password=sjp -Dliquibase.logLevel=info resources:resources liquibase:update
   
   OR go to target directory and run:
   java -jar sjp-liquibase-1.0.0-SNAPSHOT.jar --url=jdbc:postgresql://localhost:5432/sjpviewstore --username=sjp --password=sjp --logLevel=info --defaultSchemaName=sjpcppschema  update

