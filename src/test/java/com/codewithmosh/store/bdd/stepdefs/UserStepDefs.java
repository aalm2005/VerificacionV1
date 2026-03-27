package com.codewithmosh.store.bdd.stepdefs;

import com.codewithmosh.store.bdd.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Component
public class UserStepDefs {

    @Autowired
    private TestContext testContext;

    @When("the user registers with name {string} email {string} and password {string}")
    public void registerUser(String name, String email, String password) {
        Map<String, String> body = new HashMap<>();
        if (!name.isEmpty()) body.put("name", name);
        if (!email.isEmpty()) body.put("email", email);
        if (!password.isEmpty()) body.put("password", password);
        if (name.isEmpty()) body.put("name", "");
        if (email.isEmpty()) body.put("email", "");
        if (password.isEmpty()) body.put("password", "");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/users");
        testContext.setLastResponse(response);
        if (response.getStatusCode() == 201) {
            testContext.setUserId(response.jsonPath().getLong("id"));
        }
    }

    @And("the response contains the name {string}")
    public void responseContainsName(String name) {
        testContext.getLastResponse().then().body("name", equalTo(name));
    }

    @And("the response contains the email {string}")
    public void responseContainsEmail(String email) {
        testContext.getLastResponse().then().body("email", equalTo(email));
    }

    @When("the admin requests all users")
    public void adminRequestsAllUsers() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getAdminAccessToken())
                        .when()
                        .get("/users")
        );
    }

    @And("the response contains a list of users")
    public void responseContainsListOfUsers() {
        testContext.getLastResponse().then().body("$", instanceOf(java.util.List.class));
    }

    @When("the user requests all users")
    public void userRequestsAllUsers() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .when()
                        .get("/users")
        );
    }

    @When("an unauthenticated user requests all users")
    public void unauthenticatedRequestsAllUsers() {
        testContext.setLastResponse(
                given()
                        .when()
                        .get("/users")
        );
    }

    @When("the user requests their own user by ID")
    public void userRequestsOwnUserById() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .when()
                        .get("/users/" + testContext.getUserId())
        );
    }

    @When("the admin requests a user by a valid ID")
    public void adminRequestsUserById() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getAdminAccessToken())
                        .when()
                        .get("/users/" + testContext.getUserId())
        );
    }

    @When("the user requests a user with a non-existent ID")
    public void userRequestsNonExistentUser() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .when()
                        .get("/users/999999")
        );
    }

    @When("an unauthenticated user requests a user by ID")
    public void unauthenticatedRequestsUserById() {
        testContext.setLastResponse(
                given()
                        .when()
                        .get("/users/1")
        );
    }

    @When("the user updates their name to {string}")
    public void updateUserName(String name) {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .body(Map.of("name", name))
                        .when()
                        .put("/users/" + testContext.getUserId())
        );
    }

    @When("the user updates their email to {string}")
    public void updateUserEmail(String email) {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .body(Map.of("email", email))
                        .when()
                        .put("/users/" + testContext.getUserId())
        );
    }

    @When("an unauthenticated user sends an update request")
    public void unauthenticatedUpdateRequest() {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("name", "New Name"))
                        .when()
                        .put("/users/1")
        );
    }

    @When("the admin deletes the user by ID")
    public void adminDeletesUser() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getAdminAccessToken())
                        .when()
                        .delete("/users/" + testContext.getUserId())
        );
    }

    @When("the admin attempts to delete a user with a non-existent ID")
    public void adminDeletesNonExistentUser() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getAdminAccessToken())
                        .when()
                        .delete("/users/999999")
        );
    }

    @When("the user attempts to delete another user")
    public void userAttemptsDeleteAnotherUser() {
        testContext.setLastResponse(
                given()
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .when()
                        .delete("/users/" + testContext.getAdminId())
        );
    }

    @When("an unauthenticated user attempts to delete a user")
    public void unauthenticatedDeleteUser() {
        testContext.setLastResponse(
                given()
                        .when()
                        .delete("/users/1")
        );
    }

    @When("the user changes their password from {string} to {string}")
    public void changePassword(String oldPassword, String newPassword) {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .body(Map.of("oldPassword", oldPassword, "newPassword", newPassword))
                        .when()
                        .post("/users/" + testContext.getUserId() + "/change-password")
        );
    }

    @When("the user changes their password with an empty new password")
    public void changePasswordEmptyNew() {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .body(Map.of("oldPassword", "123456", "newPassword", ""))
                        .when()
                        .post("/users/" + testContext.getUserId() + "/change-password")
        );
    }

    @When("the user changes their password to {string}")
    public void changePasswordToShort(String newPassword) {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + testContext.getUserAccessToken())
                        .body(Map.of("oldPassword", "123456", "newPassword", newPassword))
                        .when()
                        .post("/users/" + testContext.getUserId() + "/change-password")
        );
    }

    @When("an unauthenticated user attempts to change a password")
    public void unauthenticatedChangePassword() {
        testContext.setLastResponse(
                given()
                        .contentType(ContentType.JSON)
                        .body(Map.of("oldPassword", "123456", "newPassword", "newpassword"))
                        .when()
                        .post("/users/1/change-password")
        );
    }
}
