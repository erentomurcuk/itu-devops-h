# Minitwit server

Run the WebApplication class to open web server.

Run SQLLite class to initialize database.

Uses
* Spark as a web application framework
  https://sparkjava.com/documentation
* Apache velocity
  https://velocity.apache.org/engine/2.3/user-guide.html
* SQLite-JDBC
  https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc

Project is loosely based on [this](https://sparkjava.com/tutorials/application-structure) tutorial.

## Environment variables
 
* MINITWIT_PORT

  Port number server should use, must be a number or unset in which case it defaults to 8080.

* MINITWIT_DB_PATH

  Path to minitwit. Default is minitwit.db
