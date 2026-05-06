package me.miscodes.waypoints.gui.pages

import de.md5lukas.kinvs.GUIPattern
import de.md5lukas.kinvs.items.GUIItem
import me.miscodes.waypoints.api.Type
import me.miscodes.waypoints.gui.WaypointsGUI
import me.miscodes.waypoints.gui.items.ToggleGlobalsItem
import me.miscodes.waypoints.gui.items.ToggleTemporaryWaypointsItem
import net.kyori.adventure.text.Component

class SettingsPage(wpGUI: WaypointsGUI) :
    BasePage(wpGUI, wpGUI.extendApi { Type.PRIVATE.getBackgroundItem() }) {
  private companion object {
    /**
     * - g = Global waypoints toggle
     * - t = Temporary waypoints toggle
     * - b = Back
     */
    val settingsPattern =
        GUIPattern(
            "_________",
            "_________",
            "_________",
            "___g_t___",
            "________b",
        )
  }

  override val title: Component
    get() = wpGUI.translations.INVENTORY_TITLE_SETTINGS.text

  private fun updatePage(update: Boolean = true) {
    val mappings =
        mutableMapOf(
            'b' to
                GUIItem(wpGUI.translations.GENERAL_BACK.item) {
                  wpGUI.playSound { click.normal }
                  wpGUI.goBack()
                },
            'g' to
                if (
                    wpGUI.plugin.waypointsConfig.features.publicWaypoints ||
                        wpGUI.plugin.waypointsConfig.features.permissionWaypoints
                ) {
                  ToggleGlobalsItem(wpGUI)
                } else background,
            't' to ToggleTemporaryWaypointsItem(wpGUI),
        )

    applyPattern(
        settingsPattern,
        0,
        0,
        background,
        mappings,
    )

    if (update) {
      wpGUI.gui.update()
    }
  }

  fun init() {
    updatePage(false)
  }
}
