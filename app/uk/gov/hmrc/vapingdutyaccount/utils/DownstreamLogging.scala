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

package uk.gov.hmrc.vapingdutyaccount.utils

import play.api.Logging
import play.api.libs.json.{JsPath, JsonValidationError}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

trait DownstreamLogging extends Logging {

  def logNonHttpError(prefix: String, hc: HeaderCarrier, ex: Exception): String = {
    val errorMsg = s"$prefix Non-HTTP error: ${ex.getMessage}"
    logger.error(errorMsg, ex)
    errorMsg
  }

  def logBackendError(prefix: String, response: HttpResponse): BackendError = {
    val message = s"$prefix Backend error - Status: ${response.status}"
    val body = response.body
    logger.error(s"$message, Body: $body")
    BackendError(message, body)
  }

  def formatJsonErrors(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): String =
    errors.map { case (path, errs) =>
      s"${path.toString()}: ${errs.map(_.message).mkString(", ")}"
    }.mkString("; ")
}

case class BackendError(message: String, body: String)