package com.ytdinfo.inndoo.config.swagger;

import com.ytdinfo.conf.core.annotation.XxlConf;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Exrickx
 */
@Slf4j
@Configuration
@EnableSwagger2
public class Swagger2Config {

//    @Value("${swagger.title}")
//    private String title;
//
//    @Value("${swagger.description}")
//    private String description;
//
//    @Value("${swagger.version}")
//    private String version;
//
//    @Value("${swagger.termsOfServiceUrl}")
//    private String termsOfServiceUrl;
//
//    @Value("${swagger.contact.name}")
//    private String name;
//
//    @Value("${swagger.contact.url}")
//    private String url;
//
//    @Value("${swagger.contact.email}")
//    private String email;

    @XxlConf("core.swagger.title")
    private String title;

    @XxlConf("core.swagger.description")
    private String description;

    @XxlConf("core.swagger.version")
    private String version;

    @XxlConf("core.swagger.termsofserviceurl")
    private String termsOfServiceUrl;

    @XxlConf("core.swagger.contact.name")
    private String name;

    @XxlConf("core.swagger.contact.url")
    private String url;

    @XxlConf("core.swagger.contact.email")
    private String email;

    @XxlConf("core.swagger.enabled")
    private Boolean enabled;

    private List<ApiKey> securitySchemes() {
        List<ApiKey> apiKeys = new ArrayList<>();
        apiKeys.add(new ApiKey("Authorization", "accessToken", "header"));
        apiKeys.add(new ApiKey("Appid", "wxappid", "header"));
        apiKeys.add(new ApiKey("TenantId", "tenantId", "header"));
        return apiKeys;
    }

    private List<SecurityContext> securityContexts() {
        List<SecurityContext> securityContexts = new ArrayList<>();
        securityContexts.add(SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(PathSelectors.regex("^(?!auth).*$")).build());
        return securityContexts;
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        List<SecurityReference> securityReferences = new ArrayList<>();
        securityReferences.add(new SecurityReference("Authorization", authorizationScopes));
        securityReferences.add(new SecurityReference("Appid", authorizationScopes));
        securityReferences.add(new SecurityReference("TenantId", authorizationScopes));
        return securityReferences;
    }

    @Bean
    public Docket createRestApi() {

        if (enabled) {
            log.info("加载Swagger2");
            HashSet<String> consumesHashSet = new HashSet<>();
            consumesHashSet.add("application/x-www-form-urlencoded");
            consumesHashSet.add("application/json");
            consumesHashSet.add("multipart/form-data");

            return new Docket(DocumentationType.SWAGGER_2)
                    .apiInfo(apiInfo()).select()
                            // 扫描所有有注解的api，用这种方式更灵活
                    .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                    .paths(PathSelectors.any())
                    .build()
                    .consumes(consumesHashSet)
                    .securitySchemes(securitySchemes())
                    .securityContexts(securityContexts());
        } else {
            return new Docket(DocumentationType.SWAGGER_2)
                    .apiInfo(apiInfo());
        }
    }

    private ApiInfo apiInfo() {
        if (enabled) {
            return new ApiInfoBuilder()
                    .title(title)
                    .description(description)
                    .termsOfServiceUrl(termsOfServiceUrl)
                    .contact(new Contact(name, url, email))
                    .version(version)
                    .build();
        } else {
            return new ApiInfoBuilder()
                    .title("")
                    .description("")
                    .license("")
                    .licenseUrl("")
                    .termsOfServiceUrl("")
                    .version("")
                    .contact(new Contact("", "", ""))
                    .build();
        }
    }
}
