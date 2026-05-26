package com.carp.ai.langchain4j.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.carp.ai.langchain4j.entity.UserInterviewProfile;

/**
 * 用户面试画像 Service 接口
 */
public interface UserInterviewProfileService extends IService<UserInterviewProfile> {
    
    /**
     * 根据用户ID获取画像，如果不存在则初始化一个
     */
    UserInterviewProfile getByUserId(Long userId);
}
