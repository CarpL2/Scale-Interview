package com.carp.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carp.ai.langchain4j.entity.UserInterviewProfile;
import com.carp.ai.langchain4j.mapper.UserInterviewProfileMapper;
import com.carp.ai.langchain4j.service.UserInterviewProfileService;
import org.springframework.stereotype.Service;

/**
 * 用户面试画像 Service 实现类
 */
@Service
public class UserInterviewProfileServiceImpl extends ServiceImpl<UserInterviewProfileMapper, UserInterviewProfile> implements UserInterviewProfileService {

    @Override
    public UserInterviewProfile getByUserId(Long userId) {
        LambdaQueryWrapper<UserInterviewProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInterviewProfile::getUserId, userId);
        UserInterviewProfile profile = this.getOne(queryWrapper);
        
        if (profile == null) {
            // 如果不存在，初始化一个空的
            profile = new UserInterviewProfile();
            profile.setUserId(userId);
            this.save(profile);
        }
        return profile;
    }
}
