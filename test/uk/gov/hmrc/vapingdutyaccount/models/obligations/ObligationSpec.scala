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
      obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = today).isDue(today) mustBe true
    }

    "return true when status is Open and due date is in the future" in {
      obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = tomorrow).isDue(today) mustBe true
    }

    "return false when status is Open and due date is in the past" in {
      obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = yesterday).isDue(today) mustBe false
    }

    "return false when status is Fulfilled regardless of due date" in {
      obligationDetails.copy(openOrFulfilledStatus = "F", iCDueDate = tomorrow).isDue(today) mustBe false
      obligationDetails.copy(openOrFulfilledStatus = "F", iCDueDate = yesterday).isDue(today) mustBe false
    }
  }

  "isOverdue must" - {

    "return true when status is Open and due date is in the past" in {
      obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = yesterday).isOverdue(today) mustBe true
    }

    "return false when status is Open and due date is today" in {
      obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = today).isOverdue(today) mustBe false
    }

    "return false when status is Open and due date is in the future" in {
      obligationDetails.copy(openOrFulfilledStatus = "O", iCDueDate = tomorrow).isOverdue(today) mustBe false
    }

    "return false when status is Fulfilled regardless of due date" in {
      obligationDetails.copy(openOrFulfilledStatus = "F", iCDueDate = yesterday).isOverdue(today) mustBe false
      obligationDetails.copy(openOrFulfilledStatus = "F", iCDueDate = tomorrow).isOverdue(today) mustBe false
    }
  }

  "isCompleted must" - {

    "return true when status is Fulfilled" in {
      obligationDetailsCompleted.isCompleted mustBe true
    }

    "return false when status is Open" in {
      obligationDetails.isCompleted mustBe false
    }
  }
}
