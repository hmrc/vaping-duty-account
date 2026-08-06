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

package uk.gov.hmrc.vapingdutyaccount.controllers.vpdSummary

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalactic.Prettifier
import org.scalactic.source.Position
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.{HeaderNames as PlayHeaderNames, Status as HttpStatus}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.{InternalServerException, HeaderNames as HmrcHeaderNames}
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.connectors.payments.PaymentsConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences
import uk.gov.hmrc.vapingdutyaccount.models.payments.{PaymentsResponse as UpstreamPaymentsResponse}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*
import uk.gov.hmrc.vapingdutyaccount.services.{ObligationService, GetObligationsService, PaymentsService, VPDSummaryService}

import java.time.Clock
import scala.concurrent.Future

class VPDSummaryControllerSpec extends SpecBase with MockitoSugar {
  val mockAuthConnector: AuthConnector                 = mock[AuthConnector]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val mockGetObligationsService: GetObligationsService       = mock[GetObligationsService]
  val mockPaymentsConnector: PaymentsConnector          = mock[PaymentsConnector]
  val config: AppConfig                                = mock[AppConfig]
  val mockVPDSummaryService: VPDSummaryService         = new VPDSummaryService(config, mockSubscriptionConnector, mockGetObligationsService, new ObligationService(), mockPaymentsConnector, new PaymentsService(), Clock.systemDefaultZone())

