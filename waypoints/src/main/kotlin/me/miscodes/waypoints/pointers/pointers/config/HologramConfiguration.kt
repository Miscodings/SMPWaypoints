package me.miscodes.waypoints.pointers.config

import me.miscodes.configurate.Positive
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
class HologramConfiguration : RepeatingPointerConfiguration {

  override var enabled = true
    private set

  @Positive
  override var interval = 5
    private set

  @Comment(
      "Minimum horizontal distance from the player. When within this range of the waypoint, the hologram snaps to the waypoint location."
  )
  @Positive
  var minDistanceFromPlayer: Int = 8
    private set

  val minDistanceFromPlayerSquared: Int
    get() = minDistanceFromPlayer * minDistanceFromPlayer

  @Comment(
      "Maximum horizontal distance the hologram appears from the player. When farther than this, the hologram is clamped to this distance."
  )
  @Positive
  var maxDistanceFromPlayer: Int = 16
    private set

  @Comment(
      "Height of the label above the beam base (ground level). Default 3.0 puts the text just above the player's head, visible through the beam."
  )
  var hologramHeightOffset: Double = 3.0
    private set

  @Comment("Text scale when the player is close (at or within minDistanceFromPlayer).")
  var textScaleNear: Float = 1.5f
    private set

  @Comment(
      "Text scale when the player is at maxDistanceFromPlayer or beyond. Linearly interpolated between near and far as the player moves."
  )
  var textScaleFar: Float = 4.0f
    private set

  @Comment(
      "How many blocks to push the text label toward the player so it renders in front of the beam. Increase if text still appears behind the beam."
  )
  var textForwardOffset: Double = 2.0
    private set

  @Comment("Settings for the beacon beam rendered above the hologram")
  var beam = Beam()
    private set

  @ConfigSerializable
  class Beam {

    var enabled: Boolean = true
      private set

    @Comment("How tall the beam is in blocks")
    @Positive
    var height: Float = 30f
      private set

    @Comment("How wide the beam is in blocks")
    var width: Float = 0.25f
      private set

    @Comment(
        "The block used to render the beam. Stained glass variants work well for a colored, semi-transparent beam."
    )
    var block: BlockData = Material.CYAN_STAINED_GLASS.createBlockData()
      private set

    @Comment("Outer shell rendered around the inner beam for a layered beacon-beam depth effect")
    var outer = Outer()
      private set

    @ConfigSerializable
    class Outer {

      var enabled: Boolean = true
        private set

      @Comment(
          "How much extra width to add to each side of the outer shell relative to the inner beam width"
      )
      var extraWidth: Float = 0.15f
        private set

      @Comment(
          "Block for the outer shell. A more transparent block (e.g. plain glass) creates a natural glow-fade effect."
      )
      var block: BlockData = Material.GLASS.createBlockData()
        private set

      @Comment(
          "Brightness override for the outer shell (0-15). Lower values make the shell appear dimmer than the inner beam."
      )
      var brightness: Int = 10
        private set
    }
  }
}
