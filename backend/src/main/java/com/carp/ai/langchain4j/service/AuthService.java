package com.carp.ai.langchain4j.service;

import com.carp.ai.langchain4j.bean.AuthRequest;
import com.carp.ai.langchain4j.bean.AuthUserView;

public interface AuthService {
    AuthUserView getDefaultUser();

    AuthUserView register(AuthRequest request);

    AuthUserView login(AuthRequest request);
}
