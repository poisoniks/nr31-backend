package org.nr31.backend.cucumber.stepdefs;

import io.cucumber.java.en.When;
import java.net.http.HttpResponse;

public class KbStepDefs extends CommonStepDefs {

    @When("I retrieve the root folders")
    public void i_retrieve_the_root_folders() throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/kb/folders/root", null);
        contextHelper.addValue("response", response);
    }

    @When("I retrieve the folder details for {string}")
    public void i_retrieve_the_folder_details_for(String slug) throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/kb/folders/" + slug, null);
        contextHelper.addValue("response", response);
    }

    @When("I retrieve the folder details for {string} with pagination size {int}")
    public void i_retrieve_the_folder_details_for_with_pagination_size(String slug, int size) throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/kb/folders/" + slug + "?size=" + size, null);
        contextHelper.addValue("response", response);
    }

    @When("I retrieve the article details for {string}")
    public void i_retrieve_the_article_details_for(String slug) throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/kb/articles/" + slug, null);
        contextHelper.addValue("response", response);
    }

    @When("I search articles with query {string}")
    public void i_search_articles_with_query(String query) throws Exception {
        HttpResponse<String> response = makeApiCall("GET", "/api/v1/kb/search?q=" + query, null);
        contextHelper.addValue("response", response);
    }

    @When("I create a new folder named {string} and restricted is {word}")
    public void i_create_a_new_folder_named_and_restricted_is(String name, String restricted) throws Exception {
        String body = String.format("{\"name\": {\"en\": \"%s\"}, \"restricted\": %s}", name, restricted);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/kb/folders", body);
        contextHelper.addValue("response", response);
    }

    @When("I create a sub-folder named {string} under parent {int} and restricted is {word}")
    public void i_create_a_sub_folder_named_under_parent_and_restricted_is(String name, int parentId, String restricted) throws Exception {
        String body = String.format("{\"name\": {\"en\": \"%s\"}, \"parentId\": %d, \"restricted\": %s}", name, parentId, restricted);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/kb/folders", body);
        contextHelper.addValue("response", response);
    }

    @When("I rename folder {int} to {string} and set restricted to {word}")
    public void i_rename_folder_to_and_set_restricted_to(int folderId, String name, String restricted) throws Exception {
        String body = String.format("{\"name\": {\"en\": \"%s\"}, \"restricted\": %s}", name, restricted);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/kb/folders/" + folderId, body);
        contextHelper.addValue("response", response);
    }

    @When("I update folder {int} parent to root")
    public void i_update_folder_parent_to_root(int folderId) throws Exception {
        String body = "{\"parentId\": -1}";
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/kb/folders/" + folderId, body);
        contextHelper.addValue("response", response);
    }

    @When("I set folder {int} parent to {int}")
    public void i_set_folder_parent_to(int folderId, int parentId) throws Exception {
        String body = String.format("{\"parentId\": %d}", parentId);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/kb/folders/" + folderId, body);
        contextHelper.addValue("response", response);
    }

    @When("I delete folder {int}")
    public void i_delete_folder(int folderId) throws Exception {
        HttpResponse<String> response = makeApiCall("DELETE", "/api/v1/kb/folders/" + folderId, null);
        contextHelper.addValue("response", response);
    }

    @When("I attempt to create a folder named {string}")
    public void i_attempt_to_create_a_folder_named(String name) throws Exception {
        String body = String.format("{\"name\": {\"en\": \"%s\"}}", name);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/kb/folders", body);
        contextHelper.addValue("response", response);
    }

    @When("I create a folder with an invalid payload missing the name")
    public void i_create_a_folder_with_an_invalid_payload_missing_the_name() throws Exception {
        String body = "{\"restricted\": false}";
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/kb/folders", body);
        contextHelper.addValue("response", response);
    }

    @When("I create a new article in folder {int} with title {string} and content {string}")
    public void i_create_a_new_article_in_folder_with_title_and_content(int folderId, String title, String content) throws Exception {
        String body = String.format("{\"folderId\": %d, \"title\": {\"en\": \"%s\"}, \"content\": {\"en\": {\"type\": \"doc\", \"content\": [{\"type\": \"paragraph\", \"content\": [{\"type\": \"text\", \"text\": \"%s\"}]}]}}}", folderId, title, content);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/kb/articles", body);
        contextHelper.addValue("response", response);
    }

    @When("I update article {int} with title {string} and content {string}")
    public void i_update_article_with_title_and_content(int articleId, String title, String content) throws Exception {
        String body = String.format("{\"title\": {\"en\": \"%s\"}, \"content\": {\"en\": {\"type\": \"doc\", \"content\": [{\"type\": \"paragraph\", \"content\": [{\"type\": \"text\", \"text\": \"%s\"}]}]}}}", title, content);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/kb/articles/" + articleId, body);
        contextHelper.addValue("response", response);
    }

    @When("I update article {int} with title {string}")
    public void i_update_article_with_title(int articleId, String title) throws Exception {
        String body = String.format("{\"title\": {\"en\": \"%s\"}}", title);
        HttpResponse<String> response = makeApiCall("PUT", "/api/v1/kb/articles/" + articleId, body);
        contextHelper.addValue("response", response);
    }

    @When("I delete article {int}")
    public void i_delete_article(int articleId) throws Exception {
        HttpResponse<String> response = makeApiCall("DELETE", "/api/v1/kb/articles/" + articleId, null);
        contextHelper.addValue("response", response);
    }

    @When("I attempt to create an article in folder {int} with title {string}")
    public void i_attempt_to_create_an_article_in_folder_with_title(int folderId, String title) throws Exception {
        String body = String.format("{\"folderId\": %d, \"title\": {\"en\": \"%s\"}, \"content\": {\"en\": {\"type\": \"doc\"}}}", folderId, title);
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/kb/articles", body);
        contextHelper.addValue("response", response);
    }

    @When("I create an article with an invalid payload missing folderId")
    public void i_create_an_article_with_an_invalid_payload_missing_folderId() throws Exception {
        String body = "{\"title\": {\"en\": \"Missing Folder Article\"}, \"content\": {\"en\": {\"type\": \"doc\"}}}";
        HttpResponse<String> response = makeApiCall("POST", "/api/v1/kb/articles", body);
        contextHelper.addValue("response", response);
    }
}