  when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)

  when(config.serviceName).thenReturn("Vaping Products Duty")
  when(config.serviceId).thenReturn("VPD")
  when(config.xCorrelationId).thenReturn("X-Correlation-Id")
  when(config.selfHref(any())).thenReturn(s"/vaping-duty-account/vpd/summary/$vpdId")
  when(config.manageContactPreferenceUrl).thenReturn("/vaping-duty/contact-preferences/how-should-we-contact-you")
  when(config.completeReturnUrlPrefix).thenReturn("/vaping-duty/complete-return/before-you-start")
  when(config.viewReturnsUrl).thenReturn("/vaping-duty/view-your-returns")
  when(config.makePaymentUrl).thenReturn("/vaping-duty-finance/pay")

  when(mockPaymentsConnector.getPayments()(using any()))
    .thenReturn(Future.successful(UpstreamPaymentsResponse(Seq.empty, Some(BigDecimal(0)))))

  val fakeRequestWithReqId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest.apply(
    method = "GET",
    uri = "/",
    headers = FakeHeaders(
      Seq(
        PlayHeaderNames.HOST       -> "localhost",
        HmrcHeaderNames.xRequestId -> "a request id"
      )
    ),
    body = AnyContentAsEmpty
  )

  val fakeRequestWithCorrelationId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest.apply(
    method = "GET",
    uri = "/",
    headers = FakeHeaders(
      Seq(
        PlayHeaderNames.HOST  -> "localhost",
        config.xCorrelationId -> "a correlation id"
      )
    ),
    body = AnyContentAsEmpty
  )

  val fakeRequestWithReqAndCorrelationId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest.apply(
    method = "GET",
    uri = "/",
    headers = FakeHeaders(
      Seq(
        PlayHeaderNames.HOST       -> "localhost",
        config.xCorrelationId      -> "a correlation id",
        HmrcHeaderNames.xRequestId -> "a request id"
      )
    ),
    body = AnyContentAsEmpty
  )

  val controller: VPDSummaryController = new VPDSummaryController(
    config,
    cc,
    fakeAuthorisedAction,
    mockVPDSummaryService
  )
  val badVpdId: String                    = "bad-invalid-vpdid"

  private def getContactMethod(preference: Boolean): String = {
    if (preference == true) "EMAIL" else "POST"
  }

  private def getContactPreferenceLines(subscription: SubscriptionContactPreferences): String = {
    val contactMethod = getContactMethod(subscription.paperlessPreference)
    val statusLine     =
      if (subscription.paperlessPreference) {
        val bounced = subscription.bouncedEmail.getOrElse(false)
        s""","contactPreferenceStatus" : { "bouncedEmail" : $bounced }"""
      } else {
        ""
      }

    s""""contactPreference" : "$contactMethod"$statusLine"""
  }

  def getExpectedAPIResponse(subscription: SubscriptionContactPreferences, approvalStatus: String = "APPROVED"): JsValue = Json.parse(s"""
       |{
       |  "service" : {
       |    "name" : "${config.serviceName}",
       |    "id" : "${config.serviceId}"
       |  },
       |  "identifiers" : {
       |    "vpdId" : "$vpdId"
       |  },
       |  "access" : {
       |    "hasSubscriptionSummaryError" : false,
       |    "approvalStatus" : "$approvalStatus"
       |  },
       |  ${getContactPreferenceLines(subscription)},
       |  "payments" : {
       |    "hasPaymentsError" : false,
       |    "balance" : {
       |      "amount" : 0,
       |      "isMultiplePaymentDue" : false
       |    }
       |  },
       |  "links" : {
       |    "self" : {
       |      "href" : "/vaping-duty-account/vpd/summary/${vpdId}",
       |      "method" : "GET"
       |    },
       |    "manageContactPreference" : {
       |      "href" : "/vaping-duty/contact-preferences/how-should-we-contact-you",
       |      "method" : "GET"
       |    }
       |  }
       |}
       |""".stripMargin)

  val expectedDegradedResponse: JsValue = Json.parse(s"""
       |{
       |  "service" : {
       |    "name" : "${config.serviceName}",
       |    "id" : "${config.serviceId}"
       |  },
       |  "identifiers" : {
       |    "vpdId" : "$vpdId"
       |  },
       |  "access" : {
       |    "hasSubscriptionSummaryError" : true
       |  },
       |  "payments" : {
       |    "hasPaymentsError" : false,
       |    "balance" : {
       |      "amount" : 0,
       |      "isMultiplePaymentDue" : false
       |    }
       |  },
       |  "links" : {
       |    "self" : {
       |      "href" : "/vaping-duty-account/vpd/summary/${vpdId}",
       |      "method" : "GET"
       |    },
       |    "manageContactPreference" : {
       |      "href" : "/vaping-duty/contact-preferences/how-should-we-contact-you",
       |      "method" : "GET"
       |    }
       |  }
       |}
       |""".stripMargin)

  private def assertHeaderIsPresentOn(result: Future[Result], header: String) = {
    assert(headers(result).get(header).get.isInstanceOf[String])
  }

  "VPDSummary must " - {
    "return data in expected shape when calling the API (PaperlessPreference is true) and response headers when request id were received" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesEmailSelected))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesEmailSelected)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is true) and response headers when correlation id were received" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesEmailSelected))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithCorrelationId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesEmailSelected)
      assertHeaderIsPresentOn(result, config.xCorrelationId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is true) and response headers when correlation id & request id were received" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesEmailSelected))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqAndCorrelationId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesEmailSelected)
      assertHeaderIsPresentOn(result, config.xCorrelationId)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is false)" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesPostNoEmail))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesPostNoEmail)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "must return a degraded response and preserve headers [CorrelationId, RequestId] if we receive an error from ETMP" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.failed(new InternalServerException("")))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqAndCorrelationId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe expectedDegradedResponse
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
      assertHeaderIsPresentOn(result, config.xCorrelationId)
    }

    "must return APIErrors.ServiceUnavailable when feature switch is set to false" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(false)

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqAndCorrelationId)

      status(result)        mustBe HttpStatus.SERVICE_UNAVAILABLE
      contentAsJson(result) mustBe Json.toJson(APIErrors.ServiceUnavailable)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
      assertHeaderIsPresentOn(result, config.xCorrelationId)
    }

    "return access APPROVED when the subscription is approved and not insolvent" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesApproved))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesApproved, "APPROVED")
    }

    "return access DEREGISTERED when the subscription is deregistered" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesDeregistered))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesDeregistered, "DEREGISTERED")
    }

    "return access REVOKED when the subscription is revoked" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesRevoked))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesRevoked, "REVOKED")
    }

    "return access INSOLVENT when the subscription is approved but insolvent" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesInsolvent))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesInsolvent, "INSOLVENT")
    }
  }
}