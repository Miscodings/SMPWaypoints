package me.miscodes.waypoints.pointers.variants

import de.md5lukas.schedulers.AbstractScheduler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt
import me.miscodes.waypoints.pointers.Pointer
import me.miscodes.waypoints.pointers.PointerManager
import me.miscodes.waypoints.pointers.Trackable
import me.miscodes.waypoints.pointers.util.safeRemove
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f

internal class HologramPointer(
    pointerManager: PointerManager,
    player: Player,
    scheduler: AbstractScheduler,
) : Pointer(pointerManager, player, scheduler, PointerVariant.HOLOGRAM) {

  private val config = pointerManager.configuration.hologram

  override val interval: Int
    get() = config.interval

  override val supportsMultipleTargets: Boolean
    get() = true

  override val async
    get() = true

  private val activeHolograms: MutableMap<Trackable, Hologram> = ConcurrentHashMap()

  override fun update(trackable: Trackable, translatedTarget: Location?) {
    if (translatedTarget === null) {
      hide(trackable, translatedTarget)
      return
    }

    val hologramText = trackable.getHologramText(player, translatedTarget)
    if (hologramText === null) return

    val playerLoc = player.location
    val dx = translatedTarget.x - playerLoc.x
    val dz = translatedTarget.z - playerLoc.z
    val horizDistSq = dx * dx + dz * dz

    val xzLocation: Location
    val horizDist: Float
    if (horizDistSq <= config.minDistanceFromPlayerSquared) {
      xzLocation = translatedTarget.clone()
      horizDist = sqrt(horizDistSq.toFloat())
    } else {
      horizDist = sqrt(horizDistSq.toFloat())
      val clampedDist = horizDist.coerceAtMost(config.maxDistanceFromPlayer.toFloat())
      val scale = clampedDist / horizDist
      xzLocation =
          Location(
              playerLoc.world,
              playerLoc.x + dx * scale,
              translatedTarget.y,
              playerLoc.z + dz * scale,
          )
    }

    val t =
        ((horizDist - config.minDistanceFromPlayer) /
                (config.maxDistanceFromPlayer - config.minDistanceFromPlayer).toFloat())
            .coerceIn(0f, 1f)
    val textScale = config.textScaleNear + (config.textScaleFar - config.textScaleNear) * t

    val frontOffset = config.textForwardOffset
    val textXOffset = if (horizDist > 0.001f) (-dx / horizDist) * frontOffset else 0.0
    val textZOffset = if (horizDist > 0.001f) (-dz / horizDist) * frontOffset else 0.0
    val textY = player.eyeLocation.y + config.hologramHeightOffset

    activeHolograms.compute(trackable) { _, hologram ->
      if (hologram == null) {
        Hologram(xzLocation, hologramText, textScale, textXOffset, textZOffset, textY)
      } else {
        hologram.updateState(xzLocation, hologramText, textScale, textXOffset, textZOffset, textY)
        hologram
      }
    }
  }

  override fun postUpdates() {
    scheduler.schedule { activeHolograms.values.forEach(Hologram::sync) }
  }

  override fun hide(trackable: Trackable, translatedTarget: Location?) {
    activeHolograms.remove(trackable)?.remove()
  }

  override fun immediateCleanup(trackable: Trackable, translatedTarget: Location?) {
    activeHolograms.remove(trackable)?.remove()
  }

  private inner class Hologram(
      private var xzLocation: Location,
      private var text: Component,
      private var textScale: Float,
      private var textXOffset: Double = 0.0,
      private var textZOffset: Double = 0.0,
      private var textY: Double = xzLocation.y,
  ) {

    private var textDisplay: TextDisplay? = null
    private var seeThroughDisplay: TextDisplay? = null
    private var beamDisplay: BlockDisplay? = null
    private var outerBeamDisplay: BlockDisplay? = null

    private var lastX: Double = Double.NaN
    private var lastZ: Double = Double.NaN
    private var groundY: Double = xzLocation.y

    private var lastAppliedScale: Float = -1f
    private var lastText: Component? = null
    private var entitiesShown = false

    fun updateState(
        newLocation: Location,
        newText: Component,
        newTextScale: Float,
        newTextXOffset: Double,
        newTextZOffset: Double,
        newTextY: Double,
    ) {
      xzLocation = newLocation
      text = newText
      textScale = newTextScale
      textXOffset = newTextXOffset
      textZOffset = newTextZOffset
      textY = newTextY
    }

    fun sync() {
      val textCapture = textDisplay
      val stCapture = seeThroughDisplay
      val beamCapture = beamDisplay
      val outerCapture = outerBeamDisplay

      if (
          (textCapture != null && !server.isOwnedByCurrentRegion(textCapture)) ||
              (stCapture != null && !server.isOwnedByCurrentRegion(stCapture)) ||
              (beamCapture != null && !server.isOwnedByCurrentRegion(beamCapture)) ||
              (outerCapture != null && !server.isOwnedByCurrentRegion(outerCapture))
      ) {
        textCapture?.safeRemove(pointerManager.plugin)
        stCapture?.safeRemove(pointerManager.plugin)
        beamCapture?.safeRemove(pointerManager.plugin)
        outerCapture?.safeRemove(pointerManager.plugin)
        textDisplay = null
        seeThroughDisplay = null
        beamDisplay = null
        outerBeamDisplay = null
        lastX = Double.NaN
        lastZ = Double.NaN
        lastAppliedScale = -1f
        lastText = null
        entitiesShown = false
        return
      }

      val xzMoved = xzLocation.x != lastX || xzLocation.z != lastZ
      if (xzMoved) {
        val world = xzLocation.world!!
        val chunkX = xzLocation.blockX shr 4
        val chunkZ = xzLocation.blockZ shr 4
        if (world.isChunkLoaded(chunkX, chunkZ)) {
          groundY = world.getHighestBlockAt(xzLocation.blockX, xzLocation.blockZ).y.toDouble() + 1.0
        }
        lastX = xzLocation.x
        lastZ = xzLocation.z
      }

      val textLocation =
          Location(
              xzLocation.world,
              xzLocation.x + textXOffset,
              textY,
              xzLocation.z + textZOffset,
          )
      val beamLocation = Location(xzLocation.world, xzLocation.x, groundY, xzLocation.z)

      if (
          textCapture != null &&
              textCapture.isValid &&
              stCapture?.isValid != false &&
              beamCapture?.isValid != false &&
              outerCapture?.isValid != false
      ) {
        if (text != lastText) {
          textCapture.text(text)
          stCapture?.text(text)
          lastText = text
        }

        if (textScale != lastAppliedScale) {
          val transform =
              Transformation(
                  Vector3f(0f, 0f, 0f),
                  AxisAngle4f(),
                  Vector3f(textScale),
                  AxisAngle4f(),
              )
          textCapture.transformation = transform
          stCapture?.transformation = transform
          lastAppliedScale = textScale
        }

        val textTeleport = textCapture.teleportAsync(textLocation)
        val stTeleport = stCapture?.teleportAsync(textLocation)
        val beamTeleport =
            if (xzMoved && beamCapture != null) beamCapture.teleportAsync(beamLocation) else null
        val outerTeleport =
            if (xzMoved && outerCapture != null) outerCapture.teleportAsync(beamLocation) else null

        if (!entitiesShown) {
          val futures =
              listOfNotNull(textTeleport, stTeleport, beamTeleport, outerTeleport).toTypedArray()
          val combinedFuture =
              if (futures.size == 1) futures[0] else CompletableFuture.allOf(*futures)
          combinedFuture.thenRunAsync(
              {
                if (!player.canSee(textCapture))
                    player.showEntity(pointerManager.plugin, textCapture)
                if (stCapture != null && !player.canSee(stCapture))
                    player.showEntity(pointerManager.plugin, stCapture)
                if (beamCapture != null && !player.canSee(beamCapture))
                    player.showEntity(pointerManager.plugin, beamCapture)
                if (outerCapture != null && !player.canSee(outerCapture))
                    player.showEntity(pointerManager.plugin, outerCapture)
                entitiesShown = true
              },
              syncExecutor,
          )
        }
        return
      }

      val world = xzLocation.world!!
      textCapture?.safeRemove(pointerManager.plugin)
      stCapture?.safeRemove(pointerManager.plugin)
      beamCapture?.safeRemove(pointerManager.plugin)
      outerCapture?.safeRemove(pointerManager.plugin)
      lastAppliedScale = -1f
      lastText = null
      entitiesShown = false

      fun spawnText(seeThrough: Boolean): TextDisplay =
          world
              .spawn(textLocation, TextDisplay::class.java) {
                it.isPersistent = false
                it.isVisibleByDefault = false
                it.teleportDuration = interval
                it.billboard = Display.Billboard.CENTER
                it.text(text)
                it.isDefaultBackground = false
                it.backgroundColor = Color.fromARGB(0, 0, 0, 0)
                it.isShadowed = true
                it.isSeeThrough = seeThrough
                it.viewRange = 8.0f
                it.transformation =
                    Transformation(
                        Vector3f(0f, 0f, 0f),
                        AxisAngle4f(),
                        Vector3f(textScale),
                        AxisAngle4f(),
                    )
              }
              .also { player.showEntity(pointerManager.plugin, it) }

      // Depth label: isSeeThrough=false — renders in front of beam via normal depth buffer
      textDisplay = spawnText(false).also { lastAppliedScale = textScale }
      // See-through label: isSeeThrough=true — shows through walls/blocks
      seeThroughDisplay = spawnText(true)

      if (config.beam.enabled) {
        val beamWidth = config.beam.width
        val beamHeight = config.beam.height

        if (config.beam.outer.enabled) {
          val outerWidth = beamWidth + config.beam.outer.extraWidth * 2
          val outerBrightness = config.beam.outer.brightness.coerceIn(0, 15)
          outerBeamDisplay =
              world
                  .spawn(beamLocation, BlockDisplay::class.java) {
                    it.isPersistent = false
                    it.isVisibleByDefault = false
                    it.teleportDuration = interval
                    it.block = config.beam.outer.block
                    it.brightness = Display.Brightness(outerBrightness, outerBrightness)
                    it.transformation =
                        Transformation(
                            Vector3f(-outerWidth / 2f, 0f, -outerWidth / 2f),
                            AxisAngle4f(),
                            Vector3f(outerWidth, beamHeight, outerWidth),
                            AxisAngle4f(),
                        )
                  }
                  .also { player.showEntity(pointerManager.plugin, it) }
        }

        beamDisplay =
            world
                .spawn(beamLocation, BlockDisplay::class.java) {
                  it.isPersistent = false
                  it.isVisibleByDefault = false
                  it.teleportDuration = interval
                  it.block = config.beam.block
                  it.brightness = Display.Brightness(15, 15)
                  it.transformation =
                      Transformation(
                          Vector3f(-beamWidth / 2f, 0f, -beamWidth / 2f),
                          AxisAngle4f(),
                          Vector3f(beamWidth, beamHeight, beamWidth),
                          AxisAngle4f(),
                      )
                }
                .also { player.showEntity(pointerManager.plugin, it) }
      }

      lastText = text
      entitiesShown = true
    }

    fun remove() {
      textDisplay?.safeRemove(pointerManager.plugin)
      seeThroughDisplay?.safeRemove(pointerManager.plugin)
      beamDisplay?.safeRemove(pointerManager.plugin)
      outerBeamDisplay?.safeRemove(pointerManager.plugin)
    }
  }
}
