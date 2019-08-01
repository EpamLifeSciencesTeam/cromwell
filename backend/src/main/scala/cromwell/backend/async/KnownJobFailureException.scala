package cromwell.backend.async

import cromwell.core.path.Path
import common.exception.ThrowableAggregation
import wom.expression.{NoIoFunctionSet, WomExpression}

import scala.concurrent.ExecutionContext
import scala.util.Try

abstract class KnownJobFailureException extends Exception {
  def stderrPath: Option[Path]
}

object KnownJobFailureException {
  def stderrExplanation(stderrPath: Option[Path])(implicit ec: ExecutionContext): Option[String] = stderrPath map { path =>
    val content = Try(path.annotatedContentAsStringWithLimit(limitBytes = 300)).recover({
      case e => s"Could not retrieve content: ${e.getMessage}"
    }).get
    s"\nCheck the content of stderr for potential additional information: ${path.pathAsString}.\n $content"
  }
}

final case class WrongReturnCode(jobTag: String, returnCode: Int, stderrPath: Option[Path], errorMessage: Option[String] = None) extends KnownJobFailureException {
  val explanation = errorMessage.getOrElse("No additional info can be provided by now")
  override def getMessage =
    s"Job $jobTag exited with return code $returnCode which has not been declared as a valid return code. $explanation. See 'continueOnReturnCode' runtime attribute for more details."
}

final case class ReturnCodeIsNotAnInt(jobTag: String, returnCode: String, stderrPath: Option[Path]) extends KnownJobFailureException {
  override def getMessage = {
    if (returnCode.isEmpty)
      s"The return code file for job $jobTag was empty."
    else
      s"Job $jobTag exited with return code $returnCode which couldn't be converted to an Integer."
  }
}

final case class StderrNonEmpty(jobTag: String, stderrLength: Long, stderrPath: Option[Path]) extends KnownJobFailureException {
  override def getMessage = s"stderr for job $jobTag has length $stderrLength and 'failOnStderr' runtime attribute was true."
}


object RuntimeAttributeValidationFailure {
  def apply(jobTag: String,
            runtimeAttributeName: String,
            runtimeAttributeValue: Option[WomExpression]): RuntimeAttributeValidationFailure = RuntimeAttributeValidationFailure(jobTag, runtimeAttributeName, runtimeAttributeValue, None)
}

final case class RuntimeAttributeValidationFailure private (jobTag: String,
                                                            runtimeAttributeName: String,
                                                            runtimeAttributeValue: Option[WomExpression],
                                                            stderrPath: Option[Path]) extends KnownJobFailureException {
  override def getMessage = s"Task $jobTag has an invalid runtime attribute $runtimeAttributeName = ${runtimeAttributeValue map { _.evaluateValue(Map.empty, NoIoFunctionSet)} getOrElse "!! NOT FOUND !!"}"
}

final case class RuntimeAttributeValidationFailures(throwables: List[RuntimeAttributeValidationFailure]) extends KnownJobFailureException with ThrowableAggregation {
  override def exceptionContext = "Runtime validation failed"
  override val stderrPath: Option[Path] = None
}

final case class JobAlreadyFailedInJobStore(jobTag: String, originalErrorMessage: String) extends KnownJobFailureException {
  override def stderrPath: Option[Path] = None
  override def getMessage = originalErrorMessage
}
