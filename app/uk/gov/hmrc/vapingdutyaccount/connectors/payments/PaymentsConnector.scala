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

package uk.gov.hmrc.vapingdutyaccount.connectors.payments

import play.api.Logging
import play.api.http.Status.OK
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.payments.PaymentsResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PaymentsConnector @Inject() (
  config: AppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  def getPayments()(using hc: HeaderCarrier): Future[PaymentsResponse] =
    httpClient
      .get(url"${config.getPaymentsUrl}")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Try(response.json.as[PaymentsResponse]) match {
              case Success(payments) =>
                Future.successful(payments)
              case Failure(_) =>
                Future.failed(InternalServerException("Parsing failed for payments response"))
            }
          case status =>
            logger.warn(s"Failed to retrieve payments with status: $status")
            Future.failed(InternalServerException("Failed to retrieve payments"))
        }
      }
}
