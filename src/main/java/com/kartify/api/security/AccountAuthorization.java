package com.kartify.api.security;

import org.springframework.stereotype.Component;

@Component("accountAuthorization")
public class AccountAuthorization {

  // Check if email is verified
  public boolean isEmailVerified(CustomUserDetails authentication){

    return authentication.isEmailVerified();

  }

}
