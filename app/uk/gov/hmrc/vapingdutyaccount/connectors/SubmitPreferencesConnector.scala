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

package uk.gov.hmrc.vapingdutyaccount.connectors

import org.apache.pekko.actor.{ActorSystem, Scheduler}
import org.apache.pekko.pattern.retry
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, InternalServerException, StringContextOps}
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.vapingdutyaccount.config.{AppConfig, CircuitBreakerProvider}
import uk.gov.hmrc.vapingdutyaccount.connectors.helpers.HIPHeaders
import uk.gov.hmrc.vapingdutyaccount.models.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubmitPreferencesConnector @Inject() (
                                             config: AppConfig,
                                             headers: HIPHeaders,
                                             circuitBreakerProvider: CircuitBreakerProvider,
                                             implicit val system: ActorSystem,
                                             implicit val httpClient: HttpClientV2
                                           )(implicit ec: ExecutionContext)
  extends HttpReadsInstances
    with Logging {

  implicit val scheduler: Scheduler = system.scheduler

  def submitContactPreferences(contactPreferenceSubmission: PaperlessPreferenceSubmission, vpdId: String)
                              (implicit hc: HeaderCarrier):
  Future[Either[ErrorResponse, PaperlessPreferenceSubmittedResponse]] =
      retry(
        () => submitCall(contactPreferenceSubmission, vpdId),
        attempts = config.retryAttemptsPost,
        delay = config.retryAttemptsDelay
      ).recoverWith { _ =>
        Future.successful(Left(ErrorCodes.unexpectedResponse))
      }

  private def submitCall(contactPreferenceSubmission: PaperlessPreferenceSubmission, vpdId: String)
                        (implicit hc: HeaderCarrier):
  Future[Either[ErrorResponse, PaperlessPreferenceSubmittedResponse]] = {

    circuitBreakerProvider.get().withCircuitBreaker {
      logger.info(s"Submitting contact preferences for vpdId $vpdId")
      httpClient
        .put(url"${config.submitPreferencesUrl(vpdId)}")
        .setHeader(headers.submissionHeaders(): _*)
        .withBody(Json.toJson(contactPreferenceSubmission))
        .execute[HttpResponse]
        .flatMap{ parseResponse(_, vpdId) }
    }
  }

  private def parseResponse(response: HttpResponse, vpdId: String) = {
      response match {
        case response if response.status == OK                   =>
          tryParse(vpdId, response)
        case response if response.status == BAD_REQUEST          =>
          logger.warn(s"Bad request returned for contact preference submission for vpdId $vpdId")
          Future.successful(Left(ErrorCodes.badRequest))
        case response if response.status == NOT_FOUND            =>
          logger.warn(s"Not found returned for contact preference submission for vpdId $vpdId")
          Future.successful(Left(ErrorCodes.entityNotFound))
        case response if response.status == UNPROCESSABLE_ENTITY =>
          logger.warn(s"Unprocessable entity returned for contact preference submission for vpdId $vpdId")
          Future.successful(Left(ErrorCodes.invalidJson))
        case response =>
          logger.warn(
            s"Received unexpected response from contact preference submission API (vpdId $vpdId). Status: ${response.status}"
          )
          Future.failed(new InternalServerException(response.body))
    }
  }

  private def tryParse(vpdId: String, response: HttpResponse) = {
    Try(response.json.as[PaperlessPreferenceSubmittedSuccess]) match {
      case Success(submissionResponse) =>
        logger.info(s"Contact preferences submitted successfully for vpdId $vpdId")
        Future.successful(Right(submissionResponse.success))
      case Failure(_) =>
        logger.warn(s"Parsing failed for submission response for vpdId $vpdId")
        Future.successful(Left(ErrorCodes.unexpectedResponse))
    }
  }
}
