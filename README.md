# Num-portal
The num-portal repository includes the backend for the Routine Data Platform (RDP). 

## Building and running locally
To get the backend running first a postgres instance needs to be running.

To start a local instance of PostgreSQL:

```
docker run --name postgres -e POSTGRES_PASSWORD=postgres -d -p 5432:5432 postgres
```
Then you can start the project with the setting for the application-local.yml:

In the root folder of the project, open cmd and run:

1. Build app: `mvn clean install`
2. Run: `spring_profiles_active=local mvn spring-boot:run`

After that you can visit the swagger website: 

### Swagger 

http://localhost:8090/swagger-ui/index.html

## Contributing

Pull requests are welcome.
For major changes, please open an issue first to discuss what you would like to change.
After that and the approval of HiGHmed e.V. (rdp-support@highmed.org) you can add the code in a Branch. 
1. Create a branch named 'feature/name-of-branch' because of pipeline requirements
2. Check you code with our provided [checkstyle](/.config/checkstyle.xml)
3. Update the previously tests
4. PRs can only be merged once the [build_for_development pipeline](/.github/workflows/build-for-development.yml) has been successfully completed

### Checkstyle 

To integrate checkstyle in your IDE (IntelliJ) you need the checkstyle-plugin. 
And for intelliJ we include a [intellij-codestyle.xml](/.config/intellij-codestyle.xml) file

#### IntelliJ Steps

1. Add the file in Settings -> Editor -> Code Style -> Java 
2. For the checkstyle-plugin you can add the file [checkstyle](/.config/checkstyle.xml) under Settings -> Tools -> Checkstyle -> Configuration File


## License

Copyright 2024 HiGHmed e.V.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
