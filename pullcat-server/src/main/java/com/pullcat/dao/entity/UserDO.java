package com.pullcat.dao.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class UserDO {

    private String id;
    private String githubLogin;
    private Long githubId;
    private String avatarUrl;
    private String email;
    private Instant createdAt = Instant.now();
}
