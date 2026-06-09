package com.pullcat.service.analysis;

import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.dto.resp.PRMetadataRespDTO;
import com.pullcat.remote.GitHubApiService;

import java.util.List;
import java.util.Map;

public interface ContextBuilder {

    String buildPRInfo(PRMetadataRespDTO meta);

    String buildDiscussionSection(String discussion);

    String buildFileTreeSection(String fileTree);

    String buildChangedFilesSection(List<FileContentRespDTO> files);

    Map<String, String> buildVariables(PRMetadataRespDTO meta, String fileTree, List<FileContentRespDTO> files);

    Map<String, String> buildVariables(PRMetadataRespDTO meta, String fileTree, List<FileContentRespDTO> files,
                                       String discussion, String relatedFiles);

    List<String> extractImports(FileContentRespDTO file);

    List<String> resolveLocalImports(List<String> imports, String fileTree);

    String buildRelatedFilesSection(GitHubApiService.PRUrl prUrl, List<String> resolvedPaths);
}
