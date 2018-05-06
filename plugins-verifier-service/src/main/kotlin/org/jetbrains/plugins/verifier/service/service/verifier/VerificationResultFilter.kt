package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultFilter.Result.Ignore
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultFilter.Result.Send
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Utility class used to control which verifications
 * should actually be sent to the Plugins Repository.
 *
 * Some verifications may be uncertain, such as those
 * that report too many problems (>100).
 * For such verifications it is better to investigate the reasons
 * manually than simply send them to confused end users.
 *
 * This class maintains a set of [ignoredVerifications].
 * That is possible to manually [unignore][unignoreVerificationResultFor]
 * verifications so that they will be sent later on.
 */
class VerificationResultFilter {

  companion object {
    private const val TOO_MANY_PROBLEMS_THRESHOLD = 100

    private val logger = LoggerFactory.getLogger(VerificationResultFilter::class.java)
  }

  private val acceptedVerifications = hashSetOf<PluginAndTarget>()

  private val _ignoredVerifications = hashMapOf<PluginAndTarget, Result.Ignore>()

  val ignoredVerifications: Map<PluginAndTarget, Result.Ignore>
    get() = _ignoredVerifications

  /**
   * Accepts verification of a plugin against IDE specified by [pluginAndTarget].
   *
   * When the verification of the plugin against this IDE occurs next time
   * the verification result will be sent anyway.
   */
  @Synchronized
  fun unignoreVerificationResultFor(pluginAndTarget: PluginAndTarget) {
    logger.info("Unignore verification result for $pluginAndTarget")
    acceptedVerifications.add(pluginAndTarget)
    _ignoredVerifications.remove(pluginAndTarget)
  }

  /**
   * Determines whether the verification result should be sent.
   *
   * Currently, if the verification reports too many compatibility problems,
   * that is more than [TOO_MANY_PROBLEMS_THRESHOLD],
   * the result is not sent unless it has been manually accepted
   * via [unignoreVerificationResultFor].
   */
  @Synchronized
  fun shouldSendVerificationResult(verificationResult: VerificationResult,
                                   verificationEndTime: Instant): Result {
    with(verificationResult) {
      val compatibilityProblems = when (this) {
        is VerificationResult.OK -> emptySet()
        is VerificationResult.StructureWarnings -> emptySet()
        is VerificationResult.InvalidPlugin -> emptySet()
        is VerificationResult.NotFound -> emptySet()
        is VerificationResult.FailedToDownload -> emptySet()
        is VerificationResult.MissingDependencies -> compatibilityProblems
        is VerificationResult.CompatibilityProblems -> compatibilityProblems
      }

      val pluginAndTarget = PluginAndTarget(plugin as UpdateInfo, verificationTarget)

      if (compatibilityProblems.size > TOO_MANY_PROBLEMS_THRESHOLD) {
        if (pluginAndTarget in acceptedVerifications) {
          logger.info("Verification $pluginAndTarget has been accepted, though there are many compatibility problems: ${compatibilityProblems.size}")
          return Result.Send
        }
        val reason = "There are too many compatibility problems between $plugin and $verificationTarget: ${compatibilityProblems.size}"
        logger.info(reason)
        val verdict = this.toString()
        val ignore = Result.Ignore(verdict, verificationEndTime, reason)
        _ignoredVerifications[pluginAndTarget] = ignore
        return ignore
      }

      return Result.Send
    }
  }

  /**
   * Possible decisions on whether to send a verification result: either [Send] or [Ignore].
   */
  sealed class Result {

    /**
     * The verification result should be sent.
     */
    object Send : Result()

    /**
     * The verification result has been ignored by some [reason][ignoreReason].
     */
    data class Ignore(val verificationVerdict: String,
                      val verificationEndTime: Instant,
                      val ignoreReason: String) : Result()

  }

}