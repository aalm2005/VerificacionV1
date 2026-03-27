package com.codewithmosh.store.bdd.stepdefs;

import com.codewithmosh.store.bdd.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class OrderStepDefs {

    @Autowired
    private TestContext testContext;

    @When("the user requests all their orders")
    public void userRequestsAllOrders() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .when()
                        .get("/orders")
        );
    }

    @And("the response contains at least 1 order")
    public void responseContainsAtLeastOneOrder() {
        testContext.getLastResponse().then().body("$", not(empty()));
    }

    @When("an unauthenticated user requests all orders")
    public void unauthenticatedRequestsAllOrders() {
        testContext.setLastResponse(
                given()
                        .when()
                        .get("/orders")
        );
    }

    @When("the admin requests all orders")
    public void adminRequestsAllOrders() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getAdminAccessToken())
                        .when()
                        .get("/orders")
        );
    }

    @When("the user requests their order by ID")
    public void userRequestsOrderById() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .when()
                        .get("/orders/" + testContext.getOrderId())
        );
    }

    @And("the response contains the order details")
    public void responseContainsOrderDetails() {
        testContext.getLastResponse().then()
                .body("id", notNullValue())
                .body("status", notNullValue())
                .body("items", notNullValue());
    }

    @When("the user requests an order with a non-existent ID")
    public void userRequestsNonExistentOrder() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .when()
                        .get("/orders/999999")
        );
    }

    @When("an unauthenticated user requests an order by ID")
    public void unauthenticatedRequestsOrderById() {
        testContext.setLastResponse(
                given()
                        .when()
                        .get("/orders/1")
        );
    }
}
