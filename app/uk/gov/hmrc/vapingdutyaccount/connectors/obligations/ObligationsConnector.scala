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

package uk.gov.hmrc.vapingdutyaccount.connectors.obligations

import play.api.Logging
import play.api.http.Status.OK
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.models.obligations.ObligationsResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ObligationsConnector @Inject() (
  config: AppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  def getObligations(vpdId: VpdId)(using hc: HeaderCarrier): Future[Option[ObligationsResponse]] =
    httpClient
      .get(url"${config.getObligationsUrl(vpdId)}")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Try {
              response.json.as[ObligationsResponse]
            } match {
              case Success(obligations) =>
                Future.successful(Some(obligations))
              case Failure(error) =>
                logger.warn(s"Failed to parse obligations response for vpdId $vpdId: ${error.getMessage}")
                Future.successful(None)
            }
          case status =>
            logger.warn(s"Unexpected response from obligations API for vpdId $vpdId. Status: $status")
            Future.successful(None)
        }
      }
      .recoverWith { case exception: Exception =>
        logger.warn(s"Exception occurred while fetching obligations for vpdId $vpdId: ${exception.getMessage}")
        Future.successful(None)
      }
}