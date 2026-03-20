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

package uk.gov.hmrc.vapingdutyaccount.models.vpdSummaryAPI

import play.api.libs.json.{JsObject, Json, Writes}

/** The VPD Summary API's successful response object. This is serialised to Json automatically by its accompanying implicit formats.
  *
  * @param service
  *   An instance of the service info case class
  * @param identifiers
  *   An instance of an identifier case class
  * @param contactPreference
  *   An instance of the desired ContactMethod (ContactMethod.resolve should be used to resolve the boolean received from Etmp to its enum
  *   value)
  * @param links
  *   An instance of this response's links case class
  *
  * @return
  *   `JsValue`
  *
  * @example
  *   {{{
  *  {
  *    "service" : {
  *      "name" : "Vaping Products Duty",
  *      "id" : "VPD"
  *    },
  *    "identifiers" : {
  *      "vpdId" : "TEST"
  *    },
  *    "contactPreference" : "POST",
  *    "links" : {
  *      "self" : {
  *        "href" : "url",
  *        "method" : "GET"
  *      },
  *      "manageContactPreference" : {
  * .      "href" : "url"
  *        "method" : "GET"
  *      }
  *    }
  *  }
  *   }}}
  */
case class VPDSummary(
    service: ServiceInfo,
    identifiers: Identifier,
    contactPreference: ContactMethod,
    links: Links
)

object VPDSummary {
  given Writes[VPDSummary] = Json.writes[VPDSummary]
}
