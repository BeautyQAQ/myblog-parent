package com.myblog.sso.service;

import com.myblog.common.utils.Result;

/**
 * 根据token查询用户信息
 * <p>Title: TokenService</p>
 * <p>Description: </p>
 * <p>Company: </p>
 * @version 1.0
 */

public interface TokenService {
    Result getUserByToken(String token);
}
