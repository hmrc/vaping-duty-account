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

package uk.gov.hmrc.vapingdutyaccount.controllers.contactPreference

import com.google.inject.Inject
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.http.HttpEntity
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.vapingdutyaccount.connectors.contactPreference.SubscriptionConnector
import uk.gov.hmrc.vapingdutyaccount.controllers.actions.{AuthorisedAction, CheckSignedInAction, CheckVpdIdAction}
import uk.gov.hmrc.vapingdutyaccount.models.UserDetails
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.{DecryptedUA, UserAnswers}
import uk.gov.hmrc.vapingdutyaccount.repositories.{UpdateFailure, UpdateSuccess, UserAnswersRepository}

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

class UserAnswersController @Inject()(
                                       cc: ControllerComponents,
                                       userAnswersRepository: UserAnswersRepository,
                                       subscriptionConnector: SubscriptionConnector,
                                       authorise: AuthorisedAction,
                                       checkVpdId: CheckVpdIdAction,
                                       checkSignedInAction: CheckSignedInAction,
                                       clock: Clock
                                      )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def createUserAnswers(): Action[JsValue] = authorise(parse.json).async { implicit request =>
    withJsonBody[UserDetails] { userDetails =>
      val vpdId = userDetails.vpdId

      checkVpdId(vpdId).invokeBlock[JsValue](
        request,
        { implicit request =>
          val subscriptionContactPreferences = subscriptionConnector.getSubscriptionContactPreferences(vpdId)
          subscriptionContactPreferences.flatMap(
            _.fold(
            err => {
              logger.warn(
                s"[UserAnswersController] [createUserAnswers] Unable to get existing contact preferences for $vpdId - status ${err.statusCode}"
              )
              Future.successful(error(err))
            },
            contactPreferences => {
              val userAnswers: UserAnswers = UserAnswers.createUserAnswers(
                userDetails = userDetails,
                contactPreferences = contactPreferences,
                clock = clock
              )
              userAnswersRepository.add(userAnswers).map(ua => Created(Json.toJson(DecryptedUA.fromUA(ua))))
            }
          )
          )
        }
      )
    }
  }

  def getUserAnswers(vpdId: String): Action[AnyContent] = (authorise andThen checkVpdId(vpdId)).async { _ =>
    userAnswersRepository.get(vpdId).map {
      case Some(ua) => Ok(Json.toJson(DecryptedUA.fromUA(ua)))
      case None     => NotFound
    }
  }

  def set(): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      withJsonBody[DecryptedUA] { decryptedUA =>
        checkVpdId(decryptedUA.vpdId).invokeBlock[JsValue](
          request,
          { _ =>
            val userAnswers = UserAnswers.fromDecryptedUA(decryptedUA)
            userAnswersRepository.set(userAnswers).map {
              case UpdateSuccess => Ok(Json.toJson(decryptedUA))
              case UpdateFailure => NotFound
            }
          }
        )
      }
    }

  def error(errorResponse: ErrorResponse): Result = Result(
    header = ResponseHeader(errorResponse.statusCode),
    body = HttpEntity.Strict(ByteString(Json.toBytes(Json.toJson(errorResponse))), Some("application/json"))
  )

  def clear(internalId: String): Action[AnyContent] = checkSignedInAction.async {
    userAnswersRepository.clearUserAnswersById(internalId).map(_ => Results.NoContent)
    
  }
}
