package com.jetbrains.pluginverifier.options

import com.jetbrains.pluginverifier.options.filter.DeprecatedPluginFilter
import com.jetbrains.pluginverifier.options.filter.PluginFilter
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Set of plugins to be verified and to be ignored,
 * which is filled on the verification command parsing.
 *
 * After the data of this class is filled, [pluginsToCheck] returns
 * the actual set of plugins to be verified.
 */
data class PluginsSet(
    /**
     * All plugins scheduled for the verification.
     *
     * Some of these plugins may be excluded later by [_pluginFilters].
     *
     * The plugins listed here are not necessarily valid,
     * but it may be unknown until the verification starts.
     */
    private val scheduledPlugins: MutableList<PluginInfo> = arrayListOf(),

    /**
     * Plugin filters that determine which plugins should be verified.
     *
     * By default, the list is initialized with [DeprecatedPluginFilter].
     */
    private val _pluginFilters: MutableList<PluginFilter> = arrayListOf(DeprecatedPluginFilter())

) {

  /**
   * Map of plugins that were ignored from the verification
   * by [_pluginFilters], to values containing the ignoring reasons.
   */
  private val _ignoredPlugins: MutableMap<PluginInfo, String> = hashMapOf()

  /**
   * Evaluates the actual set of plugins to be verified
   * in the upcoming verification task.
   */
  val pluginsToCheck: List<PluginInfo>
    get() = scheduledPlugins.filter { plugin ->
      _pluginFilters.all { it.shouldVerifyPlugin(plugin) == PluginFilter.Result.Verify }
    }

  /**
   * Contains data on why the plugins were ignored
   * from the verification. Map's values hold the reasons.
   */
  val ignoredPlugins: Map<PluginInfo, String>
    get() = scheduledPlugins.asSequence().mapNotNull { plugin ->
      val ignore = _pluginFilters.asSequence()
          .map { it.shouldVerifyPlugin(plugin) }
          .filterIsInstance<PluginFilter.Result.Ignore>()
          .firstOrNull()
      if (ignore == null) {
        null
      } else {
        plugin to ignore.reason
      }
    }.toMap()

  fun addPluginFilter(pluginFilter: PluginFilter) {
    _pluginFilters.add(pluginFilter)
  }

  fun schedulePlugin(pluginInfo: PluginInfo) {
    scheduledPlugins.add(pluginInfo)
  }

  fun schedulePlugins(pluginInfos: Iterable<PluginInfo>) {
    scheduledPlugins.addAll(pluginInfos)
  }

  override fun toString(): String {
    //Invoke [pluginsToCheck] once to avoid double work.
    val plugins = pluginsToCheck
    return """
        |Plugins (${plugins.size}): [${plugins.joinToString()}]
        |Ignored : [${_ignoredPlugins.keys.joinToString()}]
    """.trimMargin()
  }

}