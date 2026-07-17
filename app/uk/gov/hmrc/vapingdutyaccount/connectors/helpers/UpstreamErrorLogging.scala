/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.vapingdutyaccount.connectors.helpers

import play.api.libs.json.Json
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.UpstreamErrorResponseBody

import scala.util.Try

trait UpstreamErrorLogging {

  // UpstreamErrorResponse doesn't expose the raw response body directly - HttpReadsInstances
  // wraps it inside `message` as `"... returned $status. Response body: '$responseBody'"`.
  private val responseBodyPattern = """(?s).*Response body: '(.*)'$""".r

  private def responseBodyOf(error: UpstreamErrorResponse): String =
    error.message match {
      case responseBodyPattern(body) => body
      case other                     => other
    }

  protected def unprocessableEntityMessage(apiName: String, error: UpstreamErrorResponse): String = {
    val body = responseBodyOf(error)
    Try(Json.parse(body)).toOption.flatMap(_.validate[UpstreamErrorResponseBody].asOpt) match {
      case Some(UpstreamErrorResponseBody(detail)) =>
        s"$apiName returned 422 Unprocessable Entity. code=${detail.code}, text=${detail.text}"
      case None =>
        s"$apiName returned 422 Unprocessable Entity but the error body could not be parsed. Body: $body"
    }
  }
}
