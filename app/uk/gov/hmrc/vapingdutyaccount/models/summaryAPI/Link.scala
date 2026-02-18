/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vapingdutyaccount.models.summaryAPI

import play.api.libs.json.{Json, JsString, Writes}

/**
 * A link object
 * @param key The key in which this link object will be entered into the links field of the `APIResponse`
 * @param href The href for this link
 * @param method The request method expected to be used for this link
 *
 * @example
 * {{{
 *  // ...
 *   "links" : {
 *     "key" : { // <-- instance of link case class
 *       "href" : "url",
 *       "method" : "GET"
 *     }
 *   }
 * }}}
 */
final case class Link(
                       key: String,
                       href: String,
                       method: String
                     )

/**
 * The implicit writes for the `Link`, these will ignore the "key"
 * field on the case class constructor and prevent it from being
 * added to the resultant JSON payload.
 */
implicit val LinkWrites: Writes[Link] = Writes { link =>
  Json.obj(
    "href" -> JsString(link.href),
    "method" -> JsString(link.method)
  )
}
