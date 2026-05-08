package org.nr31.backend.cucumber.stepdefs;

import tools.jackson.databind.JsonNode;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilesUploadSteps extends CommonStepDefs {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private byte[] lastUploadedContent;

    @Given("I find role ID for {string} as {string}")
    public void i_find_role_id_for_as(String name, String varName) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = ?",
                Long.class,
                name
        );
        contextHelper.addValue(varName, id);
    }

    @When("I upload a PNG file {string} as {string}")
    public void i_upload_a_png_file_as(String fileName, String scope) throws Exception {
        lastUploadedContent = generatePngBytes();
        HttpResponse<String> response = uploadFile(fileName, "image/png", lastUploadedContent, scope);
        contextHelper.addValue("response", response);
    }

    @When("I upload the same PNG file {string} as {string}")
    public void i_upload_the_same_png_file_as(String fileName, String scope) throws Exception {
        HttpResponse<String> response = uploadFile(fileName, "image/png", lastUploadedContent, scope);
        contextHelper.addValue("response", response);
    }

    @When("I upload a JPEG file {string} as {string}")
    public void i_upload_a_jpeg_file_as(String fileName, String scope) throws Exception {
        byte[] content = generateJpegBytes();
        HttpResponse<String> response = uploadFile(fileName, "image/jpeg", content, scope);
        contextHelper.addValue("response", response);
    }

    @When("I upload a WEBP file {string} as {string}")
    public void i_upload_a_webp_file_as(String fileName, String scope) throws Exception {
        // WEBP isn't supported by ImageIO, so use a minimal valid WEBP binary
        byte[] content = minimalWebpBytes();
        HttpResponse<String> response = uploadFile(fileName, "image/webp", content, scope);
        contextHelper.addValue("response", response);
    }

    @When("I upload an empty file as {string}")
    public void i_upload_an_empty_file_as(String scope) throws Exception {
        HttpResponse<String> response = uploadFile("empty.png", "image/png", new byte[0], scope);
        contextHelper.addValue("response", response);
    }

    @When("I upload a text file {string} as {string}")
    public void i_upload_a_text_file_as(String fileName, String scope) throws Exception {
        byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
        HttpResponse<String> response = uploadFile(fileName, "text/plain", content, scope);
        contextHelper.addValue("response", response);
    }

    @When("I get file {string}")
    public void i_get_file(String fileIdRef) throws Exception {
        String fileId = resolveVariables(fileIdRef);
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/files/" + fileId, null);
        contextHelper.addValue("response", response);
    }

    @When("I get file {string} with width {int}")
    public void i_get_file_with_width(String fileIdRef, int width) throws Exception {
        String fileId = resolveVariables(fileIdRef);
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/files/" + fileId + "?w=" + width, null);
        contextHelper.addValue("response", response);
    }

    @When("I delete file {string}")
    public void i_delete_file(String fileIdRef) throws Exception {
        String fileId = resolveVariables(fileIdRef);
        HttpResponse<String> response = makeApiCall("DELETE", "/api/v1/files/" + fileId, null);
        contextHelper.addValue("response", response);
    }

    @Then("the response should have header {string} starting with {string}")
    public void the_response_should_have_header_starting_with(String headerName, String prefix) {
        HttpResponse<String> response = contextHelper.getValue("response");
        String value = response.headers().firstValue(headerName.toLowerCase()).orElse(null);
        assertNotNull(value, "Header '" + headerName + "' not found in response");
        assertTrue(value.startsWith(prefix),
                "Expected header '" + headerName + "' to start with '" + prefix + "' but was '" + value + "'");
    }

    @Then("the response should have header {string} with value {string}")
    public void the_response_should_have_header_with_value(String headerName, String expectedValue) {
        HttpResponse<String> response = contextHelper.getValue("response");
        String value = response.headers().firstValue(headerName.toLowerCase()).orElse(null);
        assertNotNull(value, "Header '" + headerName + "' not found in response");
        assertEquals(expectedValue, value);
    }

    @Then("files {string} and {string} should have different UUIDs")
    public void files_should_have_different_uuids(String ref1, String ref2) {
        String id1 = resolveVariables(ref1);
        String id2 = resolveVariables(ref2);
        assertNotEquals(id1, id2, "Expected different UUIDs but both were: " + id1);
    }

    @Then("files {string} and {string} should have the same stored hash")
    public void files_should_have_the_same_stored_hash(String ref1, String ref2) {
        String id1 = resolveVariables(ref1);
        String id2 = resolveVariables(ref2);
        String hash1 = jdbcTemplate.queryForObject(
                "SELECT stored_name FROM files_metadata WHERE id = ?::uuid", String.class, id1);
        String hash2 = jdbcTemplate.queryForObject(
                "SELECT stored_name FROM files_metadata WHERE id = ?::uuid", String.class, id2);
        assertEquals(hash1, hash2, "Expected same stored hash but got: " + hash1 + " vs " + hash2);
    }

    @Then("the response body should contain {string} with value containing {string}")
    public void the_response_body_should_contain_field_with_value_containing(String field, String substring) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());
        assertNotNull(root.get(field), "Field '" + field + "' not found in: " + response.body());
        String value = root.get(field).asString();
        assertTrue(value.toLowerCase().contains(substring.toLowerCase()),
                "Expected field '" + field + "' to contain '" + substring + "' but was: " + value);
    }

    private HttpResponse<String> uploadFile(String fileName, String contentType, byte[] content, String scope) throws Exception {
        String boundary = UUID.randomUUID().toString();
        byte[] body = buildMultipartBody(boundary, "file", fileName, contentType, content);

        String path = scope.equals("library") ? "library/files" : scope;
        String url = "http://localhost:" + port + "/api/v1/files/" + path;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);

        String token = contextHelper.getValue("jwt_token");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        builder.POST(HttpRequest.BodyPublishers.ofByteArray(body));
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private byte[] buildMultipartBody(String boundary, String fieldName, String fileName, String contentType, byte[] content) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String header = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n" +
                "Content-Type: " + contentType + "\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        baos.write(header.getBytes(StandardCharsets.UTF_8));
        baos.write(content);
        baos.write(footer.getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
    }

    private byte[] generatePngBytes() throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private byte[] generateJpegBytes() throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    private byte[] minimalWebpBytes() {
        // Minimal valid WEBP file (RIFF header + WEBP signature + VP8 chunk)
        return new byte[]{
                0x52, 0x49, 0x46, 0x46,  // "RIFF"
                0x24, 0x00, 0x00, 0x00,  // file size - 8
                0x57, 0x45, 0x42, 0x50,  // "WEBP"
                0x56, 0x50, 0x38, 0x20,  // "VP8 "
                0x18, 0x00, 0x00, 0x00,  // chunk size
                0x30, 0x01, 0x00, (byte) 0x9D,
                0x01, 0x2A, 0x01, 0x00,
                0x01, 0x00, 0x01, 0x40,
                0x25, (byte) 0xA4, 0x00, 0x03,
                0x70, 0x00, (byte) 0xFE, (byte) 0xFB,
                (byte) 0x94, 0x00, 0x00, 0x00
        };
    }
}
