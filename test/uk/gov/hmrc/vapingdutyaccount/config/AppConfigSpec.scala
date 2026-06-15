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

import uk.gov.hmrc.vapingdutyaccount.base.SpecBase

class SpecBaseWithConfigOverrides extends SpecBase {
  override def configOverrides: Map[String, Any] = Map(
    "appName"                                                        -> "appName",
    "microservice.services.subscription.protocol"                    -> "http",
    "microservice.services.subscription.host"                        -> "subscriptionhost",
    "microservice.services.subscription.port"                        -> 12345,
    "microservice.services.subscription.clientId"                    -> "subscription clientId",
    "microservice.services.subscription.secret"                      -> "subscription secret",
    "microservice.services.subscription.url.subscriptionSummary"     -> "/etmp/RESTAdapter/excise/subscriptionsummary",
    "microservice.services.email-verification.protocol"              -> "http",
    "microservice.services.email-verification.host"                  -> "emailverificationhost",
    "microservice.services.email-verification.port"                  -> 12345,
    "microservice.services.email-verification.url.getVerifiedEmails" -> "/email-verification/verification-status",
    "microservice.services.submit-preferences.protocol"              -> "http",
    "microservice.services.submit-preferences.host"                  -> "submissionhost",
    "microservice.services.submit-preferences.port"                  -> 12345,
    "microservice.services.submit-preferences.clientId"              -> "submission clientId",
    "microservice.services.submit-preferences.secret"                -> "submission secret",
    "microservice.services.submit-preferences.url.submitPreferences" -> "/etmp/RESTAdapter/email-contact-preference",
    "downstream-apis.idType"                                         -> "ZVPD",
    "downstream-apis.regime"                                         -> "VPD",
    "crypto.key"                                                     -> "cryptokey",
    "crypto.isEnabled"                                               -> true,
    "enrolment.serviceName"                                          -> "HMRC-VPD-ORG",
    "features.email-verification-stub"                               -> false,
    "service.links.manageContactPreference"                          -> "/vaping-duty/contact-preferences/how-should-we-contact-you",
    "service.links.completeReturn"                                   -> "/vaping-duty/complete-return/before-you-start",
    "service.links.viewReturns"                                      -> "/vaping-duty/view-your-returns"
  )
}

class SpecBaseWithEmailVerificationStubs extends SpecBase {
  override def configOverrides: Map[String, Any] = Map(
    "microservice.services.vaping-duty-stubs.protocol"               -> "http",
    "microservice.services.vaping-duty-stubs.host"                   -> "stubshost",
    "microservice.services.vaping-duty-stubs.port"                   -> 54321,
    "microservice.services.email-verification.url.getVerifiedEmails" -> "/email-verification/verification-status",
    "features.email-verification-stub"                               -> true
  )
}

class AppConfigSpec extends SpecBaseWithConfigOverrides {
  "AppConfig" - {
    "must return the appName" in {
      appConfig.appName mustBe "appName"
    }

    "for subscriptions" - {
      "must return the getSubscription url" in {
        appConfig.getSubscriptionUrl(
          vpdId
        ) mustBe s"http://subscriptionhost:12345/etmp/RESTAdapter/excise/subscriptionsummary/$vpdId"
      }

      "must return the client id" in {
        appConfig.subscriptionClientId mustBe "subscription clientId"
      }

      "must return the secret" in {
        appConfig.subscriptionSecret mustBe "subscription secret"
      }
    }

    "for email verification" - {
      "must return the getVerifiedEmailsUrl when email verification stubs is toggled off" in {
        appConfig.getVerifiedEmailsUrl(
          credId
        ) mustBe s"http://emailverificationhost:12345/email-verification/verification-status/$credId"
      }
    }

    "for contact preference submission" - {
      "must return the submitContactPreferences url" in {
        appConfig.submitPreferencesUrl(
          vpdId
        ) mustBe s"http://submissionhost:12345/etmp/RESTAdapter/email-contact-preference/VPD/ZVPD/$vpdId"
      }

      "must return the client id" in {
        appConfig.submitPreferencesClientId mustBe "submission clientId"
      }

      "must return the secret" in {
        appConfig.submitPreferencesSecret mustBe "submission secret"
      }
    }

    "must return the config relating to downstream APIs" - {
      "for idType" in {
        appConfig.idType mustBe "ZVPD"
      }

      "for regime" in {
        appConfig.regime mustBe "VPD"
      }
    }

    "must return the config relating BTA tile API" - {
      "for BTA tile API feature switch" in {
        appConfig.vpdSummaryRESTAPIEnabled mustBe true
      }

      "for serviceName" in {
        appConfig.serviceName mustBe "Vaping Products Duty"
      }

      "for serviceId" in {
        appConfig.serviceId mustBe "VPD"
      }

      "for vpdSummaryRESTAPIGetHref" in {
        appConfig.vpdSummaryRESTAPIGetHref(vpdId) mustBe s"/vpd/summary/$vpdId"
      }

      "for vpdSummaryRESTAPIGetContactPreferencesHref" in {
        appConfig.vpdSummaryRESTAPIGetContactPreferencesHref mustBe "/vpd/contact-preference"
      }

      "for xCorrelationId" in {
        appConfig.xCorrelationId mustBe "X-Correlation-Id"
      }
    }

    "for summary API links" - {
      "must return selfHref with vpdId" in {
        appConfig.selfHref(vpdId) mustBe s"/vaping-duty-account/vpd/summary/$vpdId"
      }

      "must return manageContactPreferenceUrl" in {
        appConfig.manageContactPreferenceUrl mustBe "/vaping-duty/contact-preferences/how-should-we-contact-you"
      }

      "must return completeReturnUrlPrefix" in {
        appConfig.completeReturnUrlPrefix mustBe "/vaping-duty/complete-return/before-you-start"
      }

      "must return viewReturnsUrl" in {
        appConfig.viewReturnsUrl mustBe "/vaping-duty/view-your-returns"
      }
    }

    "for encryption" - {
      "must return the encryption key" in {
        appConfig.cryptoKey mustBe "cryptokey"
      }

      "must return whether encryption is enabled" in {
        appConfig.cryptoEnabled mustBe true
      }
    }

    "must return the enrolment service name" in {
      appConfig.enrolmentServiceName mustBe "HMRC-VPD-ORG"
    }

    "getConfStringAndThrowIfNotFound must" - {
      "return a key if found" in {
        appConfig.getConfStringAndThrowIfNotFound("subscription.secret") mustBe "subscription secret"
      }

      "throw an exception if not found" in {
        a[RuntimeException] mustBe thrownBy(appConfig.getConfStringAndThrowIfNotFound("blah"))
      }
    }
  }
}

class AppConfigWithEmailVerificationStubsSpec extends SpecBaseWithEmailVerificationStubs {
  "for email verification" - {
    "must return the getVerifiedEmailsUrl when email verification stubs is toggled on" in {
      appConfig.getVerifiedEmailsUrl(
        credId
      ) mustBe s"http://stubshost:54321/email-verification/verification-status/$credId"
    }
  }
}
