package com.acme.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.acme.employee.config.AppProperties;
import com.acme.employee.schema.SchemaFieldType;
import com.acme.employee.schema.SchemaResult;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SchemaDiscoveryServiceTest {

    @Mock
    private ReactiveMongoTemplate template;

    private SchemaDiscoveryService service;

    @BeforeEach
    void setUp() {
        AppProperties.SchemaProperties schemaProps = new AppProperties.SchemaProperties(10, 60);
        AppProperties.CorsProperties corsProps = new AppProperties.CorsProperties(List.of("http://localhost"));
        service = new SchemaDiscoveryService(template, new AppProperties(schemaProps, corsProps));
    }

    @Test
    void discoversSimpleSchema() {
        Document employee1 = new Document("attributes", new Document(Map.of(
                "firstName", "Jane",
                "age", 30,
                "email", "jane@example.com"
        )));
        Document employee2 = new Document("attributes", new Document(Map.of(
                "firstName", "John",
                "age", 28,
                "email", "john@example.com",
                "active", true
        )));

        when(template.find(any(), eq(Document.class), eq("employees")))
                .thenReturn(Flux.just(employee1, employee2));

        StepVerifier.create(service.discover("employees", 5))
                .assertNext(result -> {
                    assertThat(result.collection()).isEqualTo("employees");
                    assertThat(result.fields()).containsKeys("firstName", "age", "email", "active");
                    assertThat(result.fields().get("firstName").type()).isEqualTo(SchemaFieldType.STRING);
                    assertThat(result.fields().get("age").type()).isEqualTo(SchemaFieldType.NUMBER);
                    assertThat(result.fields().get("active").type()).isEqualTo(SchemaFieldType.BOOLEAN);
                })
                .verifyComplete();
    }
}

