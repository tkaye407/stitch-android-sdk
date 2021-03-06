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

package com.mongodb.stitch.core.services.internal;

/**
 * An ifc that allows any service of any type
 * to bind to it's associated {@link CoreStitchServiceClient}.
 *
 * {@link CoreStitchServiceClient#bind(StitchServiceBinder)}
 */
public interface StitchServiceBinder {
  /**
   * Notify the binder that a rebind event has occured.
   * E.g., a change in authentication.
   *
   * @param rebindEvent the rebind event that occurred
   */
  void onRebindEvent(final RebindEvent rebindEvent);
}
