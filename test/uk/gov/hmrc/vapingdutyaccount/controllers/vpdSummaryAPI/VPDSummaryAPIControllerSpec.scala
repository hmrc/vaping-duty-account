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

package uk.gov.hmrc.vapingdutyaccount.controllers.vpdSummaryAPI

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
import uk.gov.hmrc.http.HeaderNames as HmrcHeaderNames
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{SubscriptionContactPreferences, SubscriptionSummary}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummaryAPI.*
import uk.gov.hmrc.vapingdutyaccount.services.vpdSummaryAPI.VPDSummaryAPIService

import scala.concurrent.Future
import scala.compiletime.ops.boolean

class VPDSummaryAPIControllerSpec extends SpecBase with MockitoSugar {
  val mockAuthConnector: AuthConnector                 = mock[AuthConnector]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val config: AppConfig                                = mock[AppConfig]
  val mockVPDSummaryAPIService: VPDSummaryAPIService   = new VPDSummaryAPIService(config, mockSubscriptionConnector)

  when(config.vpdIdPattern).thenReturn("(?:GB|XI)WK[0-9]{7}WK")

  when(config.serviceName).thenReturn("Vaping Products Duty")
  when(config.serviceId).thenReturn("VPD")
  when(config.xCorrelationId).thenReturn("X-Correlation-Id")

  when(config.vpdSummaryRESTAPIGetHref(vpdId)).thenReturn(s"/get/vpdId/${vpdId}")

  when(config.vpdSummaryRESTAPIGetContactPreferencesHref).thenReturn("getContactPrefs.url")

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

  val controller: VPDSummaryAPIController = new VPDSummaryAPIController(
    config,
    cc,
    fakeAuthorisedAction,
    mockVPDSummaryAPIService
  )
  val badVpdId: String          = "bad-invalid-vpdid"

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
      |      "href" : "${config.vpdSummaryRESTAPIGetHref(vpdId)}",
      |      "method" : "GET"
      |    },
      |    "manage-contact-preference" : {
      |      "href" : "${config.vpdSummaryRESTAPIGetContactPreferencesHref}",
      |      "method" : "GET"
      |    }
      |  }
      |}
      |""".stripMargin)

  /** This helper method will assert that the specified header is present on received responses
    */
  private def assertHeaderIsPresentOn(response: Future[Result], header: String) = {
    assert(headers(response).get(header).get.isInstanceOf[String])
  }

  "SummaryAPI must " - {
    "return data in expected shape when calling the API (PaperlessPreference is true) and response headers when request id were received" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionContactPreferences]](Right(contactPreferencesEmailSelected)))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesEmailSelected)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is true) and response headers when correlation id were received" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionContactPreferences]](Right(contactPreferencesEmailSelected)))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithCorrelationId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesEmailSelected)
      assertHeaderIsPresentOn(result, config.xCorrelationId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is true) and response headers when correlation id & request id were received" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionContactPreferences]](Right(contactPreferencesEmailSelected)))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqAndCorrelationId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesEmailSelected)
      assertHeaderIsPresentOn(result, config.xCorrelationId)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "return data in expected shape when calling the API (PaperlessPreference is false)" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionContactPreferences]](Right(contactPreferencesPostNoEmail)))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequestWithReqId)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesPostNoEmail)
      assertHeaderIsPresentOn(result, HmrcHeaderNames.xRequestId)
    }

    "return APIErrors.BadRequest when bad vpdId received" in {
      val result: Future[Result] = controller.getVpdSummary(badVpdId)(fakeRequest)

      status(result)        mustBe HttpStatus.BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(APIErrors.BadRequest)
    }

    "return APIErrors.InternalServerError when appropriate status code received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(
          Future[Either[ErrorResponse, SubscriptionSummary]](Left(ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error")))
        )

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequest)

      status(result)        mustBe HttpStatus.INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.toJson(APIErrors.InternalServerError)
    }

    "return APIErrors.BadRequest when appropriate status code is received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionSummary]](Left(ErrorResponse(HttpStatus.BAD_REQUEST, "bad request mate"))))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequest)

      status(result)        mustBe HttpStatus.BAD_REQUEST
      contentAsJson(result) mustBe Json.toJson(APIErrors.BadRequest)
    }

    "return APIErrors.NotFound when appropriate status code is received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionSummary]](Left(ErrorResponse(HttpStatus.NOT_FOUND, "?"))))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequest)

      status(result)        mustBe HttpStatus.NOT_FOUND
      contentAsJson(result) mustBe Json.toJson(APIErrors.VpdIdNotFound)
    }

    "returns APIErrors.UnprocessableEntity when appropirate status code is received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(
          Future[Either[ErrorResponse, SubscriptionSummary]](Left(ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "unprocessable entity")))
        )

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequest)

      status(result)        mustBe HttpStatus.UNPROCESSABLE_ENTITY
      contentAsJson(result) mustBe Json.toJson(APIErrors.UnprocessableEntity)
    }

    "returns APIErrors.Unauthorised when appropirate status code is received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(
          Future[Either[ErrorResponse, SubscriptionSummary]](Left(ErrorResponse(HttpStatus.UNAUTHORIZED, "no auth")))
        )

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequest)

      status(result)        mustBe HttpStatus.UNAUTHORIZED
      contentAsJson(result) mustBe Json.toJson(APIErrors.Unauthorised)
    }

    "return APIErrors.ServiceUnavailable when any other unknown statua code is received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(
          Future[Either[ErrorResponse, SubscriptionSummary]](
            Left(ErrorResponse(234234435, "random unexpected status response that we have not planned for"))
          )
        )

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequest)

      status(result)        mustBe HttpStatus.SERVICE_UNAVAILABLE
      contentAsJson(result) mustBe Json.toJson(APIErrors.ServiceUnavailable)
    }
  }
}
