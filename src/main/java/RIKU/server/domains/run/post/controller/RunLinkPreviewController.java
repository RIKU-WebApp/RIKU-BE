package RIKU.server.domains.run.post.controller;

import RIKU.server.domains.run.post.entity.PostStatus;
import RIKU.server.domains.run.post.entity.PostType;
import RIKU.server.domains.run.post.repository.PostRepository;
import RIKU.server.global.entity.BaseStatus;
import java.net.URI;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class RunLinkPreviewController {
    private static final DateTimeFormatter SCHEDULE = DateTimeFormatter.ofPattern("M월 d일 (E) HH:mm", Locale.KOREAN)
            .withZone(ZoneId.of("Asia/Seoul"));
    private final PostRepository posts;
    private final URI site;

    public RunLinkPreviewController(PostRepository posts, @Value("${riku.web-origin:https://www.riku.run}") URI site) {
        this.posts = posts;
        this.site = site;
    }

    @GetMapping(value = "/public/run-preview/{kind:regular|flash|training|event}/{id:[0-9]+}",
            produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@PathVariable String kind, @PathVariable long id) {
        var type = PostType.valueOf(kind.toUpperCase(Locale.ROOT));
        return posts.findSharePreviewByIdAndPostTypeAndStatusAndPostStatusNot(id, type, BaseStatus.ACTIVE, PostStatus.CANCELED)
                .map(post -> {
                    String url = site.resolve("/" + kind + "/" + id).toString();
                    String description = SCHEDULE.format(post.date().toInstant(ZoneOffset.UTC)) + "  " + post.location();
                    String title = HtmlUtils.htmlEscape(post.title());
                    String html = """
                            <!doctype html><html lang="ko"><head><meta charset="utf-8">
                            <title>%s</title>
                            <meta property="og:type" content="website">
                            <meta property="og:site_name" content="RIKU">
                            <meta property="og:url" content="%s">
                            <meta property="og:title" content="%s">
                            <meta property="og:description" content="%s">
                            <meta property="og:image" content="%s">
                            </head><body><a href="%s">%s</a></body></html>
                            """.formatted(title, url, title, HtmlUtils.htmlEscape(description),
                            HtmlUtils.htmlEscape(cover(post.postImageUrl())), url, title);
                    return ResponseEntity.ok().header("Vary", "User-Agent").header("Cache-Control", "no-store")
                            .contentType(MediaType.parseMediaType("text/html;charset=UTF-8")).body(html);
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private String cover(String value) {
        if (value != null && !value.isBlank()) {
            try {
                URI image = URI.create(value);
                if (image.getHost() != null && ("https".equals(image.getScheme()) || "http".equals(image.getScheme())))
                    return image.toASCIIString();
            } catch (IllegalArgumentException ignored) {
                // Older posts can reference a malformed or missing cover.
            }
        }
        return site.resolve("/share-cover.jpg").toString();
    }
}
