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

package uk.gov.hmrc.vapingdutyaccount.repositories

import org.mockito.Mockito.{times, verify, when}
import org.mongodb.scala.model.Filters
import org.scalatest.Assertion
import uk.gov.hmrc.vapingdutyaccount.base.ISpecBase
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.crypto.CryptoProvider
import uk.gov.hmrc.crypto.SymmetricCryptoFactory
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.UserAnswers

import java.time.Instant
import java.time.temporal.ChronoUnit

class UserAnswersRepositorySpec extends ISpecBase with DefaultPlayMongoRepositorySupport[UserAnswers] {
  private val instant = Instant.now(clock)

  private val DB_TTL_IN_SEC: Long = 100

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.dbTimeToLiveInSeconds) thenReturn DB_TTL_IN_SEC

  private val mockCryptoProvider = mock[CryptoProvider]
  when(mockCryptoProvider.getCrypto) thenReturn SymmetricCryptoFactory.aesCrypto(config.cryptoKey)

  protected override val repository: UserAnswersRepository = new UserAnswersRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    cryptoProvider = mockCryptoProvider,
    clock = clock
  )

  "CryptoProvider getCrypto must be called" in
    verify(mockCryptoProvider, times(1)).getCrypto

  "add must" - {
    "set the last updated time on the supplied user answers to `now`, and save them" in {
      val expectedAddedUserAnswers = userAnswers.copy(
        lastUpdated = instant,
        validUntil = Some(instant.plusSeconds(DB_TTL_IN_SEC))
      )

      val addedUserAnswers = repository.add(userAnswers).futureValue
      val addedRecord      = find(Filters.equal("vpdId", vpdId)).futureValue.headOption.value

      addedUserAnswers mustEqual expectedAddedUserAnswers
      verifyUserAnswerResult(addedRecord, expectedAddedUserAnswers)
    }

    "not fail (upsert) if called twice" in {
      val expectedAddedUserAnswers = userAnswers.copy(
        lastUpdated = instant,
        validUntil = Some(instant.plusSeconds(DB_TTL_IN_SEC))
      )

      repository.add(userAnswers).futureValue
      val addedUserAnswers = repository.add(userAnswers).futureValue
      val addedRecord      = find(Filters.equal("vpdId", vpdId)).futureValue.headOption.value

      addedUserAnswers mustEqual expectedAddedUserAnswers
      verifyUserAnswerResult(addedRecord, expectedAddedUserAnswers)
    }
  }

  "set must" - {
    "set the last updated time on the supplied user answers to `now`, and update them" in {
      val addedUserAnswers = repository.add(userAnswers).futureValue

      val expectedAddedUserAnswers = userAnswers.copy(
        lastUpdated = instant,
        validUntil = Some(instant.plusSeconds(DB_TTL_IN_SEC))
      )

      val updatedResult = userAnswers.copy(
        userId = "new-user-id",
        validUntil = Some(instant.plusSeconds(DB_TTL_IN_SEC))
      )

      val expectedResult = expectedAddedUserAnswers.copy(
        userId = "new-user-id"
      )

      val setResult     = repository.set(updatedResult).futureValue
      val updatedRecord = find(Filters.equal("vpdId", vpdId)).futureValue.headOption.value

      addedUserAnswers mustEqual expectedAddedUserAnswers
      setResult        mustEqual UpdateSuccess
      verifyUserAnswerResult(updatedRecord, expectedResult)
    }

    "fail to update a user answer if it wasn't previously saved" in {
      val newUserAnswers = userAnswers.copy(vpdId = "new-vpd-id")
      val setResult      = repository.set(newUserAnswers).futureValue
      setResult mustEqual UpdateFailure
    }
  }

  "get when" - {
    "there is a record for this id must" - {
      "update the lastUpdated time and get the record" in {
        insert(userAnswers.copy(validUntil = Some(instant.plusSeconds(DB_TTL_IN_SEC)))).futureValue

        val result         = repository.get(userAnswers.vpdId).futureValue
        val expectedResult = userAnswers.copy(
          lastUpdated = instant,
          validUntil = Some(instant.plusSeconds(DB_TTL_IN_SEC))
        )

        verifyUserAnswerResult(result.value, expectedResult)
      }
    }

    "there is no record for this id must" - {
      "return None" in {
        repository
          .get("VPD id that does not exist")
          .futureValue must not be defined
      }
    }
  }

  "clearUserAnswersById must" - {
    "clear down existing user answers" in {
      insert(userAnswers).futureValue
      repository.get(userAnswers.vpdId).futureValue.isEmpty          mustBe false
      repository.clearUserAnswersById(userAnswers.userId).futureValue mustBe ()
      repository.get(userAnswers.vpdId).futureValue.isEmpty          mustBe true
    }

    "not fail if user answers doesn't exist" in {
      repository.get(userAnswers.vpdId).futureValue.isEmpty          mustBe true
      repository.clearUserAnswersById(userAnswers.userId).futureValue mustBe ()
    }
  }

  def verifyUserAnswerResult(actual: UserAnswers, expected: UserAnswers): Assertion = {
    actual.vpdId                                        mustEqual expected.vpdId
    actual.userId                                        mustEqual expected.userId
    actual.subscriptionSummary                           mustEqual expected.subscriptionSummary
    actual.emailAddress                                  mustEqual expected.emailAddress
    actual.data                                          mustEqual expected.data
    actual.startedTime.truncatedTo(ChronoUnit.MILLIS)    mustEqual expected.startedTime.truncatedTo(ChronoUnit.MILLIS)
    actual.lastUpdated.truncatedTo(ChronoUnit.MILLIS)    mustEqual expected.lastUpdated.truncatedTo(ChronoUnit.MILLIS)
    actual.validUntil.get.truncatedTo(ChronoUnit.MILLIS) mustEqual expected.validUntil.get.truncatedTo(
      ChronoUnit.MILLIS
    )
  }
}
