package com.pullcat.service.analysis;

import com.pullcat.config.GitHubConfig;
import com.pullcat.model.FileContent;
import com.pullcat.model.PRMetadata;
import com.pullcat.service.github.GitHubApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContextBuilderTest {

    private ContextBuilder builder;

    @BeforeEach
    void setUp() {
        var config = new GitHubConfig();
        config.setToken("test-token");
        var apiService = new GitHubApiService(config, null, null);
        var tokenBudget = new TokenBudgetManager();
        builder = new ContextBuilder(apiService, tokenBudget);
    }

    @Test
    void buildPRInfo() {
        PRMetadata meta = new PRMetadata();
        meta.setTitle("Fix login bug");
        meta.setDescription("Fix the login issue #42");
        meta.setHeadBranch("feature/login");
        meta.setBaseBranch("main");
        meta.setFileCount(3);
        meta.setAdditions(50);
        meta.setDeletions(10);

        String result = builder.buildPRInfo(meta);

        assertThat(result).contains("Fix login bug");
        assertThat(result).contains("Fix the login issue #42");
        assertThat(result).contains("feature/login → main");
        assertThat(result).contains("3");
        assertThat(result).contains("+50");
        assertThat(result).contains("-10");
    }

    @Test
    void buildFileTreeSection() {
        String tree = "src/\n  Main.java\n  Utils.java\n\n";

        String result = builder.buildFileTreeSection(tree);

        assertThat(result).contains("## 项目结构");
        assertThat(result).contains("Main.java");
        assertThat(result).contains("Utils.java");
    }

    @Test
    void buildFileTreeSectionNull() {
        String result = builder.buildFileTreeSection(null);
        assertThat(result).contains("目录树不可用");
    }

    @Test
    void buildChangedFilesSection() {
        FileContent file = new FileContent("src/Foo.java", "public class Foo {}", "@@ -1 +1 @@\n+new line");

        String result = builder.buildChangedFilesSection(List.of(file));

        assertThat(result).contains("### 文件: src/Foo.java");
        assertThat(result).contains("```diff");
        assertThat(result).contains("public class Foo {}");
    }

    @Test
    void buildChangedFilesSectionExcludesFlaggedFiles() {
        FileContent excluded = new FileContent("image.png", "binary", "");
        excluded.setExcluded(true);

        String result = builder.buildChangedFilesSection(List.of(excluded));

        assertThat(result).doesNotContain("image.png");
    }

    @Test
    void buildVariables() {
        PRMetadata meta = new PRMetadata();
        meta.setTitle("Test");
        meta.setDescription("");
        meta.setHeadBranch("main");
        meta.setBaseBranch("main");

        Map<String, String> vars = builder.buildVariables(meta, "tree", List.of());

        assertThat(vars).containsKeys("pr_info", "pr_discussion", "file_tree", "changed_files", "related_files");
    }

    @Test
    void buildVariablesWithDiscussionAndRelatedFiles() {
        PRMetadata meta = new PRMetadata();
        meta.setTitle("Test");
        meta.setDescription("");
        meta.setHeadBranch("main");
        meta.setBaseBranch("main");

        Map<String, String> vars = builder.buildVariables(meta, "tree", List.of(),
                "## Discussion\n@user: looks good", "### 依赖: Foo.java\n```java\npublic class Foo {}\n```");

        assertThat(vars.get("pr_discussion")).contains("@user: looks good");
        assertThat(vars.get("related_files")).contains("Foo.java");
    }

    @Test
    void buildDiscussionSectionWithContent() {
        String result = builder.buildDiscussionSection("@alice: LGTM\n@bob: needs tests");
        assertThat(result).contains("@alice: LGTM");
        assertThat(result).contains("@bob: needs tests");
    }

    @Test
    void buildDiscussionSectionNull() {
        String result = builder.buildDiscussionSection(null);
        assertThat(result).isEmpty();
    }

    @Test
    void buildDiscussionSectionEmpty() {
        String result = builder.buildDiscussionSection("");
        assertThat(result).isEmpty();
    }

    @Test
    void extractImportsJava() {
        FileContent file = new FileContent("src/Foo.java",
                "import java.util.List;\nimport com.example.Utils;\n\npublic class Foo {}\n", "");

        List<String> imports = builder.extractImports(file);
        assertThat(imports).contains("java.util.List", "com.example.Utils");
    }

    @Test
    void extractImportsTypeScript() {
        FileContent file = new FileContent("src/bar.ts",
                "import React from 'react';\nimport { useRef } from 'react';\nimport { Utils } from './utils';\nconst _ = require('lodash');\n", "");

        List<String> imports = builder.extractImports(file);
        assertThat(imports).contains("react", "react", "./utils", "lodash");
    }

    @Test
    void extractImportsPython() {
        FileContent file = new FileContent("src/main.py",
                "import os\nfrom collections import defaultdict\nfrom .helpers import parse\n\n", "");

        List<String> imports = builder.extractImports(file);
        assertThat(imports).contains("os", "collections", ".helpers");
    }

    @Test
    void extractImportsNullContent() {
        FileContent file = new FileContent("src/empty.java", null, "");
        List<String> imports = builder.extractImports(file);
        assertThat(imports).isEmpty();
    }

    @Test
    void resolveLocalImportsFiltersExternalLibraries() {
        List<String> imports = List.of("java.util.List", "com.example.Utils", "react", "lodash", "@types/node", "os", "sys");

        List<String> resolved = builder.resolveLocalImports(imports, "src/com/example/Utils.java");

        assertThat(resolved).doesNotContain("java.util.List", "react", "lodash", "@types/node", "os", "sys");
        assertThat(resolved).contains("com/example/Utils.java");
    }

    @Test
    void resolveLocalImportsOnlyIfInFileTree() {
        List<String> imports = List.of("com.example.Foo", "com.example.Bar");

        List<String> resolved = builder.resolveLocalImports(imports, "src/com/example/Foo.java\nsrc/other/Baz.java");

        assertThat(resolved).contains("com/example/Foo.java");
        assertThat(resolved).doesNotContain("com/example/Bar.java");
    }

    @Test
    void resolveLocalImportsRelativePaths() {
        List<String> imports = List.of("./utils", "../common/helpers");

        List<String> resolved = builder.resolveLocalImports(imports, "src/utils.ts\nsrc/common/helpers.ts");

        assertThat(resolved).contains("utils", "common/helpers");
    }

    @Test
    void resolveLocalImportsReturnsDeduplicated() {
        List<String> imports = List.of("com.example.Utils", "com.example.Utils");

        List<String> resolved = builder.resolveLocalImports(imports, "src/com/example/Utils.java");

        assertThat(resolved).hasSize(1);
    }

    @Test
    void truncationInChangedFiles() {
        String longContent = "Line " + "x".repeat(14900) + "\n";
        FileContent file = new FileContent("src/Big.java", longContent, "");

        String result = builder.buildChangedFilesSection(List.of(file));

        assertThat(result).contains("[截断");
        assertThat(result.length()).isLessThan(longContent.length());
    }

    @Test
    void detectLanguageCommonFiles() {
        assertThat(ContextBuilder.detectLanguage("src/Main.java")).isEqualTo("java");
        assertThat(ContextBuilder.detectLanguage("components/App.tsx")).isEqualTo("typescript");
        assertThat(ContextBuilder.detectLanguage("utils/index.js")).isEqualTo("javascript");
        assertThat(ContextBuilder.detectLanguage("scripts/run.py")).isEqualTo("python");
        assertThat(ContextBuilder.detectLanguage("main.go")).isEqualTo("go");
        assertThat(ContextBuilder.detectLanguage("Dockerfile")).isEmpty();
    }

    @Test
    void buildVariablesIncludesAllFiveKeys() {
        PRMetadata meta = new PRMetadata();
        meta.setTitle("T");
        meta.setDescription("");
        meta.setHeadBranch("a");
        meta.setBaseBranch("b");

        Map<String, String> vars = builder.buildVariables(meta, "", List.of(), "disc", "rel");

        assertThat(vars).containsExactly(
                Map.entry("pr_info", vars.get("pr_info")),
                Map.entry("pr_discussion", "disc"),
                Map.entry("file_tree", vars.get("file_tree")),
                Map.entry("changed_files", vars.get("changed_files")),
                Map.entry("related_files", "rel")
        );
    }
}
