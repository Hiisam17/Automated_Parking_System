package com.ktpm.parking.business.port;

import com.ktpm.parking.business.service.AuthenticationResult;

import java.util.List;

public interface AccessTokenIssuer {

    AuthenticationResult issue(String subject, List<String> roles);
}
