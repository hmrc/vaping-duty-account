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
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionSummary
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*
import uk.gov.hmrc.vapingdutyaccount.connectors.payments.PaymentsConnector
import uk.gov.hmrc.vapingdutyaccount.services.*

import scala.concurrent.Future

class VPDSummaryControllerSpec extends SpecBase with MockitoSugar {
  val mockAuthConnector: AuthConnector                 = mock[AuthConnector]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val mockGetObligationsService: GetObligationsService = mock[GetObligationsService]
  val mockPaymentsConnector: PaymentsConnector         = mock[PaymentsConnector]
  val config: AppConfig                                = mock[AppConfig]
  val mockVPDSummaryService: VPDSummaryService         = new VPDSummaryService(
    config,
    mockSubscriptionConnector,
    new ContactPreferenceSummaryService(config),
    new ReturnsSummaryService(config, mockGetObligationsService, new ObligationService(clock)),
    new PaymentsSummaryService(config, mockPaymentsConnector, new PaymentsService())
  )

  when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)

  when(config.serviceName).thenReturn("Vaping Products Duty")
  when(config.serviceId).thenReturn("VPD")
  when(config.xCorrelationId).thenReturn("X-Correlation-Id")
  when(config.selfHref(any())).thenReturn(s"/vaping-duty-account/vpd/summary/$vpdId")
  when(config.manageContactPreferenceUrl).thenReturn("/vaping-duty/contact-preferences/how-should-we-contact-you")
  when(config.completeReturnUrlPrefix).thenReturn("/vaping-duty/complete-return/before-you-start")
  when(config.viewReturnsUrl).thenReturn("/vaping-duty/view-your-returns")
  when(config.makePaymentUrl(any())).thenReturn("/vaping-duty/start-bta-payment")
  when(config.viewPaymentsUrl).thenReturn("/vaping-duty-finance/view-payments")
  when(config.startDirectDebitUrl).thenReturn("/vaping-duty-finance/direct-debit/bta/start")
  when(config.phase2Enabled).thenReturn(true)
  when(config.returnsAndPaymentsEnabled).thenReturn(true)

  when(mockPaymentsConnector.getPayments()(using any()))
    .thenReturn(Future.successful(upstreamPaymentsNothingOwed))

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

  private def getContactPreferenceLines(subscription: SubscriptionSummary): String = {
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

  def getExpectedAPIResponse(subscription: SubscriptionSummary, approvalStatus: String = "APPROVED"): JsValue = Json.parse(s"""
       |{
       |  "service" : {
       |    "name" : "${config.serviceName}",
       |    "id" : "${config.serviceId}"
       |  },
       |  "identifiers" : {
       |    "vpdId" : "$vpdId"
       |  },
       |  "access" : {
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
       |    },
       |    "viewPayments" : {
       |      "href" : "/vaping-duty-finance/view-payments",
       |      "method" : "GET"
       |    },
       |    "setUpDirectDebit" : {
       |      "href" : "/vaping-duty-finance/direct-debit/bta/start",
       |      "method" : "GET"
       |    }
       |  }
       |}
       |""".stripMargin)

  def getExpectedMinimalAPIResponse(approvalStatus: String = "INSOLVENT"): JsValue = Json.parse(s"""
       |{
       |  "service" : {
       |    "name" : "${config.serviceName}",
       |    "id" : "${config.serviceId}"
       |  },
       |  "identifiers" : {
       |    "vpdId" : "$vpdId"
       |  },
       |  "access" : {
       |    "approvalStatus" : "$approvalStatus"
       |  },
       |  "links" : {
       |    "self" : {
       |      "href" : "/vaping-duty-account/vpd/summary/${vpdId}",
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
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(subscriptionSummaryEmailSelected))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(subscriptionSummaryEmailSelected)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is true) and response headers when correlation id were received" in {
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(subscriptionSummaryEmailSelected))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithCorrelationId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(subscriptionSummaryEmailSelected)
      assertHeaderIsPresentOn(result, config.xCorrelationId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is true) and response headers when correlation id & request id were received" in {
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(subscriptionSummaryEmailSelected))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqAndCorrelationId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(subscriptionSummaryEmailSelected)
      assertHeaderIsPresentOn(result, config.xCorrelationId)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is false)" in {
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(subscriptionSummaryPostNoEmail))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(subscriptionSummaryPostNoEmail)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "must return a 500 and preserve headers [CorrelationId, RequestId] if we receive an error from ETMP" in {
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.failed(new InternalServerException("")))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqAndCorrelationId)

      status(result)        mustBe HttpStatus.INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.toJson(APIErrors.InternalServerError)
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
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(subscriptionSummaryApproved))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(subscriptionSummaryApproved, "APPROVED")
    }

    "return access DEREGISTERED when the subscription is deregistered" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(subscriptionSummaryDeregistered))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(subscriptionSummaryDeregistered, "DEREGISTERED")
    }

    "return access REVOKED when the subscription is revoked" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(subscriptionSummaryRevoked))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(subscriptionSummaryRevoked, "REVOKED")
    }

    "return access INSOLVENT and a minimal response with no sections or action links when the subscription is approved but insolvent" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(subscriptionSummaryInsolvent))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedMinimalAPIResponse()
    }

    "return access INSOLVENT and a minimal response when the subscription is revoked but insolvent" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(subscriptionSummaryRevokedInsolvent))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedMinimalAPIResponse()
    }

    "return access INSOLVENT and a minimal response when the subscription is deregistered but insolvent" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(subscriptionSummaryDeregisteredInsolvent))
      when(mockGetObligationsService.getObligationDetails(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedMinimalAPIResponse()
    }

    "return correct response when returnsAndPaymentsEnabled is false" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)
      when(config.returnsAndPaymentsEnabled).thenReturn(false)
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(subscriptionSummaryApproved))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result) mustBe HttpStatus.OK
      
      val json = contentAsJson(result)
      (json \ "access" \ "approvalStatus").as[String] mustBe "APPROVED"
      (json \ "returns").toOption mustBe None
      (json \ "payments").toOption mustBe None
      (json \ "links" \ "setUpDirectDebit" \ "href").as[String] mustBe "/vaping-duty-finance/direct-debit/bta/start"
      (json \ "links" \ "manageContactPreference" \ "href").as[String] mustBe "/vaping-duty/contact-preferences/how-should-we-contact-you"
      (json \ "links" \ "viewPayments").toOption mustBe None
      (json \ "links" \ "makePayment").toOption mustBe None
      (json \ "links" \ "viewReturns").toOption mustBe None
      (json \ "links" \ "completeReturn").toOption mustBe None
    }

    "return correct response when returnsAndPaymentsEnabled is false and subscription fails" in {
      when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)
      when(config.returnsAndPaymentsEnabled).thenReturn(false)
      when(mockSubscriptionConnector.getSubscriptionSummary(eqTo(vpdId))(any()))
        .thenReturn(Future.failed(new InternalServerException("Subscription service error")))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result) mustBe HttpStatus.INTERNAL_SERVER_ERROR
    }
  }
}
