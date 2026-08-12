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

package uk.gov.hmrc.vapingdutyaccount.services

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.payments.PaymentsConnector
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.Payments

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetPaymentsService @Inject()(
                                    config: AppConfig,
                                    paymentsConnector: PaymentsConnector,
                                    paymentsService: PaymentsService
                                  )(implicit ec: ExecutionContext) extends Logging {

  def getPayments()(using HeaderCarrier): Future[Option[Payments]] =
    if (config.phase2Enabled)
      paymentsConnector.getPayments()
        .map(paymentsService.toPayments)
        .map(Some(_))
        .recover {
          case ex =>
            logger.warn(s"Failed to retrieve payments ${ex.getMessage}")
            Some(Payments(hasPaymentsError = true, balance = None))
        }
    else
      Future.successful(None)
}
