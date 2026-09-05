package com.chethu.paymentledgerservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

class OpenApiConfigTest {
    @Test
    void definesProjectMetadataAndJwtBearerScheme() {
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);
        SecurityScheme scheme = OpenApiConfig.class.getAnnotation(SecurityScheme.class);

        assertNotNull(definition);
        assertEquals("Lumo Payment Ledger API", definition.info().title());
        assertEquals("1.0", definition.info().version());
        assertNotNull(scheme);
        assertEquals("bearerAuth", scheme.name());
        assertEquals(SecuritySchemeType.HTTP, scheme.type());
        assertEquals("bearer", scheme.scheme());
        assertEquals("JWT", scheme.bearerFormat());
    }
}
