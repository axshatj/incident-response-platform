package com.irp.incident.knowledge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledge;

    public KnowledgeController(KnowledgeService knowledge) {
        this.knowledge = knowledge;
    }

    @GetMapping("/search")
    public List<KnowledgeHit> searchGet(
            @RequestParam @NotBlank String query,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) Integer topK) {
        return knowledge.search(query, service, environment, topK);
    }

    @PostMapping("/search")
    public List<KnowledgeHit> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        return knowledge.search(request.query(), request.service(), request.environment(), request.topK());
    }
}
