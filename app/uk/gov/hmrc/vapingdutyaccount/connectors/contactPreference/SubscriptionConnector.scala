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

import play.api.http.Status.{OK, UNPROCESSABLE_ENTITY}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.helpers.{HIPHeaders, SubscriptionConnectorLogger, UpstreamErrorLogging}
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubscriptionConnector @Inject() (
  config: AppConfig,
  headers: HIPHeaders,
  logger: SubscriptionConnectorLogger,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with UpstreamErrorLogging {

  def getSubscriptionContactPreferences(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[SubscriptionContactPreferences] =
    httpClient
      .get(url"${config.getSubscriptionUrl(vpdId)}")
      .setHeader(headers.subscriptionHeaders(): _*)
      .execute[HttpResponse]
      .flatMap(response => subscriptionParser(response))
      .recoverWith { case _: Exception =>
        logger.warn("An exception was returned while trying to fetch subscription contact preferences")
        Future.failed(InternalServerException("Failed to get subscription contact preferences"))
      }

  private def subscriptionParser(response: HttpResponse): Future[SubscriptionContactPreferences] =
    response.status match {
      case OK =>
        Try {
          response.json.as[SubscriptionContactPreferences]
        } match {
          case Success(contactPreferences) =>
            Future.successful(contactPreferences)
          case Failure(_) =>
            logger.warn("Unable to parse subscription summary successful response")
            Future.failed(InternalServerException("Failed to get subscription contact preferences"))
        }
      case UNPROCESSABLE_ENTITY =>
        logger.warn(unprocessableEntityMessage("Subscription summary API", response))
        Future.failed(InternalServerException("Failed to get subscription contact preferences"))
      case status =>
        logger.warn(s"Unexpected response from subscription summary API. Status: $status")
        Future.failed(InternalServerException("Failed to get subscription contact preferences"))
    }
}
