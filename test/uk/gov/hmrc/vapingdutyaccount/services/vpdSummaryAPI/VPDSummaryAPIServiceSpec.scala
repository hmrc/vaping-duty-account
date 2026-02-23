package uk.gov.hmrc.vapingdutyaccount.services.vpdSummaryAPI

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
import play.api.test.Helpers.await
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.HeaderNames as HmrcHeaderNames
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{SubscriptionContactPreferences, SubscriptionSummary}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummaryAPI.*
import uk.gov.hmrc.vapingdutyaccount.services.vpdSummaryAPI.VPDSummaryAPIService

import scala.concurrent.{Await, Future}

class VPDSummaryAPIServiceSpec extends SpecBase with MockitoSugar {

  val mockAuthConnector: AuthConnector                 = mock[AuthConnector]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]
  val config: AppConfig                                = mock[AppConfig]

  val vpdSummaryAPIService: VPDSummaryAPIService   = new VPDSummaryAPIService(config, mockSubscriptionConnector)

  // Tests should be added to show that the VPDSummary is built with these details.
//  when(config.serviceName).thenReturn("Vaping Products Duty")
//  when(config.serviceId).thenReturn("VPD")
//  when(config.xCorrelationId).thenReturn("X-Correlation-Id")

//  when(config.vpdSummaryRESTAPIGetHref(vpdId)).thenReturn(s"/get/vpdId/${vpdId}")

//  when(config.vpdSummaryRESTAPIGetContactPreferencesHref).thenReturn("getContactPrefs.url")

  "SummaryAPI must " - {
    "return data in expected shape when calling the API (PaperlessPreference is true)" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionContactPreferences]](Right(contactPreferencesEmailSelected)))

      val result = vpdSummaryAPIService.getVPDSummary(vpdId)

      await(result).map(_.contactPreference) mustBe Right(ContactMethod.Email)
    }

    "return data in expected shape when calling the API (PaperlessPreference is false)" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionContactPreferences]](Right(contactPreferencesPostNoEmail)))

      val result = vpdSummaryAPIService.getVPDSummary(vpdId)

      await(result).map(_.contactPreference) mustBe Right(ContactMethod.Post)
    }

    "return APIErrors.InternalServerError when appropriate status code received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(
          Future[Either[ErrorResponse, SubscriptionSummary]](Left(ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error")))
        )

      val result: Future[Either[ErrorResponse, VPDSummary]] = vpdSummaryAPIService.getVPDSummary(vpdId)

      await(result) mustBe Left(ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error"))
    }

    "return APIErrors.BadRequest when appropriate status code is received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionSummary]](Left(ErrorResponse(HttpStatus.BAD_REQUEST, "bad request mate"))))

      val result: Future[Either[ErrorResponse, VPDSummary]] = vpdSummaryAPIService.getVPDSummary(vpdId)

      await(result) mustBe Left(ErrorResponse(HttpStatus.BAD_REQUEST, "bad request mate"))
    }

    "return APIErrors.NotFound when appropriate status code is received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(Future[Either[ErrorResponse, SubscriptionSummary]](Left(ErrorResponse(HttpStatus.NOT_FOUND, "?"))))

      val result: Future[Either[ErrorResponse, VPDSummary]] = vpdSummaryAPIService.getVPDSummary(vpdId)

      await(result) mustBe Left(ErrorResponse(HttpStatus.NOT_FOUND, "?"))
    }

    "returns APIErrors.UnprocessableEntity when appropirate status code is received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(
          Future[Either[ErrorResponse, SubscriptionSummary]](Left(ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "unprocessable entity")))
        )

      val result: Future[Either[ErrorResponse, VPDSummary]] = vpdSummaryAPIService.getVPDSummary(vpdId)

      await(result) mustBe Left(ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "unprocessable entity"))
    }

    "returns APIErrors.Unauthorised when appropirate status code is received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(
          Future[Either[ErrorResponse, SubscriptionSummary]](Left(ErrorResponse(HttpStatus.UNAUTHORIZED, "no auth")))
        )

      val result: Future[Either[ErrorResponse, VPDSummary]] = vpdSummaryAPIService.getVPDSummary(vpdId)

      await(result) mustBe Left(ErrorResponse(HttpStatus.UNAUTHORIZED, "no auth"))
    }

    // Should we be returning a more uniform error from the conntector rather than expose all the details of how it failed?
    // What are we going to do with all the variations? Aren't all of these just an Internal Server Error to the 
    // client making the request?
    // The controller should just respond with the various errors in the API Spec if they happen at the controller.
    // All the failures at the connector level should just be Internal Server Errors.
    "return APIErrors.ServiceUnavailable when any other unknown statua code is received from stub connector" in {
      when(mockSubscriptionConnector.getSubscriptionContactPreferences(eqTo(vpdId))(any()))
        .thenReturn(
          Future[Either[ErrorResponse, SubscriptionSummary]](
            Left(ErrorResponse(234234435, "random unexpected status response that we have not planned for"))
          )
        )

      val result: Future[Either[ErrorResponse, VPDSummary]] = vpdSummaryAPIService.getVPDSummary(vpdId)

      await(result) mustBe Left(ErrorResponse(234234435, "random unexpected status response that we have not planned for"))
    }
  }
}
