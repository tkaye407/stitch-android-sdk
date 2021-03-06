/*
 * Copyright 2018-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.stitch.core.auth.providers.userpassword;

import static com.mongodb.stitch.core.auth.providers.userpassword.UserPasswordAuthProvider.DEFAULT_NAME;
import static com.mongodb.stitch.core.auth.providers.userpassword.UserPasswordAuthProvider.TYPE;

import com.mongodb.stitch.core.auth.ProviderCapabilities;
import com.mongodb.stitch.core.auth.StitchCredential;
import org.bson.Document;

/**
 * The credential used for User/Password log ins.
 */
public final class UserPasswordCredential implements StitchCredential {

  private final String providerName;
  private final String username;
  private final String password;

  /**
   * Constructs a user password credential for a user.
   *
   * @param username the username of the user.
   * @param password the password of the user.
   */
  public UserPasswordCredential(
      final String username, final String password) {
    this(DEFAULT_NAME, username, password);
  }

  /**
   * Constructs a user password credential for a user.
   *
   * @param providerName the authentication provider name.
   * @param username the username of the user.
   * @param password the password of the user.
   */
  UserPasswordCredential(
      final String providerName, final String username, final String password) {
    this.providerName = providerName;
    this.username = username;
    this.password = password;
  }

  @Override
  public String getProviderName() {
    return providerName;
  }

  @Override
  public String getProviderType() {
    return TYPE;
  }

  @Override
  public Document getMaterial() {
    return new Document(Fields.USERNAME, username).append(Fields.PASSWORD, password);
  }

  @Override
  public ProviderCapabilities getProviderCapabilities() {
    return new ProviderCapabilities(false);
  }

  private static class Fields {
    static final String USERNAME = "username";
    static final String PASSWORD = "password";
  }
}
