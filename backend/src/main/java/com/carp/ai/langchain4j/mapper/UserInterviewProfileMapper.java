package com.carp.ai.langchain4j.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carp.ai.langchain4j.entity.UserInterviewProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户面试画像 Mapper 接口
 */
@Mapper
public interface UserInterviewProfileMapper extends BaseMapper<UserInterviewProfile> {
}
