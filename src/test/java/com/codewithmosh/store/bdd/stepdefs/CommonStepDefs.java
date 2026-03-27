package com.codewithmosh.store.bdd.stepdefs;

import com.codewithmosh.store.bdd.TestContext;
import com.codewithmosh.store.payments.CheckoutSession;
import com.codewithmosh.store.payments.PaymentGateway;
import com.codewithmosh.store.users.Role;
import com.codewithmosh.store.users.User;
import com.codewithmosh.store.users.UserRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CommonStepDefs {

    @Autowired
    private TestContext testContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PaymentGateway paymentGateway;

    @Given("a registered user exists with email {string}")
    public void createUser(String email) {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
        User user = User.builder()
                .name("Harry Potter")
                .email(email)
                .password(passwordEncoder.encode("123456"))
                .role(Role.USER)
                .build();
        User saved = userRepository.save(user);
        testContext.setUserId(saved.getId());
        testContext.setUserEmail(email);
    }

    @Given("another user exists with email {string}")
    public void createAnotherUser(String email) {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
        User user = User.builder()
                .name("Another User")
                .email(email)
                .password(passwordEncoder.encode("123456"))
                .role(Role.USER)
                .build();
        userRepository.save(user);
    }

    @Given("a registered admin user exists")
    public void createAdminUser() {
        userRepository.findByEmail("admin@store.com").ifPresent(userRepository::delete);
        User admin = User.builder()
                .name("Admin User")
                .email("admin@store.com")
                .password(passwordEncoder.encode("admin123456"))
                .role(Role.ADMIN)
                .build();
        User saved = userRepository.save(admin);
        testContext.setAdminId(saved.getId());
    }

    @And("the user is authenticated")
    public void authenticateUser() {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", testContext.getUserEmail(), "password", testContext.getUserPassword()))
                .when()
                .post("/auth/login");
        testContext.setUserAccessToken(response.jsonPath().getString("accessToken"));
        testContext.setUserRefreshToken(response.getCookies().get("refreshToken"));
    }

    @And("the admin is authenticated")
    public void authenticateAdmin() {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", "admin@store.com", "password", "admin123456"))
                .when()
                .post("/auth/login");
        testContext.setAdminAccessToken(response.jsonPath().getString("accessToken"));
    }

    @Then("the response status code is {int}")
    public void checkStatusCode(int expectedCode) {
        testContext.getLastResponse().then().statusCode(expectedCode);
    }

    @And("the response contains the user email {string}")
    public void responseContainsUserEmail(String email) {
        testContext.getLastResponse().then().body("email", equalTo(email));
    }

    @Given("a cart has been created")
    public void createCart() {
        Response response = given()
                .when()
                .post("/carts");
        String cartIdStr = response.jsonPath().getString("id");
        testContext.setCartId(UUID.fromString(cartIdStr));
    }

    @Given("product with ID {int} has been added to the cart")
    public void addProductToCart(int productId) {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("productId", productId))
                .when()
                .post("/carts/" + testContext.getCartId() + "/items");
    }

    @Given("a product exists with ID {int}")
    public void productExistsWithId(int productId) {
        // Products are seeded via Flyway — no action needed
    }

    @Given("the cart has been cleared")
    public void clearCart() {
        given()
                .when()
                .delete("/carts/" + testContext.getCartId() + "/items");
    }

    @Given("the user has a completed order")
    public void userHasCompletedOrder() {
        when(paymentGateway.createCheckoutSession(any()))
                .thenReturn(new CheckoutSession("https://checkout.stripe.com/test-session"));

        // Create cart
        Response cartResponse = given().when().post("/carts");
        UUID cartId = UUID.fromString(cartResponse.jsonPath().getString("id"));
        testContext.setCartId(cartId);

        // Add product
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("productId", 1))
                .when()
                .post("/carts/" + cartId + "/items");

        // Checkout
        Response checkoutResponse = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                .body(Map.of("cartId", cartId.toString()))
                .when()
                .post("/checkout");

        testContext.setOrderId(checkoutResponse.jsonPath().getLong("orderId"));
    }
}
