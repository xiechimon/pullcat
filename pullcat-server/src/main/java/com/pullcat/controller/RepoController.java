package com.pullcat.controller;

import com.pullcat.model.Repo;
import com.pullcat.service.analysis.RepoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repos")
public class RepoController {

    private final RepoRepository repoRepository;

    public RepoController(RepoRepository repoRepository) {
        this.repoRepository = repoRepository;
    }

    @GetMapping
    public ResponseEntity<List<Repo>> listRepos() {
        return ResponseEntity.ok(repoRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Repo> addRepo(@RequestBody Map<String, String> body) {
        String owner = body.get("owner");
        String repo = body.get("repo");
        if (owner == null || repo == null) {
            return ResponseEntity.badRequest().build();
        }

        Repo r = new Repo(owner, repo);
        if (body.containsKey("description")) r.setDescription(body.get("description"));
        repoRepository.save(r);
        return ResponseEntity.ok(r);
    }

    @DeleteMapping("/{owner}/{repo}")
    public ResponseEntity<Map<String, Object>> removeRepo(@PathVariable String owner, @PathVariable String repo) {
        if (!repoRepository.exists(owner, repo)) {
            return ResponseEntity.notFound().build();
        }
        repoRepository.delete(owner, repo);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/{owner}/{repo}")
    public ResponseEntity<Repo> getRepo(@PathVariable String owner, @PathVariable String repo) {
        Repo r = repoRepository.findById(owner + "/" + repo);
        if (r == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(r);
    }
}
