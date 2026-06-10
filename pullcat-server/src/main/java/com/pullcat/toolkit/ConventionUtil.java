package com.pullcat.toolkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 仓库约定文件检测工具
 */
public final class ConventionUtil {

    private ConventionUtil() {
    }

    private static final Set<String> EXCLUDE = Set.of(
            "readme.md", "changelog.md", "license.md", "changes.md",
            "history.md", "release.md", "releases.md", "security.md"
    );

    private static final List<String> PRIORITY = List.of(
            "AGENTS.md", "CLAUDE.md", "OPENCODE.md", "GEMINI.md",
            ".cursorrules", "CONTRIBUTING.md", "CONVENTIONS.md",
            "DEVELOPMENT.md", ".pullcat.md"
    );

    /**
     * 从文件树字符串中检测根目录约定文件候选列表，按优先级排序，最多返回 3 个
     */
    public static List<String> detectConventionCandidates(String fileTree) {
        if (fileTree == null || fileTree.isBlank()) return List.of();

        List<String> rootFiles = new ArrayList<>();
        boolean inRoot = false;
        for (String line : fileTree.split("\n")) {
            if ("./".equals(line.trim())) {
                inRoot = true;
                continue;
            }
            if (inRoot) {
                if (line.isBlank() || !line.startsWith("  ")) break;
                String name = line.strip();
                if (name.toLowerCase().endsWith(".md") || PRIORITY.contains(name)) {
                    rootFiles.add(name);
                }
            }
        }

        rootFiles.removeIf(f -> EXCLUDE.contains(f.toLowerCase()));

        Map<String, Integer> priorityIdx = new HashMap<>();
        for (int i = 0; i < PRIORITY.size(); i++) {
            priorityIdx.put(PRIORITY.get(i), i);
        }
        rootFiles.sort(Comparator
                .comparingInt((String f) -> priorityIdx.getOrDefault(f, Integer.MAX_VALUE))
                .thenComparing(Comparator.naturalOrder()));

        return rootFiles.stream().limit(3).toList();
    }
}
