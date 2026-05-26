package com.carp.ai.langchain4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carp.ai.langchain4j.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}
