// Copyright 2014, Leanplum, Inc.

package com.leanplum;

import java.util.Map;

class RequestFactory {
  static RequestFactory defaultFactory;
 
  public synchronized static RequestFactory getInstance() {
    if (defaultFactory == null) {
      defaultFactory = new RequestFactory();
    }
    return defaultFactory;
  }
  
  public LeanplumRequest createRequest(
      String httpMethod, String apiMethod, Map<String, Object> params) {
    return new LeanplumRequest(httpMethod, apiMethod, params);
  }
}
