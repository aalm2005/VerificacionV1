package com.codewithmosh.store.bdd.stepdefs;

import com.codewithmosh.store.bdd.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class ProductStepDefs {

    @Autowired
    private TestContext testContext;

    @When("the user requests all products")
    public void requestAllProducts() {
        testContext.setLastResponse(
                given()
                        .when()
                        .get("/products")
        );
    }

    @And("the response contains a list of products")
    public void responseContainsListOfProducts() {
        testContext.getLastResponse().then().body("$", not(empty()));
    }

    @And("the response contains {int} products")
    public void responseContainsNProducts(int count) {
        testContext.getLastResponse().then().body("$", hasSize(count));
    }

    @When("the user requests the product with ID {int}")
    public void requestProductById(int productId) {
        testContext.setLastResponse(
                given()
                        .when()
                        .get("/products/" + productId)
        );
    }

    @And("the response contains a product with ID {int}")
    public void responseContainsProductWithId(int productId) {
        testContext.getLastResponse().then().body("id", equalTo(productId));
    }

    @When("the user requests a product with a non-existent ID")
    public void requestNonExistentProduct() {
        testContext.setLastResponse(
                given()
                        .when()
                        .get("/products/999999")
        );
    }

    @When("the admin creates a product with name {string} and price {double}")
    public void adminCreatesProduct(String name, double price) {
        Map<String, Object> body = new HashMap<>();
        if (!name.isEmpty()) body.put("name", name);
        body.put("price", price);

        Response response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + testContext.getAdminAccessToken())
                .body(body)
                .when()
                .post("/products");
        testContext.setLastResponse(response);
    }

    @And("the response contains the product name {string}")
    public void responseContainsProductName(String name) {
        testContext.getLastResponse().then().body("name", equalTo(name));
    }

    @When("the user attempts to create a product")
    public void userAttemptsCreateProduct() {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .body(Map.of("name", "Test Product", "price", 10.0))
                        .when()
                        .post("/products")
        );
    }

    @When("an unauthenticated user attempts to create a product")
    public void unauthenticatedCreatesProduct() {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("name", "Test Product", "price", 10.0))
                        .when()
                        .post("/products")
        );
    }

    @When("the admin updates product {int} name to {string}")
    public void adminUpdatesProductName(int productId, String name) {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + testContext.getAdminAccessToken())
                        .body(Map.of("name", name))
                        .when()
                        .put("/products/" + productId)
        );
    }

    @When("the admin updates product {int} price to {double}")
    public void adminUpdatesProductPrice(int productId, double price) {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + testContext.getAdminAccessToken())
                        .body(Map.of("price", price))
                        .when()
                        .put("/products/" + productId)
        );
    }

    @And("the response contains the product price {double}")
    public void responseContainsProductPrice(double price) {
        testContext.getLastResponse().then().body("price", equalTo((float) price));
    }

    @When("the admin updates a product with a non-existent ID")
    public void adminUpdatesNonExistentProduct() {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + testContext.getAdminAccessToken())
                        .body(Map.of("name", "Updated"))
                        .when()
                        .put("/products/999999")
        );
    }

    @When("the user attempts to update product {int}")
    public void userAttemptsUpdateProduct(int productId) {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .body(Map.of("name", "Updated"))
                        .when()
                        .put("/products/" + productId)
        );
    }

    @When("an unauthenticated user attempts to update a product")
    public void unauthenticatedUpdatesProduct() {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("name", "Updated"))
                        .when()
                        .put("/products/1")
        );
    }

    @When("the admin deletes product {int}")
    public void adminDeletesProduct(int productId) {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getAdminAccessToken())
                        .when()
                        .delete("/products/" + productId)
        );
    }

    @When("the admin attempts to delete a product with a non-existent ID")
    public void adminDeletesNonExistentProduct() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getAdminAccessToken())
                        .when()
                        .delete("/products/999999")
        );
    }

    @When("the user attempts to delete product {int}")
    public void userAttemptsDeleteProduct(int productId) {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .when()
                        .delete("/products/" + productId)
        );
    }

    @When("an unauthenticated user attempts to delete a product")
    public void unauthenticatedDeletesProduct() {
        testContext.setLastResponse(
                given()
                        .when()
                        .delete("/products/1")
        );
    }
}
