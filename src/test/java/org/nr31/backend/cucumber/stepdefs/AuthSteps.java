package org.nr31.backend.cucumber.stepdefs;

import tools.jackson.databind.JsonNode;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class AuthSteps extends CommonStepDefs {

    @Given("I log in with user {string} and password {string}")
    public void i_log_in_with_user_and_password(String username, String password) throws Exception {
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);

        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/login", body);
        contextHelper.addValue("response", response);

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            String token = root.get("accessToken").asString();
            contextHelper.addValue("jwt_token", token);
        }
    }

    @When("I refresh the token with {string}")
    public void i_refresh_the_token_with(String tokenRef) throws Exception {
        String token = resolveVariables(tokenRef);
        String body = String.format("{\"refreshToken\":\"%s\"}", token);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/refresh", body);
        contextHelper.addValue("response", response);
    }

    @When("I log out using the token {string}")
    public void i_log_out_using_the_token(String tokenRef) throws Exception {
        String token = resolveVariables(tokenRef);
        String body = String.format("{\"refreshToken\":\"%s\"}", token);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/logout", body);
        contextHelper.addValue("response", response);
    }

    @When("I change the password with current password {string} and new password {string}")
    public void i_change_the_password_with_current_password_and_new_password(String currentPassword, String newPassword) throws Exception {
        String body = String.format("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}", currentPassword, newPassword);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/auth/password", body);
        contextHelper.addValue("response", response);
    }

    @When("I request a password reset for email {string}")
    public void i_request_a_password_reset_for_email(String email) throws Exception {
        String body = String.format("{\"email\":\"%s\"}", email);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/forgot-password", body);
        contextHelper.addValue("response", response);
    }

    @When("I retrieve the password reset token for email {string} from Mailpit")
    public void i_retrieve_the_password_reset_token_for_email(String email) throws Exception {
        String mailpitHost = System.getProperty("mailpit.host", "localhost");
        String mailpitPort = System.getProperty("mailpit.http.port", "8025");
        String mailpitUrl = "http://" + mailpitHost + ":" + mailpitPort + "/api/v1/messages";

        String messageId = null;
        long startTime = System.currentTimeMillis();
        long timeout = 10000; // 10 seconds timeout

        while (System.currentTimeMillis() - startTime < timeout) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mailpitUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode messages = root.get("messages");

            if (messages != null && messages.isArray()) {
                for (JsonNode msg : messages) {
                    JsonNode toArray = msg.get("To");
                    if (toArray != null && toArray.isArray() && !toArray.isEmpty()) {
                        String toAddress = toArray.get(0).get("Address").asString();
                        if (email.equalsIgnoreCase(toAddress)) {
                            messageId = msg.get("ID").asString();
                            break;
                        }
                    }
                }
            }

            if (messageId != null) {
                break;
            }

            Thread.sleep(200);
        }

        assertNotNull(messageId, "No password reset email found in Mailpit for " + email);

        // Get the message HTML content to extract the token
        String msgUrl = "http://" + mailpitHost + ":" + mailpitPort + "/api/v1/message/" + messageId;
        HttpRequest msgRequest = HttpRequest.newBuilder()
                .uri(URI.create(msgUrl))
                .GET()
                .build();
        HttpResponse<String> msgResponse = httpClient.send(msgRequest, HttpResponse.BodyHandlers.ofString());
        JsonNode msgRoot = objectMapper.readTree(msgResponse.body());
        String htmlContent = msgRoot.get("HTML").asString();

        // Extract UUID token using pattern matching
        Pattern pattern = Pattern.compile("token=([a-f0-9\\-]{36})");
        Matcher matcher = pattern.matcher(htmlContent);
        if (matcher.find()) {
            String token = matcher.group(1);
            contextHelper.addValue("password_reset_token", token);
        } else {
            fail("Password reset token pattern not found in HTML email body");
        }
    }

    @When("I reset the password with the retrieved token and new password {string}")
    public void i_reset_the_password_with_retrieved_token_and_new_password(String newPassword) throws Exception {
        String token = contextHelper.getValue("password_reset_token");
        assertNotNull(token, "No password reset token found in scenario context");
        String body = String.format("{\"token\":\"%s\",\"newPassword\":\"%s\"}", token, newPassword);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/reset-password", body);
        contextHelper.addValue("response", response);
    }

    @When("I reset the password with token {string} and new password {string}")
    public void i_reset_the_password_with_token_and_new_password(String token, String newPassword) throws Exception {
        String body = String.format("{\"token\":\"%s\",\"newPassword\":\"%s\"}", token, newPassword);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/reset-password", body);
        contextHelper.addValue("response", response);
    }
}
