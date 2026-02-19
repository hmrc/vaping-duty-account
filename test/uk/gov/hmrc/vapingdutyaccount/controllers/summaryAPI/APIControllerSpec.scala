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

package uk.gov.hmrc.vapingdutyaccount.controllers.summaryAPI

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalactic.Prettifier
import org.scalactic.source.Position
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status as HttpStatus
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.HeaderNames
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{SubscriptionContactPreferences, SubscriptionSummary}
import uk.gov.hmrc.vapingdutyaccount.models.summaryAPI.*

import scala.concurrent.Future

class APIControllerSpec extends SpecBase with MockitoSugar {

  val mockAuthConnector: AuthConnector                 = mock[AuthConnector]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val config: AppConfig                                = mock[AppConfig]

  when(config.vpdIdPattern).thenReturn("(?:GB|XI)WK[0-9]{7}WK")

  when(config.serviceName).thenReturn("Vaping Products Duty")
  when(config.serviceId).thenReturn("VPD")
  when(config.xCorrelationId).thenReturn("X-Correlation-Id")

  when(config.vpdSummaryRESTAPIGetHref(vpdId)).thenReturn(s"/get/vpdId/${vpdId}")
  when(config.vpdSummaryRESTAPIGetMethod).thenReturn("GET")

  when(config.vpdSummaryRESTAPIGetContactPreferencesHref).thenReturn("getContactPrefs.url")
  when(config.vpdSummaryRESTAPIGetContactPreferencesMethod).thenReturn("GET")

  val controller: APIController = new APIController(
    config,
    cc,
    fakeAuthorisedAction,
    mockSubscriptionConnector
  )

  val badVpdId: String            = "bad-invalid-vpdid"

  val emailContactPreference: String = "EMAIL"

  def getExpectedAPIResponse(subscription: SubscriptionContactPreferences): JsValue = Json.parse(s"""
      |{
      |  "service" : {
      |    "name" : "${config.serviceName}",
      |    "id" : "${config.serviceId}"
      |  },
      |  "identifiers" : {
      |    "vpdId" : "$vpdId"
      |  },
      |  "contactPreference" : "${ContactMethod.resolve(subscription.paperlessPreference).toString.toUpperCase}",
      |  "links" : {
      |    "self" : {
      |      "href" : "${config.vpdSummaryRESTAPIGetHref(vpdId)}",
      |      "method" : "${config.vpdSummaryRESTAPIGetMethod}"
      |    },
      |    "manage-contact-preference" : {
      |      "href" : "${config.vpdSummaryRESTAPIGetContactPreferencesHref}",
      |      "method" : "${config.vpdSummaryRESTAPIGetContactPreferencesMethod}"
      |    }
      |  }
      |}
      |""".stripMargin)

  /** This helper method will assert that the `xRequestId` header is present on received responses in accordance with the API specification.
    */
  private def assertResponseHeadersArePresent(response: Future[Result]) = {
    assert(headers(response).get(HeaderNames.xRequestId).get.isInstanceOf[String])
  }

  "SummaryAPI must " - {
    "return data in expected shape when calling the API (PaperlessPreference is true)" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionContactPreferences]](Right(contactPreferencesEmailSelected)))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequest)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesEmailSelected)
      assertResponseHeadersArePresent(result)
    }

    "return data in expected shape when calling the API (PaperlessPreference is false)" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionContactPreferences]](Right(contactPreferencesPostNoEmail)))

      val result: Future[Result] = controller.getVpdSummary(vpdId)(fakeRequest)

      println(headers(result).get(HeaderNames.xRequestId).get)

      status(result)        mustBe HttpStatus.OK
      contentAsJson(result) mustBe getExpectedAPIResponse(contactPreferencesPostNoEmail)
      assertResponseHeadersArePresent(result)
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
