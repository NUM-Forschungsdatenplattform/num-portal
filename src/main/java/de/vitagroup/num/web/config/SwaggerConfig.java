package de.vitagroup.num.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket cohortApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("Cohort")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.regex("/cohort*"))
                .build();
    }

    @Bean
    public Docket phenotypeApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("Phenotype")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.regex("/phenotype*"))
                .build();
    }

    @Bean
    public Docket aqlApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("Aql")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.regex("/aql*"))
                .build();
    }

    @Bean
    public Docket adminApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("Admin")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.regex("/admin.*"))
                .build();
    }


}
