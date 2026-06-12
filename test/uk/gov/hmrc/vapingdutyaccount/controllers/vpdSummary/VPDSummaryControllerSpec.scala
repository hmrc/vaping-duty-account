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
import uk.gov.hmrc.vapingdutyaccount.connectors.obligations.ObligationsConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.*
import uk.gov.hmrc.vapingdutyaccount.services.{ObligationService, VPDSummaryService}

import scala.concurrent.Future

class VPDSummaryControllerSpec extends SpecBase with MockitoSugar {
  val mockAuthConnector: AuthConnector                 = mock[AuthConnector]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val mockObligationsConnector: ObligationsConnector   = mock[ObligationsConnector]
  val config: AppConfig                                = mock[AppConfig]
  val mockVPDSummaryService: VPDSummaryService   = new VPDSummaryService(config, mockSubscriptionConnector, mockObligationsConnector, new ObligationService())

  when(config.vpdSummaryRESTAPIEnabled).thenReturn(true)

  when(config.serviceName).thenReturn("Vaping Products Duty")
  when(config.serviceId).thenReturn("VPD")
  when(config.xCorrelationId).thenReturn("X-Correlation-Id")
  when(config.selfHref(any())).thenReturn(s"/vaping-duty-account/vpd/summary/$vpdId")
  when(config.manageContactPreferenceUrl).thenReturn("/vaping-duty/contact-preferences/how-should-we-contact-you")
  when(config.completeReturnUrlPrefix).thenReturn("/vaping-duty/complete-return/before-you-start")
  when(config.viewReturnsUrl).thenReturn("/vaping-duty/view-your-returns")

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

  def getExpectedAPIResponse(subscription: SubscriptionContactPreferences): JsValue = Json.parse(s"""
       |{
       |  "service" : {
       |    "name" : "${config.serviceName}",
       |    "id" : "${config.serviceId}"
       |  },
       |  "identifiers" : {
       |    "vpdId" : "$vpdId"
       |  },
       |  "contactPreference" : "${getContactMethod(subscription.paperlessPreference)}",
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
      when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesEmailSelected)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is true) and response headers when correlation id were received" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesEmailSelected))
      when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithCorrelationId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesEmailSelected)
      assertHeaderIsPresentOn(result, config.xCorrelationId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is true) and response headers when correlation id & request id were received" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesEmailSelected))
      when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqAndCorrelationId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesEmailSelected)
      assertHeaderIsPresentOn(result, config.xCorrelationId)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is false)" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(contactPreferencesPostNoEmail))
      when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesPostNoEmail)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "must return APIErrors.InternalServerError and preserve headers [CorrelationId, RequestId] if we receive an error from ETMP" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future.failed(new InternalServerException("")))
      when(mockObligationsConnector.getObligations(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Some(obligationsResponse)))

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
  }
}