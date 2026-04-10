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

import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionParser.{GetSubscriptionHttpReads, SubscriptionDetailsType}
import uk.gov.hmrc.vapingdutyaccount.connectors.helpers.HIPHeaders
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.VpdId
import uk.gov.hmrc.vapingdutyaccount.utils.DownstreamLogging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConnector @Inject() (
    config: AppConfig,
    headers: HIPHeaders,
    httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends DownstreamLogging {

  private val LOG_PREFIX = "[SubscriptionConnector][getSubscriptionContactPreferences]"

  def getSubscriptionContactPreferences(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[SubscriptionDetailsType] =
      httpClient
        .get(url"${config.getSubscriptionUrl(vpdId)}")
        .setHeader(headers.subscriptionHeaders(): _*)
        .execute[SubscriptionDetailsType](GetSubscriptionHttpReads, ec)
        .recoverWith { case ex: Exception =>
          val errMsg = logNonHttpError(LOG_PREFIX, hc, ex)
          Future.successful(Left(new Exception(errMsg)))
        }
}
