package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.java.en.When;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class EmailVerificationSteps extends CommonStepDefs {

    @When("I register with username {string}, email {string}, and password {string}")
    public void i_register_with_username_email_and_password(String username, String email, String password) throws Exception {
        String body = String.format("{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}", username, email, password);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/register", body);
        contextHelper.addValue("response", response);
    }

    @When("I retrieve the verification token for email {string} from Mailpit")
    public void i_retrieve_the_verification_token_for_email(String email) throws Exception {
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

        assertNotNull(messageId, "No verification email found in Mailpit for " + email);

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
            contextHelper.addValue("verification_token", token);
        } else {
            fail("Verification token pattern not found in HTML email body");
        }
    }

    @When("I verify the email with the retrieved token")
    public void i_verify_the_email_with_retrieved_token() throws Exception {
        String token = contextHelper.getValue("verification_token");
        assertNotNull(token, "No verification token found in scenario context");
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/verify-email?token=" + token, null);
        contextHelper.addValue("response", response);
    }

    @When("I verify the email with token {string}")
    public void i_verify_the_email_with_token(String token) throws Exception {
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/verify-email?token=" + token, null);
        contextHelper.addValue("response", response);
    }

    @When("I set the feature switch {string} to {string}")
    public void i_set_the_feature_switch_to(String featureName, String enabledStr) throws Exception {
        boolean enabled = Boolean.parseBoolean(enabledStr);

        // Fetch current config first
        HttpResponse<String> getResponse = makeAdminApiCall("GET", "/api/v1/admin/config/feature_switches", null);
        assertEquals(200, getResponse.statusCode(), "Failed to fetch feature switches: " + getResponse.body());

        JsonNode root = objectMapper.readTree(getResponse.body());
        ArrayNode arrayNode = (ArrayNode) root.get("configValue");

        boolean found = false;
        for (JsonNode node : arrayNode) {
            if (node.has("name") && featureName.equals(node.get("name").asString())) {
                ((ObjectNode) node).put("enabled", enabled);
                found = true;
                break;
            }
        }

        if (!found) {
            ObjectNode newNode = objectMapper.createObjectNode();
            newNode.put("name", featureName);
            newNode.put("enabled", enabled);
            arrayNode.add(newNode);
        }

        // Prepare update config payload
        ObjectNode updatePayload = objectMapper.createObjectNode();
        updatePayload.put("name", "feature_switches");
        updatePayload.set("description", root.get("description"));
        updatePayload.set("configValue", arrayNode);

        HttpResponse<String> putResponse = makeAdminApiCall("PUT", "/api/v1/admin/config/feature_switches", updatePayload.toString());
        assertEquals(200, putResponse.statusCode(), "Failed to update feature switches: " + putResponse.body());
    }

    @When("I request to resend the verification email for {string}")
    public void i_request_to_resend_the_verification_email_for(String email) throws Exception {
        String body = String.format("{\"email\":\"%s\"}", email);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/auth/resend-verification", body);
        contextHelper.addValue("response", response);
    }

    @When("I clear Mailpit messages")
    public void i_clear_mailpit_messages() throws Exception {
        String mailpitHost = System.getProperty("mailpit.host", "localhost");
        String mailpitPort = System.getProperty("mailpit.http.port", "8025");
        String mailpitUrl = "http://" + mailpitHost + ":" + mailpitPort + "/api/v1/messages";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(mailpitUrl))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                "Failed to clear Mailpit messages: " + response.body());
    }
}
