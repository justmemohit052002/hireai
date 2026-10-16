package com.vionsys.hireai.user.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
@ActiveProfiles("test")
class EndpointRegistrationTest {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    @DisplayName("Verify exact registered routes for UserController")
    void testUserControllerEndpointsAreRegistered() {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        List<String> registeredUserRoutes = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();
            if (handlerMethod.getBeanType().equals(UserController.class)) {
                Set<String> patterns = entry.getKey().getPatternValues();
                Set<RequestMethod> methods = entry.getKey().getMethodsCondition().getMethods();
                for (String pattern : patterns) {
                    for (RequestMethod method : methods) {
                        String route = method + " " + pattern + " -> " + handlerMethod.getMethod().getName() + "()";
                        registeredUserRoutes.add(route);
                        System.out.println("[REGISTERED ROUTE] " + route);
                    }
                }
            }
        }

        assertFalse(registeredUserRoutes.isEmpty(), "UserController must have registered endpoints");

        // Verify set-password endpoints
        assertTrue(registeredUserRoutes.stream().anyMatch(r -> r.contains("POST /users/me/set-password/request")),
                "POST /users/me/set-password/request must be registered");
        assertTrue(registeredUserRoutes.stream().anyMatch(r -> r.contains("POST /users/me/set-password/confirm")),
                "POST /users/me/set-password/confirm must be registered");

        // Verify change-password endpoint
        assertTrue(registeredUserRoutes.stream().anyMatch(r -> r.contains("PUT /users/me/change-password")),
                "PUT /users/me/change-password must be registered");

        // Verify avatar endpoints
        assertTrue(registeredUserRoutes.stream().anyMatch(r -> r.contains("POST /users/me/avatar")),
                "POST /users/me/avatar must be registered");
        assertTrue(registeredUserRoutes.stream().anyMatch(r -> r.contains("DELETE /users/me/avatar")),
                "DELETE /users/me/avatar must be registered");
        assertTrue(registeredUserRoutes.stream().anyMatch(r -> r.contains("GET /users/me/avatar")),
                "GET /users/me/avatar must be registered");
        assertTrue(registeredUserRoutes.stream().anyMatch(r -> r.contains("GET /users/{userId}/avatar")),
                "GET /users/{userId}/avatar must be registered");

        // Verify delete-account endpoints
        assertTrue(registeredUserRoutes.stream().anyMatch(r -> r.contains("POST /users/me/delete-account/request")),
                "POST /users/me/delete-account/request must be registered");
        assertTrue(registeredUserRoutes.stream().anyMatch(r -> r.contains("DELETE /users/me/delete-account")),
                "DELETE /users/me/delete-account must be registered");
        assertTrue(registeredUserRoutes.stream().anyMatch(r -> r.contains("POST /users/me/delete-account")),
                "POST /users/me/delete-account must be registered");
    }
}
