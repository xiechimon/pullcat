package com.pullcat.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pullcat.dao.entity.GitHubInstallationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * GitHub App 安装记录 Mapper
 */
@Mapper
public interface GitHubInstallationMapper extends BaseMapper<GitHubInstallationDO> {
}
