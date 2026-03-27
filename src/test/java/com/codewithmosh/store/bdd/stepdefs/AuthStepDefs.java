package com.codewithmosh.store.bdd.stepdefs;

import com.codewithmosh.store.bdd.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class AuthStepDefs {

    @Autowired
    private TestContext testContext;

    @Value("${spring.jwt.secret}")
    private String jwtSecret;

    @When("the user logs in with email {string} and password {string}")
    public void loginWithCredentials(String email, String password) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/auth/login");
        testContext.setLastResponse(response);
        if (response.getStatusCode() == 200) {
            testContext.setUserAccessToken(response.jsonPath().getString("accessToken"));
            testContext.setUserRefreshToken(response.getCookies().get("refreshToken"));
        }
    }

    @And("the response contains an access token")
    public void responseContainsAccessToken() {
        testContext.getLastResponse().then().body("accessToken", notNullValue());
    }

    @And("the response contains a refresh token")
    public void responseContainsRefreshToken() {
        testContext.getLastResponse().then().cookie("refreshToken", notNullValue());
    }

    @When("the user sends a refresh token request")
    public void sendRefreshTokenRequest() {
        Response response = given()
                .cookie("refreshToken", testContext.getUserRefreshToken())
                .when()
                .post("/auth/refresh");
        testContext.setLastResponse(response);
        if (response.getStatusCode() == 200) {
            testContext.setUserAccessToken(response.jsonPath().getString("accessToken"));
        }
    }

    @And("the response contains a new access token")
    public void responseContainsNewAccessToken() {
        testContext.getLastResponse().then().body("accessToken", notNullValue());
    }

    @When("the user sends a refresh request with an invalid token")
    public void sendRefreshRequestWithInvalidToken() {
        testContext.setLastResponse(
                given()
                        .cookie("refreshToken", "invalid.token.value")
                        .when()
                        .post("/auth/refresh")
        );
    }

    @When("the user sends a refresh request with no token")
    public void sendRefreshRequestWithNoToken() {
        testContext.setLastResponse(
                given()
                        .when()
                        .post("/auth/refresh")
        );
    }

    @When("the user sends a refresh request with an expired token")
    public void sendRefreshRequestWithExpiredToken() {
        String expiredToken = Jwts.builder()
                .subject("harry@gmail.com")
                .expiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
        testContext.setLastResponse(
                given()
                        .cookie("refreshToken", expiredToken)
                        .when()
                        .post("/auth/refresh")
        );
    }

    @When("the user requests their own profile")
    public void getUserProfile() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .when()
                        .get("/auth/me")
        );
    }

    @When("an unauthenticated user requests the profile endpoint")
    public void unauthenticatedGetProfile() {
        testContext.setLastResponse(
                given()
                        .when()
                        .get("/auth/me")
        );
    }

    @And("the user refreshes their access token")
    public void refreshAccessToken() {
        Response response = given()
                .cookie("refreshToken", testContext.getUserRefreshToken())
                .when()
                .post("/auth/refresh");
        if (response.getStatusCode() == 200) {
            testContext.setUserAccessToken(response.jsonPath().getString("accessToken"));
        }
    }
}
