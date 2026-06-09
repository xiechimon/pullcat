package com.pullcat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullcat.common.biz.user.CurrentLoginArgumentResolver;
import com.pullcat.config.WebMvcConfig;
import com.pullcat.dto.resp.CreateReviewRespDTO;
import com.pullcat.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, CurrentLoginArgumentResolver.class})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @Test
    void createReviewShouldPassResolvedLogin() throws Exception {
        OAuth2AuthenticationToken authentication = oauthAuthentication("xmon123");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            mockMvc.perform(post("/api/pullcat/v1/reviews")
                            .with(csrf())
                            .with(authentication(authentication))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "prUrl", "https://github.com/test/repo/pull/1"
                            ))))
                    .andExpect(status().isOk());

            verify(reviewService).createReview("https://github.com/test/repo/pull/1", "xmon123");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void createReviewShouldAllowAnonymousLogin() throws Exception {
        mockMvc.perform(post("/api/pullcat/v1/reviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "prUrl", "https://github.com/test/repo/pull/2"
                        ))))
                .andExpect(status().isOk());

        verify(reviewService).createReview("https://github.com/test/repo/pull/2", null);
    }

    private OAuth2AuthenticationToken oauthAuthentication(String login) {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new OAuth2UserAuthority(Map.of("login", login))),
                Map.of("login", login),
                "login"
        );
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "github");
    }
}
