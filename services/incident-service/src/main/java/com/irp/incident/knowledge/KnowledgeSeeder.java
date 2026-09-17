package com.irp.incident.knowledge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSeeder.class);

    private final KnowledgeService knowledge;

    public KnowledgeSeeder(KnowledgeService knowledge) {
        this.knowledge = knowledge;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        int loaded = seedClasspath();
        log.info("Knowledge base seeded/updated: {} documents (total={})", loaded, knowledge.count());
    }

    public int seedClasspath() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:knowledge/**/*.md");
        int loaded = 0;
        for (Resource resource : resources) {
            String relative = relativePath(resource);
            String body = resource.getContentAsString(StandardCharsets.UTF_8);
            ParsedDocument doc = parse(relative, body);
            knowledge.upsert(
                    doc.source, doc.path, doc.title, doc.service, doc.team,
                    doc.environment, doc.severity, doc.tags, doc.content
            );
            loaded++;
        }
        return loaded;
    }

    static String relativePath(Resource resource) throws IOException {
        String uri = resource.getURI().toString().replace('\\', '/');
        int idx = uri.lastIndexOf("/knowledge/");
        if (idx >= 0) {
            return uri.substring(idx + 1);
        }
        String filename = resource.getFilename();
        return filename == null ? "knowledge/unknown.md" : "knowledge/" + filename;
    }

    static ParsedDocument parse(String path, String markdown) {
        Map<String, String> meta = new LinkedHashMap<>();
        String content = markdown;
        if (markdown.startsWith("---")) {
            int end = markdown.indexOf("\n---", 3);
            if (end > 0) {
                String front = markdown.substring(3, end).trim();
                for (String line : front.split("\n")) {
                    int colon = line.indexOf(':');
                    if (colon > 0) {
                        meta.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
                    }
                }
                content = markdown.substring(end + 4).trim();
            }
        }
        String title = meta.getOrDefault("title", path);
        return new ParsedDocument(
                meta.getOrDefault("source", "runbook"),
                path.replace('\\', '/'),
                title,
                emptyToNull(meta.get("service")),
                emptyToNull(meta.get("team")),
                emptyToNull(meta.get("environment")),
                emptyToNull(meta.get("severity")),
                emptyToNull(meta.get("tags")),
                content
        );
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    record ParsedDocument(
            String source,
            String path,
            String title,
            String service,
            String team,
            String environment,
            String severity,
            String tags,
            String content
    ) {}
}
