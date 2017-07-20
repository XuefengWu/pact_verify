package com.thoughtworks.pact.verify.pact

import play.api.libs.json.JsValue

/**
  * Created by xfwu on 12/07/2017.
  */
case class PactRequest(method: String, path: String, contentType: Option[String],
                       body: Option[JsValue], cookies: Option[String], form: Option[String])
