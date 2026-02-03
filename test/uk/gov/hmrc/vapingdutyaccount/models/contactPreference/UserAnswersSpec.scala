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

import play.api.libs.json.Json
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.vapingdutyaccount.base.SpecBase
import uk.gov.hmrc.vapingdutyaccount.crypto.NoCrypto

import java.time.Instant

class UserAnswersSpec extends SpecBase {
  val ua          = userAnswers.copy(validUntil = Some(Instant.now(clock).plusMillis(1)))
  val uaDecrypted = decryptedUA.copy(validUntil = Some(Instant.now(clock).plusMillis(1)))

  "UserAnswers must" - {
    "when encryption is enabled" - {
      val jsonWithEncrpytion =
        s"""{"vpdId":"$vpdId","userId":"$userId","subscriptionSummary":{"paperlessPreference":true,"emailAddress":"QuEpxLZgVPo2eQybYbl9Yxq+hGWotDBesA31u/dlBBU=","emailVerification":true,"bouncedEmail":false,"correspondenceAddress":"dL4kJMiwICXDMg/IDWlMa9sQF+RyJnds9bhhgmdV3Tdet8rzpkdptCfRf5gLxSH8","countryCode":"XYlyMMmfKtRG++9BeQ3mmQ=="},"emailAddress":"QuEpxLZgVPo2eQybYbl9Yxq+hGWotDBesA31u/dlBBU=","verifiedEmailAddresses":["QuEpxLZgVPo2eQybYbl9Yxq+hGWotDBesA31u/dlBBU="],"data":{"contactPreferenceEmail":true},"startedTime":{"$$date":{"$$numberLong":"1718118467838"}},"lastUpdated":{"$$date":{"$$numberLong":"1718118467838"}},"validUntil":{"$$date":{"$$numberLong":"1718118467839"}}}"""

      implicit val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCrypto(appConfig.cryptoKey)

      "serialise to json" in {
        Json.toJson(ua).toString() mustBe jsonWithEncrpytion
      }
      "deserialise from json" in {
        Json.parse(jsonWithEncrpytion).as[UserAnswers] mustBe ua
      }
    }

    "when encryption is disabled" - {
      val jsonWithoutEncryption =
        s"""{"vpdId":"$vpdId","userId":"$userId","subscriptionSummary":{"paperlessPreference":true,"emailAddress":"\\"john.doe@example.com\\"","emailVerification":true,"bouncedEmail":false,"correspondenceAddress":"\\"Flat 123\\\\n1 Example Road\\\\nLondon\\\\nAB1 2CD\\"","countryCode":"\\"GB\\""},"emailAddress":"\\"john.doe@example.com\\"","verifiedEmailAddresses":["\\"john.doe@example.com\\""],"data":{"contactPreferenceEmail":true},"startedTime":{"$$date":{"$$numberLong":"1718118467838"}},"lastUpdated":{"$$date":{"$$numberLong":"1718118467838"}},"validUntil":{"$$date":{"$$numberLong":"1718118467839"}}}"""

      implicit val crypto: Encrypter with Decrypter = NoCrypto

      "serialise to json" in {
        Json.toJson(ua).toString() mustBe jsonWithoutEncryption
      }
      "deserialise from json" in {
        Json.parse(jsonWithoutEncryption).as[UserAnswers] mustBe ua
      }
    }

    "create a UserAnswers from components when there is an existing verified email" in {
      val createdUserAnswers = UserAnswers.createUserAnswers(
        userDetails,
        contactPreferencesEmailSelected,
        clock
      )
      createdUserAnswers mustBe emptyUserAnswers
    }

    "create a UserAnswers from components when there is no email in the system" in {
      val createdUserAnswers = UserAnswers.createUserAnswers(
        userDetails,
        contactPreferencesPostNoEmail,
        clock
      )
      createdUserAnswers mustBe emptyUserAnswersNoEmail
    }

    "create a UserAnswers from components when the email in the system has bounced" in {
      val createdUserAnswers = UserAnswers.createUserAnswers(
        userDetails,
        contactPreferencesEmailSelected.copy(paperlessPreference = false, bouncedEmail = Some(true)),
        clock
      )
      createdUserAnswers mustBe emptyUserAnswersBouncedEmail
    }

    "convert a DecryptedUA to a UserAnswers" in {
      UserAnswers.fromDecryptedUA(decryptedUA) mustBe userAnswers
    }
  }

  "DecryptedUA must" - {
    val json =
      s"""{"vpdId":"$vpdId","userId":"$userId","subscriptionSummary":{"paperlessPreference":true,"emailAddress":"john.doe@example.com","emailVerification":true,"bouncedEmail":false,"correspondenceAddress":"Flat 123\\n1 Example Road\\nLondon\\nAB1 2CD","countryCode":"GB"},"emailAddress":"john.doe@example.com","verifiedEmailAddresses":["john.doe@example.com"],"data":{"contactPreferenceEmail":true},"startedTime":{"$$date":{"$$numberLong":"1718118467838"}},"lastUpdated":{"$$date":{"$$numberLong":"1718118467838"}},"validUntil":{"$$date":{"$$numberLong":"1718118467839"}}}"""

    "serialise to json" in {
      Json.toJson(uaDecrypted).toString() mustBe json
    }
    "deserialise from json" in {
      Json.parse(json).as[DecryptedUA] mustBe uaDecrypted
    }

    "convert a UserAnswers to a DecryptedUA" in {
      DecryptedUA.fromUA(userAnswers) mustBe decryptedUA
    }
  }
}
