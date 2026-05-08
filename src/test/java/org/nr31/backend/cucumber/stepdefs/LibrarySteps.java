package org.nr31.backend.cucumber.stepdefs;

import tools.jackson.databind.JsonNode;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LibrarySteps extends CommonStepDefs {
    @When("I create a library folder with name {string} and no parent")
    public void i_create_a_library_folder_with_name_and_no_parent(String name) throws Exception {
        String body = String.format("{\"name\":\"%s\"}", name);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/files/library/folders", body);
        contextHelper.addValue("response", response);
    }

    @When("I create a library folder with name {string} under parent {string}")
    public void i_create_a_library_folder_with_name_under_parent(String name, String parentIdRef) throws Exception {
        String parentId = resolveVariables(parentIdRef);
        String body = String.format("{\"name\":\"%s\",\"parentId\":\"%s\"}", name, parentId);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/files/library/folders", body);
        contextHelper.addValue("response", response);
    }

    @When("I patch library folder {string} with name {string} and no parent")
    public void i_patch_library_folder_with_name_and_no_parent(String folderIdRef, String name) throws Exception {
        String folderId = resolveVariables(folderIdRef);
        String body = String.format("{\"name\":\"%s\"}", name);
        HttpResponse<String> response = makeApiCall("PATCH", "/api/v1/files/library/folders/" + folderId, body);
        contextHelper.addValue("response", response);
    }

    @When("I patch library folder {string} with name {string} under parent {string}")
    public void i_patch_library_folder_with_name_under_parent(String folderIdRef, String name, String parentIdRef)
            throws Exception {
        String folderId = resolveVariables(folderIdRef);
        String parentId = resolveVariables(parentIdRef);
        String body = String.format("{\"name\":\"%s\",\"parentId\":\"%s\"}", name, parentId);
        HttpResponse<String> response = makeApiCall("PATCH", "/api/v1/files/library/folders/" + folderId, body);
        contextHelper.addValue("response", response);
    }

    @When("I delete library folder {string}")
    public void i_delete_library_folder(String folderIdRef) throws Exception {
        String folderId = resolveVariables(folderIdRef);
        HttpResponse<String> response = makeApiCall("DELETE", "/api/v1/files/library/folders/" + folderId, null);
        contextHelper.addValue("response", response);
    }

    @When("I list library folders at root")
    public void i_list_library_folders_at_root() throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/files/library/folders", null);
        contextHelper.addValue("response", response);
    }

    @When("I list library folders in folder {string}")
    public void i_list_library_folders_in_folder(String folderIdRef) throws Exception {
        String folderId = resolveVariables(folderIdRef);
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/files/library/folders?parentId=" + folderId, null);
        contextHelper.addValue("response", response);
    }

    @When("I upload a PNG file {string} to library root")
    public void i_upload_a_png_file_to_library_root(String fileName) throws Exception {
        byte[] content = generatePngBytes();
        HttpResponse<String> response = uploadLibraryFile(fileName, "image/png", content, null);
        contextHelper.addValue("response", response);
    }

    @When("I upload a PNG file {string} to library folder {string}")
    public void i_upload_a_png_file_to_library_folder(String fileName, String folderIdRef) throws Exception {
        String folderId = resolveVariables(folderIdRef);
        byte[] content = generatePngBytes();
        HttpResponse<String> response = uploadLibraryFile(fileName, "image/png", content, folderId);
        contextHelper.addValue("response", response);
    }

    @When("I upload a text file {string} to library root")
    public void i_upload_a_text_file_to_library_root(String fileName) throws Exception {
        byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);
        HttpResponse<String> response = uploadLibraryFile(fileName, "text/plain", content, null);
        contextHelper.addValue("response", response);
    }

    @When("I list library files at root")
    public void i_list_library_files_at_root() throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/files/library/files", null);
        contextHelper.addValue("response", response);
    }

    @When("I list library files in folder {string}")
    public void i_list_library_files_in_folder(String folderIdRef) throws Exception {
        String folderId = resolveVariables(folderIdRef);
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/files/library/files?folderId=" + folderId, null);
        contextHelper.addValue("response", response);
    }

    @When("I list library files at root with page {int} size {int}")
    public void i_list_library_files_at_root_with_page_and_size(int page, int size) throws Exception {
        HttpResponse<String> response = makeApiCall("GET",
                "/api/v1/files/library/files?page=" + page + "&size=" + size, null);
        contextHelper.addValue("response", response);
    }

    @When("I patch library file {string} with name {string} and no folder")
    public void i_patch_library_file_with_name_and_no_folder(String fileIdRef, String name) throws Exception {
        String fileId = resolveVariables(fileIdRef);
        String body = String.format("{\"name\":\"%s\",\"folderId\":null}", name);
        HttpResponse<String> response = makeApiCall("PATCH", "/api/v1/files/library/files/" + fileId, body);
        contextHelper.addValue("response", response);
    }

    @When("I patch library file {string} with name {string} under folder {string}")
    public void i_patch_library_file_with_name_under_folder(String fileIdRef, String name, String folderIdRef)
            throws Exception {
        String fileId = resolveVariables(fileIdRef);
        String folderId = resolveVariables(folderIdRef);
        String body = String.format("{\"name\":\"%s\",\"folderId\":\"%s\"}", name, folderId);
        HttpResponse<String> response = makeApiCall("PATCH", "/api/v1/files/library/files/" + fileId, body);
        contextHelper.addValue("response", response);
    }

    @When("I delete library file {string}")
    public void i_delete_library_file(String fileIdRef) throws Exception {
        String fileId = resolveVariables(fileIdRef);
        HttpResponse<String> response = makeApiCall("DELETE", "/api/v1/files/library/files/" + fileId, null);
        contextHelper.addValue("response", response);
    }

    @Then("the library file list should contain an entry with name {string}")
    public void the_library_file_list_should_contain_an_entry_with_name(String expectedName) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());

        JsonNode content = root.get("content");
        assertNotNull(content, "Response does not contain 'content' array. Body: " + response.body());
        assertTrue(content.isArray(), "'content' is not an array. Body: " + response.body());

        boolean found = false;
        for (JsonNode node : content) {
            JsonNode nameNode = node.get("name");
            if (nameNode != null && nameNode.asText().equals(expectedName)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected file with name '" + expectedName + "' in list. Body: " + response.body());
    }

    @And("the library file list should not contain an entry with name {string}")
    public void the_library_file_list_should_not_contain_an_entry_with_name(String unexpectedName) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());

        JsonNode content = root.get("content");
        assertNotNull(content, "Response does not contain 'content' array. Body: " + response.body());
        assertTrue(content.isArray(), "'content' is not an array. Body: " + response.body());

        boolean found = false;
        for (JsonNode node : content) {
            JsonNode nameNode = node.get("name");
            if (nameNode != null && nameNode.asText().equals(unexpectedName)) {
                found = true;
                break;
            }
        }
        assertFalse(found, "Did not expect file with name '" + unexpectedName + "' in list. Body: " + response.body());
    }

    @Then("the library folder list should contain an entry with name {string}")
    public void the_library_folder_list_should_contain_an_entry_with_name(String expectedName) throws Exception {
        HttpResponse<String> response = contextHelper.getValue("response");
        JsonNode root = objectMapper.readTree(response.body());

        assertTrue(root.isArray(), "Response is not an array. Body: " + response.body());

        boolean found = false;
        for (JsonNode node : root) {
            JsonNode nameNode = node.get("name");
            if (nameNode != null && nameNode.asText().equals(expectedName)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected folder with name '" + expectedName + "' in list. Body: " + response.body());
    }

    private HttpResponse<String> uploadLibraryFile(String fileName, String contentType,
            byte[] content, String folderId) throws Exception {
        String boundary = UUID.randomUUID().toString();
        byte[] body = buildMultipartBody(boundary, fileName, contentType, content, folderId);

        String url = "http://localhost:" + port + "/api/v1/files/library/files";

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

    private byte[] buildMultipartBody(String boundary, String fileName, String contentType,
            byte[] fileContent, String folderId) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String filePart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        baos.write(filePart.getBytes(StandardCharsets.UTF_8));
        baos.write(fileContent);
        baos.write("\r\n".getBytes(StandardCharsets.UTF_8));

        if (folderId != null) {
            String folderPart = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"folderId\"\r\n\r\n"
                    + folderId + "\r\n";
            baos.write(folderPart.getBytes(StandardCharsets.UTF_8));
        }

        baos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return baos.toByteArray();
    }

    private byte[] generatePngBytes() throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
