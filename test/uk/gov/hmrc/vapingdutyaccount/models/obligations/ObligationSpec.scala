package uk.gov.hmrc.vapingdutyaccount.models.obligations

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.vapingdutyaccount.utils.helpers.TestData

import java.time.LocalDate

class ObligationSpec extends AnyFreeSpec with Matchers with TestData {

  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)

  "isDue must" - {

    "return true when status is Open and due date is today" in {
      openObligation.copy(iCDueDate = today).isDue(today) mustBe true
    }

    "return true when status is Open and due date is in the future" in {
      openObligation.copy(iCDueDate = tomorrow).isDue(today) mustBe true
    }

    "return false when status is Open and due date is in the past" in {
      openObligation.copy(iCDueDate = yesterday).isDue(today) mustBe false
    }

    "return false when status is Fulfilled regardless of due date" in {
      fulfilledObligation.copy(iCDueDate = tomorrow).isDue(today) mustBe false
      fulfilledObligation.copy(iCDueDate = yesterday).isDue(today) mustBe false
    }
  }

  "isOverdue must" - {

    "return true when status is Open and due date is in the past" in {
      openObligation.copy(iCDueDate = yesterday).isOverdue(today) mustBe true
    }

    "return false when status is Open and due date is today" in {
      openObligation.copy(iCDueDate = today).isOverdue(today) mustBe false
    }

    "return false when status is Open and due date is in the future" in {
      openObligation.copy(iCDueDate = tomorrow).isOverdue(today) mustBe false
    }

    "return false when status is Fulfilled regardless of due date" in {
      fulfilledObligation.copy(iCDueDate = yesterday).isOverdue(today) mustBe false
      fulfilledObligation.copy(iCDueDate = tomorrow).isOverdue(today) mustBe false
    }
  }

  "isCompleted must" - {

    "return true when status is Fulfilled" in {
      fulfilledObligation.isCompleted mustBe true
    }

    "return false when status is Open" in {
      openObligation.isCompleted mustBe false
    }
  }
}
