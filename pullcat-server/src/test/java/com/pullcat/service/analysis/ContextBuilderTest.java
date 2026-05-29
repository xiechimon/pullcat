package com.pullcat.service.analysis;

import com.pullcat.model.FileContent;
import com.pullcat.model.PRMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContextBuilderTest {

    private final ContextBuilder builder = new ContextBuilder();

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

        assertThat(vars).containsKeys("pr_info", "file_tree", "changed_files");
    }
}
