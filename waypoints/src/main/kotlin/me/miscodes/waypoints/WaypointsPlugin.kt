package me.miscodes.waypoints

import de.md5lukas.commons.paper.UUIDUtils
import de.md5lukas.commons.paper.registerEvents
import de.md5lukas.commons.time.DurationFormatter
import de.md5lukas.schedulers.Schedulers
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import me.miscodes.configurate.commonSerializers
import me.miscodes.waypoints.api.WaypointsAPI
import me.miscodes.waypoints.command.WaypointsCommand
import me.miscodes.waypoints.command.WaypointsScriptCommand
import me.miscodes.waypoints.config.InventoryConfiguration
import me.miscodes.waypoints.config.PresetsConfiguration
import me.miscodes.waypoints.config.TeleportPaymentType
import me.miscodes.waypoints.config.WaypointsConfiguration
import me.miscodes.waypoints.data.SQLiteManager
import me.miscodes.waypoints.events.ConfigReloadEvent
import me.miscodes.waypoints.events.PointerEvents
import me.miscodes.waypoints.events.WaypointsListener
import me.miscodes.waypoints.integrations.*
import me.miscodes.waypoints.lang.Translations
import me.miscodes.waypoints.lang.WorldTranslations
import me.miscodes.waypoints.lang.YmlTranslationLoader
import me.miscodes.waypoints.pointers.PointerManager
import me.miscodes.waypoints.pointers.PointerManagerHooks
import me.miscodes.waypoints.tasks.BackupDatabaseTask
import me.miscodes.waypoints.tasks.CleanDatabaseTask
import me.miscodes.waypoints.tasks.UpdateChecker
import me.miscodes.waypoints.util.APIExtensions
import me.miscodes.waypoints.util.TeleportManager
import me.miscodes.waypoints.util.toMinecraftTicks
import org.bstats.bukkit.Metrics
import org.bstats.charts.SimplePie
import org.bstats.charts.SingleLineChart
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.permissions.Permission
import org.bukkit.plugin.java.JavaPlugin
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.extensions.set
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader

class WaypointsPlugin : JavaPlugin() {

  internal lateinit var databaseManager: SQLiteManager
  lateinit var waypointsConfig: WaypointsConfiguration
    private set

  lateinit var inventoryConfig: InventoryConfiguration
    private set

  lateinit var api: WaypointsAPI
    private set

  val apiExtensions = APIExtensions(this)
  lateinit var pointerManager: PointerManager

  private lateinit var translationLoader: YmlTranslationLoader
  lateinit var translations: Translations
    private set

  lateinit var worldTranslations: WorldTranslations
    private set

  lateinit var teleportManager: TeleportManager
    private set

  lateinit var uuidUtils: UUIDUtils
    private set

  lateinit var durationFormatter: DurationFormatter
    private set

  private var vaultIntegration0: VaultIntegration? = null
  val vaultIntegration: VaultIntegration
    get() =
        vaultIntegration0
            ?: throw IllegalStateException(
                "The vault integration is configured to be used, but no vault compatible plugin is installed"
            )

  var geyserIntegration: GeyserIntegration? = null
    private set

  var dynMapIntegrationAvailable = false
    private set

  var squareMapIntegrationAvailable = false
    private set

  var pl3xMapIntegrationAvailable = false
    private set

  private var blueMapIntegrationAvailable = false

  private lateinit var metrics: Metrics

  override fun onEnable() {
    try {
      Class.forName("io.papermc.paper.configuration.Configuration")
    } catch (_: ClassNotFoundException) {
      logger.log(Level.SEVERE, "SMPWaypoints requires the Paper server implementation")
      server.pluginManager.disablePlugin(this)
      return
    }
    loadConfiguration()
    initDatabase()

    initTranslations()
    initTeleportManager()
    initCommons()
    initIntegrations()

    registerCommands()
    registerEvents()
    registerCustomizablePermissions(false)

    startMetrics()
    startBackgroundTasks()
  }

  // <editor-fold desc="onEnable Methods">
  private val configLoader = yamlConfiguration("config.yml")
  private val presetLoader = yamlConfiguration("presets.yml")

  private fun yamlConfiguration(fileName: String): YamlConfigurationLoader =
      YamlConfigurationLoader.builder()
          .path(dataPath.resolve(fileName))
          .defaultOptions { options -> options.serializers { it.registerAll(commonSerializers()) } }
          .commentsEnabled(true)
          .nodeStyle(NodeStyle.BLOCK)
          .indent(2)
          .build()

