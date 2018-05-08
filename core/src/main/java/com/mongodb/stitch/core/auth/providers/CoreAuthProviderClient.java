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

package com.mongodb.stitch.core.auth.providers;

import com.mongodb.stitch.core.auth.internal.StitchAuthRoutes;
import com.mongodb.stitch.core.internal.net.StitchRequestClient;

public abstract class CoreAuthProviderClient {

  private final String providerName;
  private final StitchRequestClient requestClient;
  private final StitchAuthRoutes authRoutes;

  protected CoreAuthProviderClient(final CoreAuthProviderClient coreClient) {
    this.providerName = coreClient.providerName;
    this.requestClient = coreClient.requestClient;
    this.authRoutes = coreClient.authRoutes;
  }

  protected CoreAuthProviderClient(
      final String providerName,
      final StitchRequestClient requestClient,
      final StitchAuthRoutes authRoutes) {
    this.providerName = providerName;
    this.requestClient = requestClient;
    this.authRoutes = authRoutes;
  }

  protected String getProviderName() {
    return providerName;
  }

  protected StitchRequestClient getRequestClient() {
    return requestClient;
  }

  protected StitchAuthRoutes getAuthRoutes() {
    return authRoutes;
  }
}