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

import play.api.libs.json.{JsObject, Json, Writes}
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig

/**
 * Generates the API response
 *
 * @param config Used to populate the "service" field of the APIResponse (see example below)
 * @param code   Status code to be returned with the request
 * @param vpdId  VpdId that executed the request
 * @param contactMethod The contact method this VpdId uses
 * @param links The links that will be sent in the 'links' field of the API response
 * @param identifier the identifier for this request
 * @return `JsValue`
 * @note Currently, the `identifiers` field is populated automatically as the information required
 * for thta field is already supplied in the case class.
 * @example
 * {{{
 * {
 *   "service" : {
 *     "name" : "Vaping Products Duty",
 *     "id" : "VPD"
 *   },
 *   "identifiers" : {
 *     "vpdId" : "TEST"
 *   },
 *   "contactPreference" : "POST",
 *   "links" : {
 *     "key" : {
 *       "href" : "url",
 *       "method" : "GET"
 *     }
 *   }
 * }
 * }}}
 */
case class VPDSummary (
  config: VPDSummaryConfig,
  code: Int,
  vpdId: String,
  contactMethod: ContactMethod,
  links: Seq[Link],
  // identifier: Identifier,
)

implicit val VPDSummaryWrites: Writes[VPDSummary] = Writes { summary => 
  Json.obj(
    "service" -> Json.toJson(summary.config),
    "identifiers" -> Json.toJson(Identifier(vpdId = summary.vpdId)),
    "contactPreference" -> Json.toJson(summary.contactMethod),
    "links" -> Json.obj(
      summary.links map { link =>
        link.key -> Json.toJson(link)
      }: _*
    ),
  )
}