  private fun loadConfiguration() {
    val configNode = configLoader.load()

    waypointsConfig =
        configNode.get<WaypointsConfiguration>()
            ?: throw IllegalStateException("Config could not be loaded")
    configNode.set(WaypointsConfiguration::class, waypointsConfig)
    configLoader.save(configNode)

    val presetNode = presetLoader.load()
    val presetConfig =
        presetNode.get<PresetsConfiguration>()
            ?: throw IllegalStateException("Presets could not be loaded")
    presetNode.set(PresetsConfiguration::class, presetConfig)
    presetLoader.save(presetNode)

    presetConfig.applyToConfiguration(waypointsConfig)

    val inventoryFile = File(dataFolder, "inventory.yml")

    if (!inventoryFile.exists()) {
      saveResource("inventory.yml", false)
    }

    val inventoryYaml = YamlConfiguration.loadConfiguration(inventoryFile)
    inventoryYaml.setDefaults(
        YamlConfiguration.loadConfiguration(getResource("inventory.yml")!!.reader())
    )

    inventoryConfig = InventoryConfiguration(inventoryYaml)
  }

  fun reloadConfiguration() {
    // Snapshot each online player's active trackables before clearing anything
    val previousTargets =
        server.onlinePlayers.associateWith { pointerManager.getCurrentTargets(it).toList() }

    // Immediately cancel all tasks and remove all display entities so no ghost beams linger
    pointerManager.clearAll()

    loadConfiguration()
    ConfigReloadEvent(waypointsConfig).callEvent()
    registerCustomizablePermissions(true)

    // Re-enable the previously active trackables under the fresh configuration
    previousTargets.forEach { (player, trackables) ->
      trackables.forEach { pointerManager.enable(player, it) }
    }
  }

  private fun initDatabase() {
    databaseManager =
        SQLiteManager(this, waypointsConfig.database, dataPath.resolve("waypoints.db"))

    databaseManager.initDatabase()

    api = databaseManager.api

    pointerManager = PointerManager(this, PointerManagerHooks(this), waypointsConfig.pointers)
  }

  private fun initTranslations() {
    translationLoader = YmlTranslationLoader(this)

    translationLoader.loadLanguage(waypointsConfig.general.language)

    translations = Translations(translationLoader)

    worldTranslations = WorldTranslations(translationLoader)
  }

  private fun initTeleportManager() {
    teleportManager = TeleportManager(this)
  }

  private fun initCommons() {
    uuidUtils = UUIDUtils(Dispatchers.Default.asExecutor())
    initDurationFormatter()
  }

  fun initDurationFormatter() {
    with(translations) {
      durationFormatter =
          DurationFormatter(
              { timeUnit, isPlural ->
                when (timeUnit) {
                  TimeUnit.SECONDS -> if (isPlural) TEXT_DURATION_SECONDS else TEXT_DURATION_SECOND
                  TimeUnit.MINUTES -> if (isPlural) TEXT_DURATION_MINUTES else TEXT_DURATION_MINUTE
                  TimeUnit.HOURS -> if (isPlural) TEXT_DURATION_HOURS else TEXT_DURATION_HOUR
                  TimeUnit.DAYS -> if (isPlural) TEXT_DURATION_DAYS else TEXT_DURATION_DAY
                  else ->
                      throw UnsupportedOperationException("The TimeUnit $timeUnit is not supported")
                }.rawText
              },
              TEXT_DURATION_ADD_SPACES.value(),
          )
    }
  }

  private fun initIntegrations() {
    val vault = VaultIntegration(this)
    if (vault.setupEconomy()) {
      vaultIntegration0 = vault
    }

    if (waypointsConfig.integrations.geyser.enabled) {
      val geyser = GeyserIntegration(this)
      if (geyser.setupGeyser()) {
        geyserIntegration = geyser
      } else {
        slF4JLogger.warn(
            "The geyser integration is enabled in the config but geyser is not installed on this server"
        )
      }
    }

    val enabledWebMaps = WebMapIntegration.startWebMapIntegrations(this)

    dynMapIntegrationAvailable = DynMapIntegration.PLUGIN_NAME in enabledWebMaps
    squareMapIntegrationAvailable = SquareMapIntegration.PLUGIN_NAME in enabledWebMaps
    blueMapIntegrationAvailable = BlueMapIntegration.PLUGIN_NAME in enabledWebMaps
    pl3xMapIntegrationAvailable = Pl3xMapIntegration.PLUGIN_NAME in enabledWebMaps
  }

