package com.pullcat.remote;

import com.pullcat.dto.resp.FileContentRespDTO;
import com.pullcat.dto.resp.PRDataRespDTO;
import com.pullcat.dto.resp.PRMetadataRespDTO;
import com.pullcat.remote.dto.resp.GitHubFileRespDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * GitHub 远程调用服务接口
 */
public interface GitHubApiService {

    PRUrl parsePrUrl(String url);

    Mono<PRMetadataRespDTO> fetchPRMetadata(PRUrl prUrl);

    Mono<String> fetchPRComments(PRUrl prUrl);

    Mono<String> fetchFileContent(PRUrl prUrl, String path);

    Mono<Void> updateCommitStatus(PRUrl prUrl, String sha, String state, String description, String targetUrl);

    Mono<String> fetchHeadSha(PRUrl prUrl);

    Mono<PRDataRespDTO> fetchPRData(PRUrl prUrl);

    Mono<String> fetchDiff(PRUrl prUrl);

    Mono<List<GitHubFileRespDTO>> fetchChangedFiles(PRUrl prUrl);

    Flux<FileContentRespDTO> fetchFileContents(PRUrl prUrl, List<GitHubFileRespDTO> changedFiles);

    Mono<String> fetchFileTree(PRUrl prUrl);

    /**
     * 返回一个使用 Installation Token 的独立 API 服务实例
     */
    Mono<GitHubApiService> withInstallationToken(long installationId);

    Mono<Long> publishReviewWithComments(PRUrl prUrl, String summaryBody, List<ReviewComment> comments);

    Mono<Long> publishReview(PRUrl prUrl, String summaryBody);

    /**
     * 在 PR issue 评论区发布一条评论
     */
    Mono<Long> postIssueComment(PRUrl prUrl, String body);

    /**
     * 行级审查评论
     */
    record ReviewComment(String file, Integer line, String body) {
    }

    /**
     * 解析后的 GitHub PR URL 信息
     */
    record PRUrl(String owner, String repo, int number, String headRef, String headSha) {
        public PRUrl(String owner, String repo, int number) {
            this(owner, repo, number, null, null);
        }

        public PRUrl withHeadInfo(String headRef, String headSha) {
            return new PRUrl(owner, repo, number, headRef, headSha);
        }
    }
}
