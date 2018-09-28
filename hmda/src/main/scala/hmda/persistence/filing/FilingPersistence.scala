package hmda.persistence.filing

import akka.actor.typed.{ActorSystem, Behavior, Props}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ClusterShardingSettings
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityTypeKey}
import akka.persistence.typed.scaladsl.{Effect, PersistentBehaviors}
import akka.persistence.typed.scaladsl.PersistentBehaviors.CommandHandler
import com.typesafe.config.ConfigFactory
import hmda.messages.filing.FilingCommands._
import hmda.messages.filing.FilingEvents.{FilingCreated, FilingEvent, SubmissionAdded}
import hmda.model.filing.{Filing, FilingDetails, FilingId}
import hmda.model.filing.submission.Submission

object FilingPersistence {

  final val name = "Filing"

  val ShardingTypeName = EntityTypeKey[FilingCommand](name)

  case class FilingState(filing: Filing = Filing(),
                         submissions: List[Submission] = Nil) {
    def update(event: FilingEvent): FilingState = {
      event match {
        case FilingCreated(f) =>
          if (this.filing.isEmpty) {
            FilingState(f, this.submissions)
          } else {
            this
          }
        case SubmissionAdded(submission) =>
          if (submissions.contains(submission)) {
            this
          } else {
            FilingState(this.filing, submission :: submissions)
          }
        case _ => this
      }
    }
  }

  def behavior(filingId: FilingId): Behavior[FilingCommand] =
    Behaviors.setup { ctx =>
      ctx.log.debug(s"Started Filing Persistence: s${filingId.toString}")
      PersistentBehaviors
        .receive[FilingCommand, FilingEvent, FilingState](
          persistenceId = s"$name-${filingId.toString}",
          emptyState = FilingState(),
          commandHandler = commandHandler,
          eventHandler = eventHandler
        )
        .snapshotEvery(1000)
        .withTagger(_ => Set(s"$name-${filingId.toString}"))
    }

  val commandHandler
    : CommandHandler[FilingCommand, FilingEvent, FilingState] = {
    (ctx, state, cmd) =>
      cmd match {
        case CreateFiling(filing, replyTo) =>
          Effect.persist(FilingCreated(filing)).thenRun { _ =>
            ctx.log.debug(s"Filing created: ${filing.lei}-${filing.period}")
            replyTo ! FilingCreated(filing)
          }

        case GetFiling(replyTo) =>
          if (state.filing.isEmpty) {
            replyTo ! None
          } else {
            replyTo ! Some(state.filing)
          }
          Effect.none

        case GetFilingDetails(replyTo) =>
          replyTo ! FilingDetails(state.filing, state.submissions)
          Effect.none

        case AddSubmission(submission, replyTo) =>
          Effect.persist(SubmissionAdded(submission)).thenRun { _ =>
            ctx.log.debug(s"Added submission: ${submission.toString}")
            replyTo ! submission
          }

        case GetLatestSubmission(replyTo) =>
          val maybeSubmission = state.submissions.headOption
          replyTo ! maybeSubmission
          Effect.none

        case GetSubmissions(replyTo) =>
          replyTo ! state.submissions
          Effect.none

        case _ =>
          Effect.none
      }
  }

  val eventHandler: (FilingState, FilingEvent) => FilingState = {
    case (state, evt @ SubmissionAdded(_)) => state.update(evt)
    case (state, evt @ FilingCreated(_))   => state.update(evt)
    case (state, _)                        => state
  }

  def startFilingPersistenceShard(system: ActorSystem[_],
                                  filingId: FilingId): Unit = {

    val typeKey = ShardingTypeName
    val config = ConfigFactory.load()
    val shardNumber = config.getInt("hmda.filing.shardNumber")
    val sharding = ClusterSharding(system)
    sharding.spawn(
      entityId => behavior(entityId),
      Props.empty,
      typeKey,
      ClusterShardingSettings(system),
      maxNumberOfShards = shardNumber,
      handOffStopMessage = FilingStop
    )

  }


}
