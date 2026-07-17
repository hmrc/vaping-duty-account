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

package uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference

import play.api.http.Status.{OK, UNPROCESSABLE_ENTITY}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.helpers.{HIPHeaders, SubmitPreferencesConnectorLogger, UpstreamErrorLogging}
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{PaperlessPreferenceSubmission, PaperlessPreferenceSubmittedResponse, PaperlessPreferenceSubmittedSuccess}
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubmitPreferencesConnector @Inject() (
  config: AppConfig,
  headers: HIPHeaders,
  logger: SubmitPreferencesConnectorLogger,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with UpstreamErrorLogging {

  def submitContactPreferences(contactPreferenceSubmission: PaperlessPreferenceSubmission, vpdId: VpdId)
                              (implicit hc: HeaderCarrier): Future[PaperlessPreferenceSubmittedResponse] =
    httpClient
      .put(url"${config.submitPreferencesUrl(vpdId)}")
      .setHeader(headers.submissionHeaders(): _*)
      .withBody(Json.toJson(contactPreferenceSubmission))
      .execute[HttpResponse]
      .flatMap(response => submitPreferencesParser(response))
      .recoverWith { case _: Exception =>
        logger.warn("An exception was returned while trying to submit contact preferences")
        Future.failed(InternalServerException("Failed to submit contact preferences"))
      }

  private def submitPreferencesParser(response: HttpResponse): Future[PaperlessPreferenceSubmittedResponse] =
    response.status match {
      case OK =>
        Try {
          response.json.as[PaperlessPreferenceSubmittedSuccess]
        } match {
          case Success(submissionResponse) =>
            Future.successful(submissionResponse.success)
          case Failure(_) =>
            logger.warn("Parsing failed for submission response")
            Future.failed(InternalServerException("Failed to submit contact preferences"))
        }
      case UNPROCESSABLE_ENTITY =>
        logger.warn(unprocessableEntityMessage("Contact preference submission API", response))
        Future.failed(InternalServerException("Failed to submit contact preferences"))
      case status =>
        logger.warn(s"Unexpected response from contact preference submission API. Status: $status")
        Future.failed(InternalServerException("Failed to submit contact preferences"))
    }

}
