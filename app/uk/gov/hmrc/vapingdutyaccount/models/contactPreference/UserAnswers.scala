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
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.vapingdutyaccount.models.*

import java.time.{Clock, Instant}

case class UserAnswers(vpdId: String,
                       internalId: String,
                       subscriptionSummary: EncryptedSubscriptionSummary,
                       emailAddress: Option[SensitiveString],
                       data: JsObject = Json.obj(),
                       startedTime: Instant,
                       lastUpdated: Instant,
                       validUntil: Option[Instant] = None)

object UserAnswers {
  def createUserAnswers(
    userDetails: UserDetails,
    subscriptionSummary: SubscriptionSummary,
    clock: Clock
  ): UserAnswers = {
    val existingEmail: Option[SensitiveString] = subscriptionSummary.emailAddress.map(SensitiveString.apply)

    val correspondenceAddress: String = Seq(
      subscriptionSummary.addressLine1,
      subscriptionSummary.addressLine2,
      subscriptionSummary.addressLine3,
      subscriptionSummary.addressLine4,
      subscriptionSummary.addressLine5,
      subscriptionSummary.postCode
    ).flatten.mkString("\n")

    UserAnswers(
      vpdId = userDetails.vpdId,
      internalId = userDetails.internalId,
      subscriptionSummary = EncryptedSubscriptionSummary(
        subscriptionSummary.paperlessPreference,
        existingEmail,
        subscriptionSummary.verifiedEmail,
        subscriptionSummary.bouncedEmail,
        SensitiveString(correspondenceAddress)
      ),
      emailAddress = None,
      startedTime = Instant.now(clock),
      lastUpdated = Instant.now(clock)
    )
  }

  def fromDecryptedUA(decryptedUA: DecryptedUA): UserAnswers =
    UserAnswers(
      vpdId = decryptedUA.vpdId,
      internalId = decryptedUA.internalId,
      subscriptionSummary = EncryptedSubscriptionSummary(
        decryptedUA.subscriptionSummary.paperlessPreference,
        decryptedUA.subscriptionSummary.emailAddress.map(SensitiveString.apply),
        decryptedUA.subscriptionSummary.emailVerification,
        decryptedUA.subscriptionSummary.bouncedEmail,
        SensitiveString(decryptedUA.subscriptionSummary.correspondenceAddress)
      ),
      emailAddress = decryptedUA.emailAddress.map(SensitiveString.apply),
      data = decryptedUA.data,
      startedTime = decryptedUA.startedTime,
      lastUpdated = decryptedUA.lastUpdated,
      validUntil = decryptedUA.validUntil
    )

  implicit def format(implicit crypto: Encrypter with Decrypter): OFormat[UserAnswers] =
    (
      (__ \ "vpdId").format[String] and
        (__ \ "internalId").format[String] and
        (__ \ "subscriptionSummary").format[EncryptedSubscriptionSummary] and
        (__ \ "emailAddress").formatNullable[SensitiveString] and
        (__ \ "data").formatWithDefault[JsObject](Json.obj()) and
        (__ \ "startedTime").format(MongoJavatimeFormats.instantFormat) and
        (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat) and
        (__ \ "validUntil").formatNullable(MongoJavatimeFormats.instantFormat)
    )(UserAnswers.apply, o => Tuple.fromProductTyped(o))

  implicit def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
}
