package cn.ethanwu.autoincrementslug.reconciler;

import cn.ethanwu.autoincrementslug.config.ConfigConstant;
import cn.ethanwu.autoincrementslug.service.AISConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.halo.app.core.extension.content.Post;
import run.halo.app.extension.ExtensionClient;
import run.halo.app.extension.controller.Controller;
import run.halo.app.extension.controller.ControllerBuilder;
import run.halo.app.extension.controller.Reconciler;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostReconciler implements Reconciler<Reconciler.Request> {

    private final ExtensionClient client;
    private final AISConfigService aisConfigService;

    @Override
    public Result reconcile(Request request) {
        Post post = client.fetch(Post.class, request.name()).orElse(null);
        if (post == null || post.getMetadata().getDeletionTimestamp() != null) {
            return new Result(false, null);
        }

        Map<String, String> annotations = post.getMetadata().getAnnotations();
        if (annotations != null && ConfigConstant.ANNOTATION_VALUE_TRUE.equals(
                annotations.get(ConfigConstant.ANNOTATION_PROCESSED))) {
            return new Result(false, null);
        }

        String slug;
        try {
            slug = aisConfigService.generateSlug();
        } catch (Exception e) {
            log.warn("Failed to generate slug, retrying...", e);
            return new Result(true, Duration.ofSeconds(1));
        }

        post.getSpec().setSlug(slug);

        if (post.getMetadata().getAnnotations() == null) {
            post.getMetadata().setAnnotations(new HashMap<>());
        }
        post.getMetadata().getAnnotations()
                .put(ConfigConstant.ANNOTATION_PROCESSED, ConfigConstant.ANNOTATION_VALUE_TRUE);

        try {
            client.update(post);
            log.info("Updated post {} slug to {}", post.getMetadata().getName(), slug);
        } catch (Exception e) {
            log.error("Failed to update post {}", post.getMetadata().getName(), e);
            return new Result(true, Duration.ofSeconds(1));
        }

        return new Result(false, null);
    }

    @Override
    public Controller setupWith(ControllerBuilder builder) {
        return builder.extension(new Post()).build();
    }
}
