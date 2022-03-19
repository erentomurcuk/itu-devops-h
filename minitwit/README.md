# MiniTwit

Run the WebApplication class to open the web server locally.

The application is currently deployed at http://143.198.249.10/.

Uses
* Spark as a web application framework:
  https://sparkjava.com/documentation
* Apache velocity for HTML templates:
  https://velocity.apache.org/engine/2.3/user-guide.html
* PostgreSQL as a remote database: https://www.postgresql.org/docs/

Project is loosely based on [this](https://sparkjava.com/tutorials/application-structure) tutorial.

## Environment variables
 
* MINITWIT_PORT

  Port number server should use, must be a number or unset in which case it defaults to 8080.

* MINITWIT_DB_PASS

  The password to the Postgres database.

* MINITWIT_DB_USER

  The username to the Postgres database.

* MINITWIT_DB_URL

  The URL to the Postgres database.
