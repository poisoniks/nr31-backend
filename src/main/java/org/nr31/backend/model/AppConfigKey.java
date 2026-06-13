package org.nr31.backend.model;

import lombok.Getter;

@Getter
public enum AppConfigKey {
    ALLOWED_MIME_TYPES("allowed_mime_types"),
    CMS_RICHTEXT_MAX_SIZE_BYTES("cms.richtext.max_size_bytes"),
    CMS_NEWSFEED_MAX_ITEMS("cms.newsfeed.max_items"),
    CMS_SLOT_RESTRICTIONS("cms_slot_restrictions"),
    KB_SEARCH_PRECISION("kb_search_precision"),
    FEATURE_SWITCHES("feature_switches"),
    FETCH_SCHEDULED_DISCORD_EVENTS_GUILD_ID("fetch_scheduled_discord_events_guild_id"),
    ROSTER_EXPORT_TEMPLATE_FILE_ID("roster_export_template_file_id");

    private final String key;

    AppConfigKey(String key) {
        this.key = key;
    }
}
