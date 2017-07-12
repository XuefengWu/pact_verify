package com.thoughtworks.verify.pact

import play.api.libs.json.JsValue

/**
  * Created by xfwu on 12/07/2017.
  */
case class PactResponse(status: Int, body: Option[JsValue])
