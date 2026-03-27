package com.codewithmosh.store.bdd.stepdefs;

import com.codewithmosh.store.bdd.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

public class CartStepDefs {

    @Autowired
    private TestContext testContext;

    @When("the visitor creates a new cart")
    public void visitorCreatesCart() {
        Response response = given()
                .when()
                .post("/carts");
        testContext.setLastResponse(response);
        if (response.getStatusCode() == 201) {
            testContext.setCartId(UUID.fromString(response.jsonPath().getString("id")));
        }
    }

    @And("the response contains a cart ID")
    public void responseContainsCartId() {
        testContext.getLastResponse().then().body("id", notNullValue());
    }

    @When("the user creates a new cart")
    public void userCreatesCart() {
        Response response = given()
                .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                .when()
                .post("/carts");
        testContext.setLastResponse(response);
        if (response.getStatusCode() == 201) {
            testContext.setCartId(UUID.fromString(response.jsonPath().getString("id")));
        }
    }

    @And("the visitor creates another new cart")
    public void visitorCreatesAnotherCart() {
        Response response = given()
                .when()
                .post("/carts");
        if (response.getStatusCode() == 201) {
            testContext.setSecondCartId(UUID.fromString(response.jsonPath().getString("id")));
        }
    }

    @Then("both cart IDs are different")
    public void cartIdsAreDifferent() {
        assertNotEquals(testContext.getCartId(), testContext.getSecondCartId());
    }

    @When("the user adds product with ID {int} to the cart")
    public void addProductToCart(int productId) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("productId", productId))
                .when()
                .post("/carts/" + testContext.getCartId() + "/items");
        testContext.setLastResponse(response);
    }

    @And("the cart contains {int} item(s)")
    public void cartContainsItems(int count) {
        Response cartResponse = given()
                .when()
                .get("/carts/" + testContext.getCartId());
        cartResponse.then().body("items", hasSize(count));
    }

    @When("the user adds a product with a non-existent ID to the cart")
    public void addNonExistentProductToCart() {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("productId", 999999))
                        .when()
                        .post("/carts/" + testContext.getCartId() + "/items")
        );
    }

    @When("the user adds product with ID {int} to a non-existent cart")
    public void addProductToNonExistentCart(int productId) {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("productId", productId))
                        .when()
                        .post("/carts/" + UUID.randomUUID() + "/items")
        );
    }

    @And("the user adds product with ID {int} to the cart again")
    public void addProductToCartAgain(int productId) {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("productId", productId))
                        .when()
                        .post("/carts/" + testContext.getCartId() + "/items")
        );
    }

    @When("the user requests their cart")
    public void getUserCart() {
        testContext.setLastResponse(
                given()
                        .when()
                        .get("/carts/" + testContext.getCartId())
        );
    }

    @And("the cart total price is greater than 0")
    public void cartTotalPriceGreaterThanZero() {
        double totalPrice = testContext.getLastResponse().jsonPath().getDouble("totalPrice");
        assertTrue(totalPrice > 0, "Cart total price should be greater than 0");
    }

    @When("the user requests a cart with a non-existent ID")
    public void requestNonExistentCart() {
        testContext.setLastResponse(
                given()
                        .when()
                        .get("/carts/" + UUID.randomUUID())
        );
    }

    @And("the cart total price equals the sum of all item prices")
    public void cartTotalPriceEqualsSum() {
        Response cartResponse = testContext.getLastResponse();
        double totalPrice = cartResponse.jsonPath().getDouble("totalPrice");
        List<Map<String, Object>> items = cartResponse.jsonPath().getList("items");
        double sumOfPrices = items.stream()
                .mapToDouble(item -> {
                    Object price = ((Map<?, ?>) item.get("product")).get("price");
                    Integer quantity = (Integer) item.get("quantity");
                    return ((Number) price).doubleValue() * quantity;
                })
                .sum();
        assertEquals(sumOfPrices, totalPrice, 0.01);
    }

    @When("the user updates the quantity of product {int} in the cart to {int}")
    public void updateProductQuantity(int productId, int quantity) {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("quantity", quantity))
                        .when()
                        .put("/carts/" + testContext.getCartId() + "/items/" + productId)
        );
    }

    @And("the quantity of product {int} in the cart is {int}")
    public void checkProductQuantity(int productId, int expectedQuantity) {
        Response cartResponse = given()
                .when()
                .get("/carts/" + testContext.getCartId());
        List<Map<String, Object>> items = cartResponse.jsonPath().getList("items");
        int actualQuantity = items.stream()
                .filter(item -> {
                    Map<?, ?> product = (Map<?, ?>) item.get("product");
                    return ((Number) product.get("id")).intValue() == productId;
                })
                .mapToInt(item -> (Integer) item.get("quantity"))
                .findFirst()
                .orElse(-1);
        assertEquals(expectedQuantity, actualQuantity);
    }

    @When("the user removes product {int} from the cart")
    public void removeProductFromCart(int productId) {
        testContext.setLastResponse(
                given()
                        .when()
                        .delete("/carts/" + testContext.getCartId() + "/items/" + productId)
        );
    }

    @And("the cart does not contain product {int}")
    public void cartDoesNotContainProduct(int productId) {
        Response cartResponse = given()
                .when()
                .get("/carts/" + testContext.getCartId());
        List<Map<String, Object>> items = cartResponse.jsonPath().getList("items");
        boolean containsProduct = items.stream()
                .anyMatch(item -> {
                    Map<?, ?> product = (Map<?, ?>) item.get("product");
                    return ((Number) product.get("id")).intValue() == productId;
                });
        assertFalse(containsProduct, "Cart should not contain product " + productId);
    }

    @When("the user removes product {int} from a non-existent cart")
    public void removeProductFromNonExistentCart(int productId) {
        testContext.setLastResponse(
                given()
                        .when()
                        .delete("/carts/" + UUID.randomUUID() + "/items/" + productId)
        );
    }

    @When("the user clears the cart")
    public void clearCart() {
        testContext.setLastResponse(
                given()
                        .when()
                        .delete("/carts/" + testContext.getCartId() + "/items")
        );
    }

    @When("the user clears a non-existent cart")
    public void clearNonExistentCart() {
        testContext.setLastResponse(
                given()
                        .when()
                        .delete("/carts/" + UUID.randomUUID() + "/items")
        );
    }
}
