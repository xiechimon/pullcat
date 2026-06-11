package com.pullcat.controller;

import com.pullcat.service.analysis.GitHubInstallationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub App 安装回调控制层
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pullcat/v1/github/app")
public class GitHubAppController {

    private final GitHubInstallationService gitHubInstallationService;

    /**
     * 处理 GitHub App setup 回调
     */
    @GetMapping("/setup")
    public ResponseEntity<Void> setup(@RequestParam("installation_id") Long installationId,
                                      @RequestParam("setup_action") String setupAction,
                                      @RequestParam(value = "account_login", required = false) String accountLogin,
                                      @RequestParam(value = "account_type", required = false) String accountType) {
        if ("install".equals(setupAction) || "request".equals(setupAction) || "update".equals(setupAction)
                || "unsuspend".equals(setupAction)) {
            gitHubInstallationService.saveInstallation(installationId, accountLogin, accountType);
            return redirect("/?installed=true");
        }
        if ("delete".equals(setupAction) || "suspend".equals(setupAction)) {
            gitHubInstallationService.suspendInstallation(installationId);
        }
        return redirect("/");
    }

    private ResponseEntity<Void> redirect(String location) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, location);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
