package com.irp.agent.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StructuredLlmTest {

    @Test
    void extractJsonStripsMarkdownFences() {
        String raw = """
                ```json
                {"severity":"SEV2"}
                ```
                """;
        assertThat(StructuredLlm.extractJson(raw)).isEqualTo("{\"severity\":\"SEV2\"}");
    }

    @Test
    void extractJsonRejectsPlainText() {
        assertThatThrownBy(() -> StructuredLlm.extractJson("no json here"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
