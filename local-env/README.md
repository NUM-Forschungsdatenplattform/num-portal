# Start local environment

* create an .env file based on [.env-template](.env-template)

## Config Database and Keycloak

* start postgres and keycloak with the [docker-compos File](docker-compose.yaml)
  * `docker compose up -d`
  * 3 container are starting (postgres, pgadmin, keycloak)
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
    * import client [num-portal-webapp](num-portal-webapp.json) 
    * create role `SUPER_ADMIN`, `CONTENT_ADMIN`, `ORGANIZATION_ADMIN`, `MANAGER`, `STUDY_COORDINATOR`, `STUDY_APPROVER`, `RESEARCHER` and `CRITERIA_EDITOR`
    * add some user
    * add user to the role `SUPER_ADMIN` and other
    * import client [num-portal](num-portal.json)
    * update num-portal client:
      * copy Client -> num-portal -> Credentials -> Client secret, into [application-local.yml](./../src/main/resources/application-local.yml) -> `spring.security.oauth2.client.registration.userStoreClient.client-secret`
      * assign service accounts roles -> manage-users and manage-realm
    * Client scopes -> profile -> Mappers -> Add mapper: groups

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
* copy [ehrbase-env file](https://github.com/ehrbase/ehrbase/blob/master/.env.ehrbase) next to the [.env file](.env)
* Prepare database:
  * execute the [SQL script](https://github.com/ehrbase/ehrbase/blob/develop/createdb.sql), or
  * create `ehrbase` DB and import test date with dump if wanted `psql -U postgres -d ehrbase -f <dump-file>`
