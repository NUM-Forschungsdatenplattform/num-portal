# Start local environment

* create an .env file based on [.env-template](.env-template)
* * copy [ehrbase-env file](https://github.com/ehrbase/ehrbase/blob/master/.env.ehrbase) next to the [.env file](.env)

## Config Database and Keycloak

* start postgres and keycloak with the [docker-compos File](docker-compose.yaml)
  * `docker compose up -d`
  * 3 container are starting: postgres, pgadmin, keycloak, ehrbase (keycloak, ehrbase crash on first start because database do not exist)
* connect to DB for example with [pgadmin](localhost:8888):
  * add New Server -> Connection:
    * Hostname: postgres
    * Port: 5432
    * Username: via .env file: ${POSTGRES_USER}
    * Password: via .env file: ${POSTGRES_PASSWORD}
    * `CREATE DATABASE numportal OWNER postgres;`
  * config keyclaok:
      ```SQL
      CREATE USER keycloak WITH PASSWORD 'password';
      CREATE DATABASE keycloak OWNER keycloak;
      ```
    * start keycloak again
    * open [keycloak admin console](http://localhost:8180/admin/) and login via .env config
    * create Realm `crr`
    * create 'Realm roles': `SUPER_ADMIN`, `CONTENT_ADMIN`, `ORGANIZATION_ADMIN`, `MANAGER`, `STUDY_COORDINATOR`, `STUDY_APPROVER`, `RESEARCHER` and `CRITERIA_EDITOR`
    * Users: 
      * add some user
      * role-mapping -> Assign role -> `SUPER_ADMIN` and other
    * import client [num-portal](num-portal.json)
    * num-portal client:
      * generate: Clients -> num-portal -> Credentials -> Client secret
      * copy Client secret, into [application-local.yml](./../src/main/resources/application-local.yml) -> `spring.security.oauth2.client.registration.userStoreClient.client-secret`
      * assign service accounts roles_
        * filter by Client
        * select `manage-users` and `manage-realm`
    * Client scopes from `crr` -> profile -> Mappers -> Add mapper -> from predefined mappers: groups

      [//]: # ( move to FE repo )
    * import [frontend client](num-portal-webapp.json)
* run the [SQL script](./../attachments-db-setup/createdb.sql) to create the attachment database
* insert first organisation and user into the num db
  ```sql
  INSERT INTO num.organization(id, name, description, active) VALUES (1, 'name', 'description', true);
  INSERT INTO num.user_details(user_id, approved, organization_id, created_date) VALUES (<<USER_ID_FROM_KEYCLOAK>>, true, 1, current_date);
  ```
* open [swagger UI](http://localhost:8090/swagger-ui/index.html)
* start FE

## EhrBase
* Prepare database:
  * execute the SQL script on some Database:
    ```sql
    CREATE ROLE ehrbase WITH LOGIN PASSWORD 'ehrbase';
    CREATE ROLE ehrbase_restricted WITH LOGIN PASSWORD 'ehrbase_restricted';
    CREATE DATABASE ehrbase ENCODING 'UTF-8' LOCALE 'C' TEMPLATE template0;
    GRANT ALL PRIVILEGES ON DATABASE ehrbase TO ehrbase;
    GRANT ALL PRIVILEGES ON DATABASE ehrbase TO ehrbase_restricted;
    ```
  * execute this [SQL script](configDB.sql) on the database `ehrbase`
  * _optional_: import test date:
    * with dump if wanted `psql -U postgres -d ehrbase -f <dump-file>`:
    * execute: https://github.com/ehrbase/ehrbase/blob/v0.32.0/UPDATING.md#ehrbase-0250
    * execute: https://github.com/ehrbase/ehrbase/blob/v0.32.0/base/db-setup/add_restricted_user.sql
