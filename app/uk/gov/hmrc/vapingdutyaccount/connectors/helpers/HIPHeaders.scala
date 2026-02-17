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

package uk.gov.hmrc.vapingdutyaccount.connectors.helpers

import play.api.http.HeaderNames
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.utils.DateTimeHelper

import java.time.{Clock, Instant}
import java.util.Base64
import javax.inject.Inject

class HIPHeaders @Inject() (randomUUIDGenerator: RandomUUIDGenerator, appConfig: AppConfig, clock: Clock) {
  private val correlationIdHeader: String       = "correlationid"
  private val xMessageTypeHeader: String        = "X-Message-Type"
  private val xOriginatingSystemHeader: String  = "X-Originating-System"
  private val xReceiptDateHeader: String        = "X-Receipt-Date"
  private val xRegimeHeader: String             = "X-Regime"
  private val xTransmittingSystemHeader: String = "X-Transmitting-System"

  private val mdtp = "MDTP"
  private val hip  = "HIP"
  private val messageType = "VPDSubscriptionDisplay"
  private val regime = "VPD"
  
  def subscriptionHeaders(): Seq[(String, String)] =
    Seq(
      (HeaderNames.AUTHORIZATION, authorizationForSubscription()),
      (correlationIdHeader, randomUUIDGenerator.uuid),
      (xMessageTypeHeader, messageType),
      (xOriginatingSystemHeader, mdtp),
      (xReceiptDateHeader, DateTimeHelper.formatISOInstantSeconds(Instant.now(clock))),
      (xRegimeHeader, regime),
      (xTransmittingSystemHeader, hip)
    )

  def submissionHeaders(): Seq[(String, String)] =
    Seq(
      (HeaderNames.AUTHORIZATION, authorizationForSubmission()),
      (correlationIdHeader, randomUUIDGenerator.uuid),
      (xOriginatingSystemHeader, mdtp),
      (xReceiptDateHeader, DateTimeHelper.formatISOInstantSeconds(Instant.now(clock))),
      (xTransmittingSystemHeader, hip)
    )

  private def authorizationForSubscription(): String = {
    val clientId = appConfig.subscriptionClientId
    val secret   = appConfig.subscriptionSecret

    val encoded = Base64.getEncoder.encodeToString(s"$clientId:$secret".getBytes("UTF-8"))
    s"Basic $encoded"
  }

  private def authorizationForSubmission(): String = {
    val clientId = appConfig.submitPreferencesClientId
    val secret   = appConfig.submitPreferencesSecret

    val encoded = Base64.getEncoder.encodeToString(s"$clientId:$secret".getBytes("UTF-8"))
    s"Basic $encoded"
  }
}
