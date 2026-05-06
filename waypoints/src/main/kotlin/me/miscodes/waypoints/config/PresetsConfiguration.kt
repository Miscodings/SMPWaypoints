package me.miscodes.waypoints.config

import java.time.Duration
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
class PresetsConfiguration {

  @Comment(
      "To ease the configuration of the plugin, some opinionated preset options are available that override the options in config.yml"
  )
  var miscellaneous = Miscellaneous()
    private set

  @ConfigSerializable
  class Miscellaneous {
    @Comment("Remove all limits on the amounts of waypoints and folders that can be created")
    var removeLimits = false
      private set

    @Comment("Disable global waypoints entirely")
    var disableGlobalWaypoints = false
      private set
  }

  @Comment(
      """
    Select only the presets you want or combine them to remove all restrictions.
  """
  )
  var teleportation = Teleportation()
    private set

  @ConfigSerializable
  class Teleportation {

    @Comment("Everyone can teleport for free")
    var everyoneCanTeleport = false
      private set

    @Comment("Everyone can teleport to everywhere")
    var everyoneCanTeleportToEverywhere = false
      private set

    @Comment("All teleport cooldowns are disabled")
    var noTeleportCooldown = false
      private set
  }

  fun applyToConfiguration(config: WaypointsConfiguration) {
    if (miscellaneous.removeLimits) {
      fun clearLimits(limits: WaypointsConfiguration.Limits.Limits0) {
        limits.limit = 0
        limits.permissionLimits = emptySet()
        limits.public.limit = 0
        limits.public.permissionLimits = emptySet()
      }
      clearLimits(config.limits.waypoints)
      clearLimits(config.limits.folders)
    }

    if (miscellaneous.disableGlobalWaypoints) {
      config.features.publicWaypoints = false
      config.features.permissionWaypoints = false
    }

    with(teleportation) {
      val teleportConfigs =
          arrayOf(
              config.teleport.private,
              config.teleport.death,
              config.teleport.public,
              config.teleport.permission,
          )

      if (everyoneCanTeleport) {
        teleportConfigs.forEach { it.paymentType = TeleportPaymentType.FREE }
      }

      if (everyoneCanTeleportToEverywhere) {
        teleportConfigs.forEach {
          it.mustVisit = false
          it.onlyLastWaypoint = false
        }
      }

      if (noTeleportCooldown) {
        teleportConfigs.forEach { it.cooldown = Duration.ZERO }
      }
    }
  }
}
