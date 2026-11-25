package uk.gov.hmrc.vapingdutyaccount.services

import com.google.inject.Inject
import play.api.http.HttpVerbs
import uk.gov.hmrc.vapingdutyaccount.config.AppConfig
import uk.gov.hmrc.vapingdutyaccount.models.contactPreference.SubscriptionContactPreferences
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.{Access, AccessApprovalStatus, ContactMethod, ContactPreferenceStatus, ManageContactPreference}
import uk.gov.hmrc.vapingdutyaccount.models.vpdSummary.internal.ContactPreferencesAndLinks

class VPDSummaryContactPreferencesService @Inject(config: AppConfig) {

  def buildContactPreferencesAndLinks(contactPreferences: SubscriptionContactPreferences): ContactPreferencesAndLinks = {
    val approvalStatus = AccessApprovalStatus.fromSubscription(contactPreferences)
    val isInsolvent = approvalStatus == AccessApprovalStatus.Insolvent
    val access = Some(Access(approvalStatus = Some(approvalStatus)))
    val (contactMethod, contactPreferenceStatus) = buildContactPreferences(contactPreferences, isInsolvent)

    ContactPreferencesAndLinks(
      isInsolvent = isInsolvent,
      access = access,
      contactMethod = contactMethod,
      contactPreferenceStatus = contactPreferenceStatus,
      manageContactPreferencesLink = manageContactPreferencesLink(isInsolvent)
    )
  }

  private def buildContactPreferences(contactPreferences: SubscriptionContactPreferences, isInsolvent: Boolean) = {
    if (isInsolvent) (None, None)
    else {
      val cm = resolveContactMethod(contactPreferences)
      (Some(cm), resolveContactPreferenceStatus(cm, contactPreferences))
    }
  }

  private def resolveContactMethod(contactPreferences: SubscriptionContactPreferences): ContactMethod =
    if (contactPreferences.paperlessPreference) ContactMethod.Email else ContactMethod.Post

  private def resolveContactPreferenceStatus(
                                              contactMethod: ContactMethod,
                                              contactPreferences: SubscriptionContactPreferences
                                            ): Option[ContactPreferenceStatus] =
    if (contactMethod == ContactMethod.Email)
      Some(ContactPreferenceStatus(contactPreferences.bouncedEmail.getOrElse(false)))
    else
      None

  private def manageContactPreferencesLink(isInsolvent: Boolean) = {
    if (isInsolvent)
      None
    else
      Some(ManageContactPreference(config.manageContactPreferenceUrl, HttpVerbs.GET))
  }

}
