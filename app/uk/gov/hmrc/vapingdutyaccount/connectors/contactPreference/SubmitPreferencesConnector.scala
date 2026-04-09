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

import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.apache.pekko.pattern.retry
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.vapingdutyaccount.config.{AppConfig, CircuitBreakerProvider}
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubmitPreferencesParser.{SubmitPreferencesDetailsType, SubmitPreferencesHttpReads}
import uk.gov.hmrc.vapingdutyaccount.connectors.helpers.HIPHeaders
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{PaperlessPreferenceSubmission, SubmitPreferencesErrorResponse}
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.utils.DownstreamLogging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class SubmitPreferencesServerErrorException(message: String) extends Exception(message)

class SubmitPreferencesConnector @Inject() (
                                             config: AppConfig,
                                             headers: HIPHeaders,
                                             circuitBreakerProvider: CircuitBreakerProvider,
                                             implicit val system: ActorSystem,
                                             implicit val httpClient: HttpClientV2
                                           )(implicit ec: ExecutionContext)
  extends DownstreamLogging {

  implicit val scheduler: Scheduler = system.scheduler

  private val LOG_PREFIX = "[SubmitPreferencesConnector][submitContactPreferences]"

  def submitContactPreferences(contactPreferenceSubmission: PaperlessPreferenceSubmission, vpdId: VpdId)
                              (implicit hc: HeaderCarrier): Future[SubmitPreferencesDetailsType] =
      retry(
        () => submitCall(contactPreferenceSubmission, vpdId),
        attempts = config.retryAttemptsPost,
        delay = config.retryAttemptsDelay
      ).recoverWith { case ex: Exception =>
        logger.error(s"$LOG_PREFIX All retry attempts failed for vpdId $vpdId", ex)
        val errMsg = logNonHttpError(LOG_PREFIX, hc, ex)
        Future.successful(Left(SubmitPreferencesErrorResponse(errMsg, None, None)))
      }

  private def submitCall(contactPreferenceSubmission: PaperlessPreferenceSubmission, vpdId: VpdId)
                        (implicit hc: HeaderCarrier): Future[SubmitPreferencesDetailsType] =
    circuitBreakerProvider.get().withCircuitBreaker {
      httpClient
        .put(url"${config.submitPreferencesUrl(vpdId)}")
        .setHeader(headers.submissionHeaders(): _*)
        .withBody(Json.toJson(contactPreferenceSubmission))
        .execute[SubmitPreferencesDetailsType](SubmitPreferencesHttpReads, ec)
      .flatMap {
        case Left(error:SubmitPreferencesErrorResponse) if error.statusCode.exists(_ >= 500) =>
          Future.failed(SubmitPreferencesServerErrorException(s"Server error: ${error.error}"))
        case other =>
          Future.successful(other)
      }
      .recoverWith {
        case ex: SubmitPreferencesServerErrorException =>
          Future.failed(ex)
        case ex: Exception =>
          val errMsg = logNonHttpError(LOG_PREFIX, hc, ex)
          Future.successful(Left(SubmitPreferencesErrorResponse(errMsg, None, None)))
      }
    }
}
