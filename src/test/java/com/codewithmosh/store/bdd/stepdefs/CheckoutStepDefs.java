package com.codewithmosh.store.bdd.stepdefs;

import com.codewithmosh.store.bdd.TestContext;
import com.codewithmosh.store.orders.PaymentStatus;
import com.codewithmosh.store.payments.CheckoutSession;
import com.codewithmosh.store.payments.PaymentGateway;
import com.codewithmosh.store.payments.PaymentResult;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Component
public class CheckoutStepDefs {

    @Autowired
    private TestContext testContext;

    @Autowired
    private PaymentGateway paymentGateway;

    @When("the user sends a checkout request with their cart ID")
    public void checkout() {
        when(paymentGateway.createCheckoutSession(any()))
                .thenReturn(new CheckoutSession("https://checkout.stripe.com/test-session-id"));

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                .body(Map.of("cartId", testContext.getCartId().toString()))
                .when()
                .post("/checkout");
        testContext.setLastResponse(response);
        if (response.getStatusCode() == 200) {
            testContext.setOrderId(response.jsonPath().getLong("orderId"));
        }
    }

    @And("the response contains a Stripe checkout URL")
    public void responseContainsStripeUrl() {
        testContext.getLastResponse().then().body("checkoutUrl", notNullValue());
    }

    @When("an unauthenticated user sends a checkout request")
    public void unauthenticatedCheckout() {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("cartId", testContext.getCartId().toString()))
                        .when()
                        .post("/checkout")
        );
    }

    @When("the user sends a checkout request with a non-existent cart ID")
    public void checkoutNonExistentCart() {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .body(Map.of("cartId", UUID.randomUUID().toString()))
                        .when()
                        .post("/checkout")
        );
    }

    @Given("the user has completed a checkout")
    public void userHasCompletedCheckout() {
        when(paymentGateway.createCheckoutSession(any()))
                .thenReturn(new CheckoutSession("https://checkout.stripe.com/test-session-id"));

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                .body(Map.of("cartId", testContext.getCartId().toString()))
                .when()
                .post("/checkout");
        testContext.setOrderId(response.jsonPath().getLong("orderId"));
    }

    @When("Stripe sends a successful payment webhook event")
    public void stripeWebhook() {
        when(paymentGateway.parseWebhookRequest(any()))
                .thenReturn(Optional.of(new PaymentResult(testContext.getOrderId(), PaymentStatus.PAID)));

        testContext.setLastResponse(
                given()
                        .contentType(ContentType.TEXT)
                        .header("stripe-signature", "t=dummy,v1=dummy")
                        .body("{\"type\":\"payment_intent.succeeded\"}")
                        .when()
                        .post("/checkout/webhook")
        );
    }

    @Then("the order status is updated to paid")
    public void checkOrderStatusPaid() {
        given()
                .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                .when()
                .get("/orders/" + testContext.getOrderId())
                .then()
                .body("status", equalTo("PAID"));
    }
}
