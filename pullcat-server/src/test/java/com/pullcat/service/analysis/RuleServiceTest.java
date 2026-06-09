package com.pullcat.service.analysis;

import com.pullcat.dao.mapper.RuleMapper;
import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.enums.RuleType;
import com.pullcat.common.enums.Severity;
import com.pullcat.dao.entity.RuleDO;
import com.pullcat.dto.req.RuleUpsertReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RuleRespDTO;
import com.pullcat.service.impl.RuleServiceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock
    RuleMapper ruleMapper;

    @Mock
    RuleSuggestionService ruleSuggestionService;

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ValueOperations<String, Object> valueOperations;

    @InjectMocks
    RuleServiceImpl ruleService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ---- list ----

    @Test
    void list_returnsMappedDTOs() {
        RuleDO rule = rule("r1");
        when(valueOperations.get(anyString())).thenReturn(null);
        when(ruleMapper.selectList(any())).thenReturn(List.of(rule));
        List<RuleRespDTO> result = ruleService.list("owner", "repo");
        assertEquals(1, result.size());
        assertEquals("r1", result.get(0).getId());
    }

    // ---- getSuggestions ----

    @Test
    void getSuggestions_delegatesToSuggestionService() {
        RuleDO rule = rule("r2");
        when(ruleSuggestionService.getSuggestions("owner", "repo")).thenReturn(List.of(rule));
        List<RuleRespDTO> result = ruleService.getSuggestions("owner", "repo");
        assertEquals(1, result.size());
        assertEquals("r2", result.get(0).getId());
    }

    // ---- create ----

    @Test
    void create_assignsIdAndSaves() {
        RuleUpsertReqDTO req = upsertReq();
        RuleRespDTO result = ruleService.create("owner", "repo", req);
        assertNotNull(result.getId());
        verify(ruleMapper).insert(any(RuleDO.class));
    }

    @Test
    void create_setsRepoOwnerAndName() {
        RuleUpsertReqDTO req = upsertReq();
        ruleService.create("owner", "repo", req);
        verify(ruleMapper).insert(argThat((RuleDO r) -> "owner".equals(r.getRepoOwner()) && "repo".equals(r.getRepoName())));
    }

    // ---- update ----

    @Test
    void update_usesProvidedRuleId() {
        RuleUpsertReqDTO req = upsertReq();
        RuleRespDTO result = ruleService.update("owner", "repo", "rid1", req);
        verify(ruleMapper).updateById(argThat((RuleDO r) -> "rid1".equals(r.getId())));
        assertEquals("rid1", result.getId());
    }

    // ---- delete ----

    @Test
    void delete_delegatesToRepository() {
        DeletedRespDTO result = ruleService.delete("owner", "repo", "rid1");
        verify(ruleMapper).deleteById("rid1");
        assertTrue(result.isDeleted());
    }

    // ---- toggle ----

    @Test
    void toggle_notFound_throwsClientException() {
        when(ruleMapper.selectOne(any())).thenReturn(null);
        assertThrows(ClientException.class, () -> ruleService.toggle("owner", "repo", "rid1"));
    }

    @Test
    void toggle_enabled_disablesRule() {
        RuleDO rule = rule("rid1");
        rule.setEnabled(true);
        when(ruleMapper.selectOne(any())).thenReturn(rule);
        RuleRespDTO result = ruleService.toggle("owner", "repo", "rid1");
        assertFalse(result.isEnabled());
        verify(ruleMapper).updateById(rule);
    }

    @Test
    void toggle_disabled_enablesRule() {
        RuleDO rule = rule("rid1");
        rule.setEnabled(false);
        when(ruleMapper.selectOne(any())).thenReturn(rule);
        RuleRespDTO result = ruleService.toggle("owner", "repo", "rid1");
        assertTrue(result.isEnabled());
    }

    // ---- helpers ----

    private RuleDO rule(String id) {
        RuleDO r = new RuleDO();
        r.setId(id);
        r.setName("test-rule");
        r.setType(RuleType.CODE_PATTERN);
        r.setPattern("TODO");
        r.setSeverity(Severity.MEDIUM);
        r.setEnabled(true);
        return r;
    }

    private RuleUpsertReqDTO upsertReq() {
        RuleUpsertReqDTO req = new RuleUpsertReqDTO();
        req.setName("test");
        req.setType(RuleType.CODE_PATTERN);
        req.setPattern("TODO");
        req.setSeverity(Severity.MEDIUM);
        req.setEnabled(true);
        return req;
    }
}
