# Instructions for creating the database

1. Install Postgres 9.4 or later
2. Create a database structurecppdb with following commands:

		1.Open command prompt

		2.cd D:\software\PostgreSQL\9.4\bin

		3.Connect to the database: 
		a.psql -U postgres postgres

		Note:  The above command will prompt for a password, enter the password you used to setup the database. 

		4.Create user and database by entering the following queries:

		a.CREATE USER structurecpp WITH PASSWORD 'password';

		b.CREATE DATABASE structurecppdb OWNER structurecpp;


		5.Quit the postgres database by entering:
		a. /q or CTRL+C

		6.Login to cppsiviewdb database:
		a.psql -U structurecpp structurecppdb
		Note: The above command will prompt for a password, enter the password "password". 

		7.Create a new schema in structuredb database by entering the following query:

		a.CREATE SCHEMA structurecppschema;

		8.Provide relevant access to user/schema/database by entering the following commands:

		a.ALTER DATABASE structurecppdb SET search_path TO structurecppschema;
		b.ALTER user structurecpp SET search_path TO structurecppschema;
		c.GRANT ALL ON SCHEMA structurecppschema TO structurecpp ;

		9.
		Quit the structuredb database
		 /q or CTRL+C
 
5. Run with the following command:
   mvn -Dliquibase.url=jdbc:postgresql://localhost:5432/structureviewstore -Dliquibase.username=structure -Dliquibase.password=structure -Dliquibase.logLevel=info resources:resources liquibase:update
   
   OR go to target directory and run:
   java -jar structure-liquibase-1.0.0-SNAPSHOT.jar --url=jdbc:postgresql://localhost:5432/structureviewstore --username=structure --password=structure --logLevel=info --defaultSchemaName=structurecppschema  update

