# Num-portal

### Building and running locally

1. Postgres should be up and running, instructions below

In the root folder of the project, open cmd and run:

1. Build app: ```mvn clean install```
2. Run: ```mvn spring-boot:run```

### Database 

Start a local instance of PostgreSQL: 

``` 
docker run --name postgres -e POSTGRES_PASSWORD=postgres -d -p 5432:5432 postgres
```

### Swagger

http://localhost:8090/swagger-ui/


### License

Copyright 2020 vitagroup AG

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
