package com.thoughtworks.verify.pact

import play.api.libs.json.JsValue

/**
  * Created by xfwu on 12/07/2017.
  */
case class Interaction(description: String,
                       request: PactRequest,
                       response: PactResponse,
                       matchingRules: Option[JsValue])


