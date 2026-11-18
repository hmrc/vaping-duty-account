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

package uk.gov.hmrc.vapingdutyaccount.models.contactPreference

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class DecryptedUA(vpdId: String,
                       internalId: String,
                       subscriptionSummary: DecryptedSubscriptionSummary,
                       emailAddress: Option[String],
                       data: JsObject = Json.obj(),
                       startedTime: Instant,
                       lastUpdated: Instant,
                       validUntil: Option[Instant] = None)

object DecryptedUA {
  def fromUA(userAnswers: UserAnswers): DecryptedUA =
    DecryptedUA(
      vpdId = userAnswers.vpdId,
      internalId = userAnswers.internalId,
      subscriptionSummary = DecryptedSubscriptionSummary(
        userAnswers.subscriptionSummary.paperlessPreference,
        userAnswers.subscriptionSummary.emailAddress.map(_.decryptedValue),
        userAnswers.subscriptionSummary.emailVerification,
        userAnswers.subscriptionSummary.bouncedEmail,
        userAnswers.subscriptionSummary.correspondenceAddress.decryptedValue
      ),
      emailAddress = userAnswers.emailAddress.map(_.decryptedValue),
      data = userAnswers.data,
      startedTime = userAnswers.startedTime,
      lastUpdated = userAnswers.lastUpdated,
      validUntil = userAnswers.validUntil
    )

  given OFormat[DecryptedUA] = (
    (__ \ "vpdId").format[String] and
      (__ \ "internalId").format[String] and
      (__ \ "subscriptionSummary").format[DecryptedSubscriptionSummary] and
      (__ \ "emailAddress").formatNullable[String] and
      (__ \ "data").formatWithDefault[JsObject](Json.obj()) and
      (__ \ "startedTime").format(MongoJavatimeFormats.instantFormat) and
      (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat) and
      (__ \ "validUntil").formatNullable(MongoJavatimeFormats.instantFormat)
  )(DecryptedUA.apply, o => Tuple.fromProductTyped(o))

}
