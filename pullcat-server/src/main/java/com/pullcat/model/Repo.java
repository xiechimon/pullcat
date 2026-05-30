package com.pullcat.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class Repo {

    private String owner;
    private String repo;
    private String fullName;
    private String description;
    private Integer stars;
    private String language;
    private Instant addedAt = Instant.now();

    public Repo(String owner, String repo) {
        this.owner = owner;
        this.repo = repo;
        this.fullName = owner + "/" + repo;
    }
}
