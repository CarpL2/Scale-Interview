package com.carp.ai.langchain4j.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carp.ai.langchain4j.bean.AuthRequest;
import com.carp.ai.langchain4j.bean.AuthUserView;
import com.carp.ai.langchain4j.entity.AppUser;
import com.carp.ai.langchain4j.entity.UserInterviewProfile;
import com.carp.ai.langchain4j.mapper.AppUserMapper;
import com.carp.ai.langchain4j.service.AuthService;
import com.carp.ai.langchain4j.service.UserInterviewProfileService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AuthServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AuthService {

    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "1234";

    @Resource
    private UserInterviewProfileService profileService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureDefaultUser() {
        ensureUserTable();
        AppUser user = getById(DEFAULT_USER_ID);
        if (user != null && DEFAULT_USERNAME.equals(user.getUsername())
                && hashPassword(DEFAULT_PASSWORD, user.getPasswordSalt()).equals(user.getPasswordHash())) {
            return;
        }

        AppUser defaultUser = user == null ? new AppUser() : user;
        defaultUser.setId(DEFAULT_USER_ID);
        defaultUser.setUsername(DEFAULT_USERNAME);
        String salt = newSalt();
        defaultUser.setPasswordSalt(salt);
        defaultUser.setPasswordHash(hashPassword(DEFAULT_PASSWORD, salt));
        if (defaultUser.getCreateTime() == null) {
            defaultUser.setCreateTime(LocalDateTime.now());
        }
        defaultUser.setUpdateTime(LocalDateTime.now());
        saveOrUpdate(defaultUser);

        UserInterviewProfile profile = profileService.getByUserId(DEFAULT_USER_ID);
        if (profile.getUserName() == null || profile.getUserName().isBlank()) {
            profile.setUserName(DEFAULT_USERNAME);
            profileService.updateById(profile);
        }
    }

    private void ensureUserTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS app_user (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录用户名',
                    password_hash VARCHAR(128) NOT NULL COMMENT '密码哈希',
                    password_salt VARCHAR(64) NOT NULL COMMENT '密码盐',
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轻量用户表'
                """);
    }

    @Override
    public AuthUserView getDefaultUser() {
        ensureDefaultUser();
        return toView(getById(DEFAULT_USER_ID));
    }

    @Transactional
    @Override
    public AuthUserView register(AuthRequest request) {
        String username = normalizeUsername(request);
        String password = normalizePassword(request);

        boolean exists = lambdaQuery().eq(AppUser::getUsername, username).exists();
        if (exists) {
            throw new IllegalArgumentException("用户名已存在");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        String salt = newSalt();
        user.setPasswordSalt(salt);
        user.setPasswordHash(hashPassword(password, salt));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        save(user);

        UserInterviewProfile profile = profileService.getByUserId(user.getId());
        profile.setUserName(username);
        profileService.updateById(profile);
        return toView(user);
    }

    @Override
    public AuthUserView login(AuthRequest request) {
        String username = normalizeUsername(request);
        String password = normalizePassword(request);

        AppUser user = lambdaQuery().eq(AppUser::getUsername, username).one();
        if (user == null || !hashPassword(password, user.getPasswordSalt()).equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return toView(user);
    }

    private String normalizeUsername(AuthRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        String username = request.getUsername().trim();
        if (username.length() > 50) {
            throw new IllegalArgumentException("用户名不能超过50个字符");
        }
        return username;
    }

    private String normalizePassword(AuthRequest request) {
        if (request == null || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (request.getPassword().length() < 4) {
            throw new IllegalArgumentException("密码至少4位");
        }
        return request.getPassword();
    }

    private AuthUserView toView(AppUser user) {
        AuthUserView view = new AuthUserView();
        view.setUserId(user.getId());
        view.setUsername(user.getUsername());
        return view;
    }

    private String newSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("密码摘要计算失败", e);
        }
    }
}
