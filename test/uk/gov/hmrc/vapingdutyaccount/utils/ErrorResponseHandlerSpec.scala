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

package uk.gov.hmrc.vapingdutyaccount.utils

import play.api.http.HttpEntity
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase

class ErrorResponseHandlerSpec extends SpecBase {
  
  private val errorHandler = new ErrorResponseHandler()
  
  private def extractBodyAsJson(result: play.api.mvc.Result) = {
    val bodyBytes = result.body.asInstanceOf[HttpEntity.Strict].data
    Json.parse(bodyBytes.utf8String)
  }
  
  "ErrorResponseHandler" - {
    "convert ErrorResponse to Result with correct status code and JSON body" - {
      "handling a 400 BAD_REQUEST error" in {
        val errorResponse = ErrorResponse(BAD_REQUEST, "Bad request")
        val result = errorHandler.toResult(errorResponse)
        
        result.header.status              mustBe BAD_REQUEST
        result.body.contentType           mustBe Some("application/json")
        extractBodyAsJson(result)         mustBe Json.toJson(errorResponse)
      }
      
      "handling a 404 NOT_FOUND error" in {
        val errorResponse = ErrorResponse(NOT_FOUND, "Not found")
        val result = errorHandler.toResult(errorResponse)
        
        result.header.status              mustBe NOT_FOUND
        result.body.contentType           mustBe Some("application/json")
        extractBodyAsJson(result)         mustBe Json.toJson(errorResponse)
      }
      
      "handling a 422 UNPROCESSABLE_ENTITY error" in {
        val errorResponse = ErrorResponse(UNPROCESSABLE_ENTITY, "Unprocessable entity")
        val result = errorHandler.toResult(errorResponse)
        
        result.header.status              mustBe UNPROCESSABLE_ENTITY
        result.body.contentType           mustBe Some("application/json")
        extractBodyAsJson(result)         mustBe Json.toJson(errorResponse)
      }

      "handling a 500 INTERNAL_SERVER_ERROR error" in {
        val errorResponse = ErrorResponse(INTERNAL_SERVER_ERROR, "Internal server error")
        val result = errorHandler.toResult(errorResponse)

        result.header.status              mustBe INTERNAL_SERVER_ERROR
        result.body.contentType           mustBe Some("application/json")
        extractBodyAsJson(result)         mustBe Json.toJson(errorResponse)
      }

      "handling a 503 SERVICE_UNAVAILABLE error" in {
        val errorResponse = ErrorResponse(SERVICE_UNAVAILABLE, "Service unavailable")
        val result = errorHandler.toResult(errorResponse)

        result.header.status              mustBe SERVICE_UNAVAILABLE
        result.body.contentType           mustBe Some("application/json")
        extractBodyAsJson(result)         mustBe Json.toJson(errorResponse)
      }
    }
  }
}
