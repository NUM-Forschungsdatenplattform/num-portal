# Num-portal

### Building and running locally

1. Postgres should be up and running, instructions below

In the root folder of the project, open cmd and run:

1. Build app: ```mvn clean install```
2. Run: ```mvn spring-boot:run```

### Database 

Start local instance of PostgreSQL: 

``` 
docker run --name postgres -e POSTGRES_PASSWORD=postgres -d -p 5432:5432 postgres
```

### Swagger

http://localhost:8090/swagger-ui/


