/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.Logging
import play.api.http.Status.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.vapingdutyaccount.config.{AppConfig, CircuitBreakerProvider}
import uk.gov.hmrc.vapingdutyaccount.connectors.helpers.HIPHeaders
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences
import uk.gov.hmrc.vapingdutyaccount.models.exceptions.*
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubscriptionConnector @Inject() (
    config: AppConfig,
    headers: HIPHeaders,
    circuitBreakerProvider: CircuitBreakerProvider,
    implicit val system: ActorSystem,
    implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  implicit val scheduler: Scheduler = system.scheduler

  private val LOG_PREFIX = "[SubscriptionConnector][getSubscriptionContactPreferences]"
  
  def getSubscriptionContactPreferences(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[SubscriptionContactPreferences] =
    retry(
      () => call(vpdId),
      attempts = config.retryAttempts,
      delay = config.retryAttemptsDelay
    ).recoverWith { case ex =>
      logger.error(s"$LOG_PREFIX All retry attempts failed for vpdId $vpdId", ex)
      Future.failed(UpstreamServiceException(s"Failed to get subscription for $vpdId after retries", 500, ex))
    }

  private def call(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[SubscriptionContactPreferences] =
    circuitBreakerProvider.get().withCircuitBreaker {
      httpClient
        .get(url"${config.getSubscriptionUrl(vpdId)}")
        .setHeader(headers.subscriptionHeaders(): _*)
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case OK                   =>
              parseSuccessResponse(vpdId, response)
            case BAD_REQUEST          =>
              logger.error(s"$LOG_PREFIX [BUG] Bad request sent to get subscription for vpdId $vpdId - check our request payload")
              Future.failed(BadRequestException(s"Bad request for subscription $vpdId"))
            case NOT_FOUND            =>
              logger.warn(s"$LOG_PREFIX No subscription summary found for vpdId $vpdId")
              Future.failed(EntityNotFoundException(s"Subscription not found for $vpdId"))
            case UNPROCESSABLE_ENTITY =>
              logger.error(s"$LOG_PREFIX [BUG] Subscription summary request unprocessable for vpdId $vpdId - check our JSON structure")
              Future.failed(UnprocessableEntityException(s"Unprocessable entity for $vpdId"))
            case INTERNAL_SERVER_ERROR | BAD_GATEWAY | SERVICE_UNAVAILABLE =>
              logger.warn(s"$LOG_PREFIX Upstream service error (${response.status}) while fetching subscription for vpdId $vpdId")
              Future.failed(UpstreamServiceException(s"Upstream error for $vpdId", response.status))
            case statusCode           =>
              logger.warn(s"$LOG_PREFIX Unexpected status code ($statusCode) while fetching subscription for vpdId $vpdId")
              Future.failed(UpstreamServiceException(s"Unexpected response for $vpdId", statusCode))
          }
        }
    }

  private def parseSuccessResponse(vpdId: VpdId, response: HttpResponse): Future[SubscriptionContactPreferences] = {
    Try {
      response.json.as[SubscriptionContactPreferences]
    } match {
      case Success(contactPreferenceResponse) =>
        Future.successful(contactPreferenceResponse)
      case Failure(error) =>
        logger.error(s"$LOG_PREFIX [BUG] Unable to parse subscription summary success response for vpdId $vpdId - check our model", error)
        Future.failed(ParseException(s"Parse failure for $vpdId", error))
    }
  }
}
