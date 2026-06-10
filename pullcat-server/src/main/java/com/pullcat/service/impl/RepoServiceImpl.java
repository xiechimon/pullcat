package com.pullcat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pullcat.common.convention.exception.ClientException;
import com.pullcat.common.constant.RedisKeys;
import com.pullcat.common.enums.CommonErrorCodeEnum;
import com.pullcat.dao.entity.RepoDO;
import com.pullcat.dao.mapper.RepoMapper;
import com.pullcat.dto.req.CreateRepoReqDTO;
import com.pullcat.dto.resp.DeletedRespDTO;
import com.pullcat.dto.resp.RepoRespDTO;
import com.pullcat.service.RepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RepoServiceImpl extends ServiceImpl<RepoMapper, RepoDO> implements RepoService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<RepoRespDTO> listRepos() {
        return baseMapper.selectList(null).stream().map(this::toRespDTO).toList();
    }

    @Override
    public RepoRespDTO addRepo(CreateRepoReqDTO req) {
        String owner = req.getOwner();
        String repo = req.getRepo();
        if (owner == null || repo == null) {
            throw new ClientException(CommonErrorCodeEnum.CLIENT_ERROR.code(), "owner 和 repo 不能为空");
        }

        RepoDO repoDO = new RepoDO(owner, repo);
        if (req.getDescription() != null) {
            repoDO.setDescription(req.getDescription());
        }
        baseMapper.insert(repoDO);
        redisTemplate.opsForValue().set(RedisKeys.repoKey(repoDO.getFullName()), repoDO);
        return toRespDTO(repoDO);
    }

    @Override
    public DeletedRespDTO removeRepo(String owner, String repo) {
        String fullName = owner + "/" + repo;
        if (baseMapper.selectById(fullName) == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "仓库不存在");
        }

        baseMapper.deleteById(fullName);
        redisTemplate.delete(RedisKeys.repoKey(fullName));
        return new DeletedRespDTO(true);
    }

    @Override
    public RepoRespDTO getRepo(String owner, String repo) {
        String fullName = owner + "/" + repo;
        Object cached = redisTemplate.opsForValue().get(RedisKeys.repoKey(fullName));
        RepoDO repoDO = cached instanceof RepoDO ? (RepoDO) cached : baseMapper.selectOne(
                new LambdaQueryWrapper<RepoDO>().eq(RepoDO::getFullName, fullName));
        if (repoDO == null) {
            throw new ClientException(CommonErrorCodeEnum.NOT_FOUND.code(), "仓库不存在");
        }
        redisTemplate.opsForValue().set(RedisKeys.repoKey(fullName), repoDO);

        return toRespDTO(repoDO);
    }

    private RepoRespDTO toRespDTO(RepoDO repoDO) {
        RepoRespDTO resp = new RepoRespDTO();
        resp.setOwner(repoDO.getOwner());
        resp.setRepo(repoDO.getRepo());
        resp.setFullName(repoDO.getFullName());
        resp.setDescription(repoDO.getDescription());
        resp.setStars(repoDO.getStars());
        resp.setLanguage(repoDO.getLanguage());
        resp.setAddedAt(repoDO.getAddedAt() == null ? null : repoDO.getAddedAt().toString());
        return resp;
    }
}
