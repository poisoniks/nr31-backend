package org.nr31.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.YoutubeVideoDto;
import org.nr31.backend.model.PageRevision;
import org.nr31.backend.model.RevisionStatus;
import org.nr31.backend.repository.PageRevisionRepository;
import org.nr31.backend.service.YouTubeService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class YouTubeServiceImpl implements YouTubeService {

    private final PageRevisionRepository pageRevisionRepository;

    private static final String ATOM_NS   = "http://www.w3.org/2005/Atom";
    private static final String YT_NS     = "http://www.youtube.com/xml/schemas/2015";
    private static final String FEED_URL  = "https://www.youtube.com/feeds/videos.xml?channel_id=";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    @Cacheable(value = "youtubeLatestVideo", key = "#channelId")
    public Optional<YoutubeVideoDto> getLatestVideo(String channelId) {
        // Cache miss — this should normally not be hit at runtime because the
        // scheduled job pre-warms the cache, but it serves as a fallback.
        log.info("Cache miss for channel {} — fetching on-demand", channelId);
        return fetchLatestVideoFromFeed(channelId);
    }

    @Override
    @CacheEvict(value = "youtubeLatestVideo", key = "#channelId")
    public void evictCache(String channelId) {
        log.debug("Evicted YouTube cache for channel {}", channelId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getTrackedChannelIds() {
        Set<String> channelIds = new HashSet<>();

        // Scan published revisions first, then drafts (editors may add new
        // YouTube widgets that haven't been published yet).
        List<PageRevision> revisions = pageRevisionRepository.findAllByStatus(RevisionStatus.PUBLISHED);
        revisions.forEach(r -> extractChannelIds(r.getLayoutData(), channelIds));

        List<PageRevision> drafts = pageRevisionRepository.findAllByStatus(RevisionStatus.DRAFT);
        drafts.forEach(r -> extractChannelIds(r.getLayoutData(), channelIds));

        return channelIds;
    }

    private Optional<YoutubeVideoDto> fetchLatestVideoFromFeed(String channelId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FEED_URL + channelId))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/atom+xml")
                    .GET()
                    .build();

            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                log.warn("YouTube feed returned HTTP {} for channel {}", response.statusCode(), channelId);
                return Optional.empty();
            }

            return parseFeed(response.body(), channelId);

        } catch (Exception e) {
            log.error("Error fetching YouTube feed for channel {}: {}", channelId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Parses an Atom feed XML stream and returns the most recently published entry.
     * The feed is already sorted newest-first by YouTube, so we just take the first entry.
     */
    private Optional<YoutubeVideoDto> parseFeed(InputStream xml, String channelId) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // Harden against XXE
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xml);

        String feedAuthor = textContent(doc.getDocumentElement(), ATOM_NS, "name");

        NodeList entries = doc.getDocumentElement().getElementsByTagNameNS(ATOM_NS, "entry");
        if (entries.getLength() == 0) {
            log.info("No entries in YouTube feed for channel {}", channelId);
            return Optional.empty();
        }

        // First entry is the most recent (YouTube guarantees this ordering)
        Element entry = (Element) entries.item(0);

        String videoId    = textContentNS(entry, YT_NS, "videoId");
        String title      = textContentNS(entry, ATOM_NS, "title");
        String link       = linkHref(entry);
        String author     = textContentNS(entry, ATOM_NS, "name");
        String publishedStr = textContentNS(entry, ATOM_NS, "published");

        if (author.isEmpty()) {
            author = feedAuthor;
        }

        if (videoId.isEmpty() || title.isEmpty()) {
            log.warn("Incomplete entry in YouTube feed for channel {}", channelId);
            return Optional.empty();
        }

        OffsetDateTime published = publishedStr.isEmpty()
                ? OffsetDateTime.now()
                : OffsetDateTime.parse(publishedStr);

        boolean isShort = link.contains("/shorts/");

        YoutubeVideoDto dto = YoutubeVideoDto.builder()
                .videoId(videoId)
                .title(title)
                .link("https://www.youtube.com/watch?v=" + videoId)
                .published(published)
                .author(author)
                .isShort(isShort)
                .channelId(channelId)
                .build();

        log.info("Fetched latest video for channel {}: '{}' ({})", channelId, title, videoId);
        return Optional.of(dto);
    }

    /** Extracts the href of the first {@code <link rel="alternate">} child element. */
    private String linkHref(Element parent) {
        NodeList links = parent.getElementsByTagNameNS(ATOM_NS, "link");
        for (int i = 0; i < links.getLength(); i++) {
            Element el = (Element) links.item(i);
            if ("alternate".equals(el.getAttribute("rel"))) {
                return el.getAttribute("href");
            }
        }
        return "";
    }

    /** Returns the text content of the first matching namespaced child, or "". */
    private String textContentNS(Element parent, String ns, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS(ns, localName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }

    /** Searches the full document for the first matching tag (used for feed-level fields). */
    private String textContent(Element root, String ns, String localName) {
        NodeList nodes = root.getElementsByTagNameNS(ns, localName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }

    /** Scans a page revision's JSONB layout for {@code "type": "youtube"} widgets. */
    private void extractChannelIds(JsonNode layoutData, Set<String> channelIds) {
        if (layoutData == null) {
            return;
        }
        JsonNode slots = layoutData.path("slots");
        if (!slots.isArray()) {
            return;
        }
        for (JsonNode slot : slots) {
            JsonNode widgets = slot.path("widgets");
            if (!widgets.isArray()) {
                continue;
            }
            for (JsonNode widget : widgets) {
                if ("youtube".equals(widget.path("type").asText(""))) {
                    String id = widget.path("channelId").asText("");
                    if (!id.isEmpty()) {
                        channelIds.add(id);
                    }
                }
            }
        }
    }
}
