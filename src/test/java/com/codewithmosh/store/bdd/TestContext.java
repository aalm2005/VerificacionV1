package com.codewithmosh.store.bdd;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ScenarioScope
@Data
public class TestContext {
    private String userEmail;
    private String userPassword = "123456";
    private String userAccessToken;
    private String userRefreshToken;
    private Long userId;
    private String adminAccessToken;
    private Long adminId;
    private UUID cartId;
    private UUID secondCartId;
    private Long orderId;
    private Response lastResponse;
}
