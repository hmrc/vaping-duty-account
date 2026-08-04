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

package uk.gov.hmrc.vapingdutyaccount.models.vpdSummary

import play.api.http.Status.*
import play.api.libs.json.{Json, Writes}

final case class APIError(message: String, code: Int)

/** Types extracted from OpenAPI specification
  *
  * @example
  *   {{{
  * Json.toJson(APIErrors.BadRequest) would serialise to:
  *
  * {
  *   "message" : "Bad request (invalid headers or malformed request)",
  *   "code" : 400
  * }
  *   }}}
  */
object APIErrors {
  val BadRequest = APIError("Bad request (invalid headers or malformed request)", BAD_REQUEST)

  val Unauthorised = APIError("Unauthorised (missing or invalid bearer token)", UNAUTHORIZED)

  val ValidTokenNotAllowedForRequestedVpdId = APIError("Forbidden (token valid but not authorised for this VPD ID)", FORBIDDEN)

  val InternalServerError = APIError("Internal server error", INTERNAL_SERVER_ERROR)

  val ServiceUnavailable = APIError("Service unavailable (feature is switched OFF)", SERVICE_UNAVAILABLE)

  val NotAcceptable = APIError("The requested API version is not supported", NOT_ACCEPTABLE)
}

implicit val APIErrorFormat: Writes[APIError] = Json.writes[APIError]
