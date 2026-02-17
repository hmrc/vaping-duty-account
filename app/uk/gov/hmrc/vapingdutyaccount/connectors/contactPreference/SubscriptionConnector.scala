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
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.vapingdutyaccount.config.{AppConfig, CircuitBreakerProvider}
import uk.gov.hmrc.vapingdutyaccount.connectors.helpers.HIPHeaders
import uk.gov.hmrc.vapingdutyaccount.models.ErrorCodes
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubscriptionConnector @Inject()(
  config: AppConfig,
  headers: HIPHeaders,
  circuitBreakerProvider: CircuitBreakerProvider,
  implicit val system: ActorSystem,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  implicit val scheduler: Scheduler = system.scheduler

  def getSubscriptionContactPreferences(
    vpdId: String
  )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, SubscriptionContactPreferences]] =
      retry(
        () => call(vpdId),
        attempts = config.retryAttempts,
        delay = config.retryAttemptsDelay
      ).recoverWith { _ =>
        Future.successful(Left(ErrorCodes.unexpectedResponse))
      }

  private def call(vpdId: String)(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, SubscriptionContactPreferences]] =
    circuitBreakerProvider.get().withCircuitBreaker {
      logger.info(
        s"[SubscriptionConnector] [getSubscriptionContactPreferences] Fetching subscription summary for vpdId $vpdId"
      )
      httpClient
        .get(url"${config.getSubscriptionUrl(vpdId)}")
        .setHeader(headers.subscriptionHeaders(): _*)
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case OK                   =>
              Try {
                response.json.as[SubscriptionContactPreferences]
              } match {
                case Success(doc) =>
                  logger.info(
                    s"[SubscriptionConnector] [getSubscriptionContactPreferences] Retrieved subscription summary success for vpdId $vpdId"
                  )
                  Future.successful(Right(doc))
                case Failure(error)   =>
                  logger.warn(
                    s"[SubscriptionConnector] [getSubscriptionContactPreferences] Unable to parse subscription summary success for vpdId $vpdId with $error"
                  )
                  Future.successful(
                    Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse subscription summary success"))
                  )
              }
            case BAD_REQUEST          =>
              logger.warn(
                s"[SubscriptionConnector] [getSubscriptionContactPreferences] Bad request sent to get subscription for vpdId $vpdId"
              )
              Future.successful(Left(ErrorResponse(BAD_REQUEST, "Bad request")))
            case NOT_FOUND            =>
              logger.warn(
                s"[SubscriptionConnector] [getSubscriptionContactPreferences] No subscription summary found for vpdId $vpdId"
              )
              Future.successful(Left(ErrorResponse(NOT_FOUND, "Subscription summary not found")))
            case UNPROCESSABLE_ENTITY =>
              logger.warn(
                s"[SubscriptionConnector] [getSubscriptionContactPreferences] Subscription summary request unprocessable for vpdId $vpdId"
              )
              Future.successful(Left(ErrorResponse(UNPROCESSABLE_ENTITY, "Unprocessable entity")))
            case _                    =>
              logger.warn(
                s"[SubscriptionConnector] [getSubscriptionContactPreferences] An error was returned while trying to fetch subscription summary for vpdId $vpdId"
              )
              Future.failed(new InternalServerException(response.body))
          }
        }
    }
}
