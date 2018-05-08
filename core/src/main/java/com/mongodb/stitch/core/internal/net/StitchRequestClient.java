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

package com.mongodb.stitch.core.internal.net;

import com.mongodb.stitch.core.StitchError;
import com.mongodb.stitch.core.StitchRequestErrorCode;
import com.mongodb.stitch.core.StitchRequestException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class StitchRequestClient {

  private final String baseUrl;
  private final Transport transport;

  public StitchRequestClient(final String baseUrl, final Transport transport) {
    this.baseUrl = baseUrl;
    this.transport = transport;
  }

  private static Response inspectResponse(final Response response) {
    if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
      return response;
    }

    StitchError.handleRequestError(response);
    return null;
  }

  /**
   * Performs a request against Stitch app servers. Throws a Stitch specific exception
   * if the request fails.
   * @param stitchReq The request to perform.
   * @return A {@link Response} to the request.
   */
  public Response doRequest(final StitchRequest stitchReq) {
    final Response response;
    try {
      response = transport.roundTrip(buildRequest(stitchReq));
    } catch (Exception e) {
      throw new StitchRequestException(e, StitchRequestErrorCode.TRANSPORT_ERROR);
    }

    return inspectResponse(response);
  }

  /**
   * Performs a JSON request against Stitch app servers. Throws a Stitch specific exception
   * if the request fails.
   * @param stitchReq The request to perform.
   * @return A {@link Response} to the request.
   */
  public Response doJsonRequestRaw(final StitchDocRequest stitchReq) {
    final StitchDocRequest.Builder newReqBuilder = stitchReq.builder();
    newReqBuilder.withBody(stitchReq.getDocument().toJson().getBytes(StandardCharsets.UTF_8));
    final Map<String, String> newHeaders = newReqBuilder.getHeaders(); // This is not a copy
    newHeaders.put(Headers.CONTENT_TYPE, ContentTypes.APPLICATION_JSON);
    newReqBuilder.withHeaders(newHeaders);

    return doRequest(newReqBuilder.build());
  }

  private Request buildRequest(final StitchRequest stitchReq) {
    return new Request.Builder()
        .withMethod(stitchReq.getMethod())
        .withUrl(String.format("%s%s", baseUrl, stitchReq.getPath()))
        .withHeaders(stitchReq.getHeaders())
        .withBody(stitchReq.getBody())
        .build();
  }
}