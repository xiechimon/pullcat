package com.pullcat.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class User {

    private String id;
    private String githubLogin;
    private Long githubId;
    private String avatarUrl;
    private String email;
    private Instant createdAt = Instant.now();
}
