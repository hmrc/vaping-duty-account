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

package uk.gov.hmrc.vapingdutyaccount.services.vpdSummaryAPI

import com.google.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummaryAPI.{ContactMethod, Identifier, Links, ManageContactPreference, Self, ServiceInfo, VPDSummary}

import scala.concurrent.{ExecutionContext, Future}

class VPDSummaryAPIService @Inject()(
    config: AppConfig,
    subscriptionConnector: SubscriptionConnector
)(implicit ec: ExecutionContext) {


  def getVPDSummary(vpdId: String)(implicit hc: HeaderCarrier): Future[VPDSummary] = {
    subscriptionConnector
      .getSubscriptionContactPreferencesLight(vpdId)
      .map(createVPDSummary(vpdId, _))
  }

  private def createVPDSummary(vpdId: String, etmpData: SubscriptionContactPreferences) = {
    VPDSummary(
      service = ServiceInfo(config.serviceName, config.serviceId),
      identifiers = Identifier(vpdId),
      contactPreference = ContactMethod.resolve(paperlessPreference = etmpData.paperlessPreference),
      links = Links(
        Self(config.vpdSummaryRESTAPIGetHref(vpdId), "GET"),
        ManageContactPreference(config.vpdSummaryRESTAPIGetContactPreferencesHref, "GET")
      )
    )
  }

}
