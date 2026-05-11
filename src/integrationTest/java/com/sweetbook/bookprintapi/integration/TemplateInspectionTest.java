package com.sweetbook.bookprintapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.sweetbook.bookprintapi.ListResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.Map;

@EnabledIfEnvironmentVariable(named = "BOOKPRINT_API_KEY", matches = ".+")
class TemplateInspectionTest extends IntegrationTestBase {

    /**
     * sandbox 99번에 어떤 템플릿이 살아있는지 진단용. Cover/Contents 통합 테스트의
     * 전제로 사용 가능한 templateUid가 있는지 확인.
     */
    @Test
    void listTemplates_byKind() {
        // 서버 enum (TemplatesController.cs:120): cover / content / divider / publish
        for (String kind : new String[] {"cover", "content", "divider", "publish"}) {
            Map<String, Object> q = new HashMap<>();
            q.put("templateKind", kind);
            q.put("limit", 5);
            ListResult<Map<String, Object>> result = client.templates.list(q);
            System.out.println("[templates kind=" + kind + "] count="
                    + result.items().size() + " (total=" + result.pagination().total() + ")");
            for (Map<String, Object> t : result.items()) {
                System.out.println("  - " + t.get("templateUid") + " | "
                        + t.get("templateName") + " | bookSpec=" + t.get("bookSpecUid"));
            }
        }
    }

    @Test
    void listTemplates_globalFirstPage() {
        ListResult<Map<String, Object>> result = client.templates.list();
        System.out.println("[templates all] first " + Math.min(result.items().size(), 10) + " of " + result.pagination().total());
        for (int i = 0; i < Math.min(result.items().size(), 10); i++) {
            Map<String, Object> t = result.items().get(i);
            System.out.println("  - " + t.get("templateUid") + " | kind=" + t.get("templateKind") + " | spec=" + t.get("bookSpecUid"));
        }
    }
}