  private fun registerCommands() {
    lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
      val registrar = it.registrar()
      registrar.register(
          WaypointsCommand(this).buildCommand(),
          waypointsConfig.commandAliases.waypoints,
      )
      registrar.register(
          WaypointsScriptCommand(this).buildCommand(),
          waypointsConfig.commandAliases.waypointsScript,
      )
    }
  }

  private fun registerEvents() {
    registerEvents(WaypointsListener(this))
    registerEvents(PointerEvents(this))
  }

  private fun registerCustomizablePermissions(clearPrevious: Boolean) {
    val pm = server.pluginManager
    if (clearPrevious) {
      pm.permissions.forEach {
        val name = it.name
        if (
            name.startsWith(WaypointsPermissions.LIMIT_PREFIX_WAYPOINTS) ||
                name.startsWith(WaypointsPermissions.LIMIT_PREFIX_FOLDERS) ||
                name.startsWith(WaypointsPermissions.LIMIT_PREFIX_PUBLIC_WAYPOINTS) ||
                name.startsWith(WaypointsPermissions.LIMIT_PREFIX_PUBLIC_FOLDERS)
        ) {
          pm.removePermission(name)
        }
      }
    }

    val permissions = mutableListOf<Permission>()
    waypointsConfig.limits.waypoints.permissionLimits.mapTo(permissions) {
      Permission(WaypointsPermissions.LIMIT_PREFIX_WAYPOINTS + it)
    }
    waypointsConfig.limits.folders.permissionLimits.mapTo(permissions) {
      Permission(WaypointsPermissions.LIMIT_PREFIX_FOLDERS + it)
    }
    waypointsConfig.limits.waypoints.public.permissionLimits.mapTo(permissions) {
      Permission(WaypointsPermissions.LIMIT_PREFIX_PUBLIC_WAYPOINTS + it)
    }
    waypointsConfig.limits.folders.public.permissionLimits.mapTo(permissions) {
      Permission(WaypointsPermissions.LIMIT_PREFIX_PUBLIC_FOLDERS + it)
    }

    pm.addPermissions(permissions)
  }

  private fun startMetrics() {
    if (Environment.DEV) return
    metrics = Metrics(this, Environment.METRICS_PLUGIN_ID)

    with(api.statistics) {
      metrics.addCustomChart(SingleLineChart("total_waypoints") { totalWaypoints })
      metrics.addCustomChart(SingleLineChart("total_folders") { totalFolders })
    }
    metrics.addCustomChart(
        SimplePie("web_map") {
          when {
            dynMapIntegrationAvailable -> "DynMap"
            squareMapIntegrationAvailable -> "squaremap"
            blueMapIntegrationAvailable -> "BlueMap"
            pl3xMapIntegrationAvailable -> "Pl3xMap"
            else -> "none"
          }
        }
    )
    metrics.addCustomChart(
        SimplePie("actually_uses_vault") {
          if (vaultIntegration0 !== null) {
                waypointsConfig.teleport
                    .let { arrayOf(it.private, it.death, it.public, it.permission) }
                    .any { it.paymentType === TeleportPaymentType.VAULT }
              } else {
                false
              }
              .toString()
        }
    )
    metrics.addCustomChart(
        SimplePie("global_waypoints_enabled") {
          waypointsConfig.features.publicWaypoints.toString()
        }
    )
    metrics.addCustomChart(
        SimplePie("death_waypoints_enabled") { waypointsConfig.features.deathWaypoints.toString() }
    )
    metrics.addCustomChart(
        SimplePie("player_tracking_enabled") { waypointsConfig.playerTracking.enabled.toString() }
    )
    metrics.addCustomChart(
        SimplePie("hologram_pointer_enabled") {
          waypointsConfig.pointers.hologram.enabled.toString()
        }
    )
  }

  private fun startBackgroundTasks() {
    val scheduler = Schedulers.global(this)
    val h24 = Duration.ofDays(1).toMinecraftTicks()
    scheduler.scheduleAtFixedRateAsync(h24, h24, CleanDatabaseTask(this))

    if (!Environment.DEV && waypointsConfig.general.updateChecker) {
      val checker = UpdateChecker(this)
      checker.setTaskHandle(scheduler.scheduleAtFixedRateAsync(h24, 0, checker))
    }

    if (waypointsConfig.database.backup.enable) {
      val backupInterval = waypointsConfig.database.backup.backupInterval.toMinecraftTicks()
      scheduler.scheduleAtFixedRateAsync(backupInterval, backupInterval, BackupDatabaseTask(this))
    }
  }

  // </editor-fold>

  override fun onDisable() {
    if (this::databaseManager.isInitialized) {
      databaseManager.close()
    }
    if (this::metrics.isInitialized) {
      metrics.shutdown()
    }
  }
}
