package RIKU.server.domains.run.post;

import RIKU.server.domains.run.post.entity.Post;
import RIKU.server.domains.run.post.entity.PostStatus;
import RIKU.server.domains.run.post.entity.PostType;
import RIKU.server.domains.run.post.repository.PostRepository;
import RIKU.server.global.entity.BaseStatus;
import java.time.LocalDateTime;
import java.util.Locale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class RunLinkPreviewTest {
    @Autowired MockMvc mvc;
    @Autowired PostRepository posts;

    @ParameterizedTest @EnumSource(PostType.class)
    void exposesOnlyCardFieldsForPublishedPosts(PostType type) throws Exception {
        var date = LocalDateTime.of(2026, 11, 8, 16, 30);
        String image = "https://example.org/cover.jpg";
        var post = posts.save(Post.create(null, "Run <A>", "정문", date, "private body", image, type));
        String kind = type.name().toLowerCase(Locale.ROOT);
        String path = "/public/run-preview/" + kind + "/" + post.getId();
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Run &lt;A&gt;")))
                .andExpect(content().string(containsString("11월 9일 (월) 01:30  정문")))
                .andExpect(content().string(containsString("content=\"" + image + "\"")))
                .andExpect(content().string(not(containsString("private body"))));
        String otherKind = type == PostType.FLASH ? "event" : "flash";
        mvc.perform(get("/public/run-preview/" + otherKind + "/" + post.getId())).andExpect(status().isNotFound());
        post.updatePost("Run", "정문", date, "", null);
        mvc.perform(get(path)).andExpect(content().string(containsString("/share-cover.jpg")));
        post.updatePostStatus(PostStatus.CANCELED);
        mvc.perform(get(path)).andExpect(status().isNotFound());
        post.updatePostStatus(PostStatus.CLOSED);
        post.updateStatus(BaseStatus.INACTIVE);
        mvc.perform(get(path)).andExpect(status().isNotFound());
        mvc.perform(get("/run/" + kind + "/post/" + post.getId())).andExpect(status().isUnauthorized());
    }
}
