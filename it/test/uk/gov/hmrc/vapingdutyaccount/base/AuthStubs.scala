/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.vapingdutyaccount.base

import org.scalatest.Suite
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.vapingdutyaccount.utils.helpers.WireMockHelper
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{APIErrors, APIErrorFormat}

trait AuthStubs extends WireMockHelper {
  this: Suite =>
  val authUrl            = "/auth/authorise"
  val testAuthInternalId = "internalId"

  val authRequest =
    s"""{
       |  "authorise":[
       |    [
       |      {
       |         "authProviders":["GovernmentGateway"]
       |      },
       |      {
       |         "identifiers":[],
       |         "state":"Activated",
       |         "enrolment":"HMRC-VPD-ORG"
       |      },
       |      {
       |        "credentialStrength":"strong"
       |      }
       |    ],
       |  {
       |    "affinityGroup":"Organisation"
       |  },
       |  {
       |    "confidenceLevel":50
       |  }],
       |  "retrieve":[ "internalId", "authorisedEnrolments" ]
       |}""".stripMargin

  def authOKResponse(vpdId: String) =
    s"""|  {
        |    "internalId": "$testAuthInternalId",
        |    "authorisedEnrolments" : [ {
        |      "key" : "HMRC-VPD-ORG",
        |      "identifiers" : [ {
        |        "key" : "ZVPD",
        |        "value" : "$vpdId"
        |      } ],
        |      "state" : "Activated",
        |      "confidenceLevel" : 50
        |    } ]
        |  }
         """.stripMargin

  val authNotOKResponse = Json.stringify(Json.toJson(APIErrors.Unauthorised))

  def stubAuthorised(vpdId: String): Unit =
    stubPost(authUrl, OK, authRequest, authOKResponse(vpdId))

  def stubNotAuthorised(vpdId: String): Unit = {
    stubPost(authUrl, UNAUTHORIZED, authRequest, authNotOKResponse)
  }

  def verifyAuthorised(): Unit =
    verifyPost(authUrl)
}
