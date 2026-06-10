package com.pullcat.controller;

import com.pullcat.service.analysis.GitHubInstallationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GitHubAppController.class)
@AutoConfigureMockMvc(addFilters = false)
class GitHubAppControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitHubInstallationService gitHubInstallationService;

    @Test
    void setup_install_savesInstallationAndRedirectsWithInstalledFlag() throws Exception {
        mockMvc.perform(get("/api/pullcat/v1/github/app/setup")
                        .param("installation_id", "42")
                        .param("setup_action", "install")
                        .param("account_login", "xiechimon")
                        .param("account_type", "User"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/dashboard?installed=true"));

        verify(gitHubInstallationService).saveInstallation(42L, "xiechimon", "User");
    }

    @Test
    void setup_suspend_marksInstallationSuspendedAndRedirectsDashboard() throws Exception {
        mockMvc.perform(get("/api/pullcat/v1/github/app/setup")
                        .param("installation_id", "88")
                        .param("setup_action", "suspend"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/dashboard"));

        verify(gitHubInstallationService).suspendInstallation(88L);
    }
}
