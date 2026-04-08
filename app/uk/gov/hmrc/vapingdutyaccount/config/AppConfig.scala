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

package uk.gov.hmrc.vapingdutyaccount.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vapingdutyaccount.models.identifiers.{CredentialId, VpdId}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration

@SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {
  val appName: String = config.get[String]("appName")

  private val subscriptionHost: String                  = servicesConfig.baseUrl("subscription")
  lazy val subscriptionClientId: String                 = getConfStringAndThrowIfNotFound("subscription.clientId")
  lazy val subscriptionSecret: String                   = getConfStringAndThrowIfNotFound("subscription.secret")
  private lazy val subscriptionGetSubscriptionUrlPrefix = getConfStringAndThrowIfNotFound(
    "subscription.url.subscriptionSummary"
  )

  private val emailVerificationStubsEnabled: Boolean = config.get[Boolean]("features.email-verification-stub")

  private val stubsHost: String                             = servicesConfig.baseUrl("vaping-duty-stubs")
  private val emailVerificationHost: String                 = servicesConfig.baseUrl("email-verification")
  private lazy val emailVerificationGetVerifiedEmailsPrefix = getConfStringAndThrowIfNotFound(
    "email-verification.url.getVerifiedEmails"
  )

  private val submitPreferencesHost: String   = servicesConfig.baseUrl("submit-preferences")
  lazy val submitPreferencesClientId: String  = getConfStringAndThrowIfNotFound("submit-preferences.clientId")
  lazy val submitPreferencesSecret: String    = getConfStringAndThrowIfNotFound("submit-preferences.secret")
  private lazy val submitPreferencesUrlPrefix = getConfStringAndThrowIfNotFound(
    "submit-preferences.url.submitPreferences"
  )

  val idType: String = config.get[String]("downstream-apis.idType")
  val regime: String = config.get[String]("downstream-apis.regime")

  val cryptoKey: String           = config.get[String]("crypto.key")
  val cryptoEnabled: Boolean      = config.get[Boolean]("crypto.isEnabled")
  val dbTimeToLiveInSeconds: Long = config.get[Long]("mongodb.timeToLive")

  val enrolmentServiceName: String   = config.get[String]("enrolment.serviceName")
  val enrolmentIdentifierKey: String = config.get[String]("enrolment.identifierKey")

  def getSubscriptionUrl(vpdId: VpdId): String =
    s"$subscriptionHost$subscriptionGetSubscriptionUrlPrefix/$vpdId"

  def getVerifiedEmailsUrl(credId: CredentialId): String =
    if (emailVerificationStubsEnabled) {
      s"$stubsHost$emailVerificationGetVerifiedEmailsPrefix/$credId"
    } else {
      s"$emailVerificationHost$emailVerificationGetVerifiedEmailsPrefix/$credId"
    }

  def submitPreferencesUrl(vpdId: VpdId): String =
    s"$submitPreferencesHost$submitPreferencesUrlPrefix/$regime/$idType/$vpdId"

  private[config] def getConfStringAndThrowIfNotFound(key: String) =
    servicesConfig.getConfString(key, throw new RuntimeException(s"Could not find services config key '$key'"))

  // API retry attempts
  lazy val retryAttempts: Int                 = config.get[Int]("microservice.services.retry.retry-attempts")
  lazy val retryAttemptsPost: Int             = config.get[Int]("microservice.services.retry.retry-attempts-post")
  lazy val retryAttemptsDelay: FiniteDuration =
    config.get[FiniteDuration]("microservice.services.retry.retry-attempts-delay")

  // Circuit breaker
  lazy val maxFailures: Int             = config.get[Int]("microservice.services.circuit-breaker.max-failures")
  lazy val callTimeout: FiniteDuration  =
    config.get[FiniteDuration]("microservice.services.circuit-breaker.call-timeout")
  lazy val resetTimeout: FiniteDuration = {
    config.get[FiniteDuration]("microservice.services.circuit-breaker.reset-timeout")
  }

  /** VPD Summary API
    *
    * Types extracted from OpenAPI specification
    */
  lazy val vpdSummaryRESTAPIEnabled: Boolean = config.get[Boolean]("features.bta.tile.api")

  // Static info for API
  lazy val serviceName: String = config.get[String]("service.name")
  lazy val serviceId: String   = config.get[String]("service.id")

  def vpdSummaryRESTAPIGetHref(vpdId: VpdId): String = s"/vpd/summary/$vpdId"
  lazy val vpdSummaryRESTAPIGetContactPreferencesHref: String   = "/vpd/contact-preference"

  lazy val xCorrelationId: String = "X-Correlation-Id"
}
