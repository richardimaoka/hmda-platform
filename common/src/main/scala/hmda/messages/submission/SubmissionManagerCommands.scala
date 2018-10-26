package hmda.messages.submission

import akka.actor.typed.ActorRef
import hmda.model.filing.submission.{SubmissionId, SubmissionStatus}
import hmda.messages.CommonMessages.Command
import hmda.messages.submission.SubmissionEvents.SubmissionEvent
import hmda.model.filing.submission.Submission

object SubmissionManagerCommands {

  sealed trait SubmissionManagerCommand extends Command
  case class Create(submissionId: SubmissionId) extends SubmissionManagerCommand
  case class StartUpload(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class CompleteUpload(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class StartParsing(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class CompleteParsing(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class CompleteParsingWithErrors(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class StartSyntacticalValidity(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class CompleteSyntacticalValidity(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class CompleteSyntacticalValidityWithErrors(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class StartQuality(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class CompleteQuality(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class CompleteQualityWithErrors(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class StartMacro(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class CompleteMacro(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class CompleteMacroWithErrors(submissionId: SubmissionId)
      extends SubmissionManagerCommand
  case class Verify(submissionId: SubmissionId) extends SubmissionManagerCommand
  case class Sign(submissionId: SubmissionId) extends SubmissionManagerCommand
  case class Fail(submissionId: SubmissionId) extends SubmissionManagerCommand
  case class GetSubmissionStatus(replyTo: ActorRef[SubmissionStatus])
      extends SubmissionManagerCommand
  case class SubmissionManagerStop() extends SubmissionManagerCommand

  case class UpdateSubmissionStatus(submission: Submission)
      extends SubmissionManagerCommand

  case object SubmissionManagerStop extends SubmissionManagerCommand

  case class WrappedSubmissionResponse(maybeSubmission: Option[Submission])
      extends SubmissionManagerCommand
  case class WrappedSubmissionEventResponse(submissionEvent: SubmissionEvent)
      extends SubmissionManagerCommand

}