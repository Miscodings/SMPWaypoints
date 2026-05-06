package me.miscodes.waypoints.lang

class BooleanTranslationOption(
    private val translationLoader: TranslationLoader,
    private val key: String,
) : AbstractTranslation {

  init {
    translationLoader.registerTranslationWrapper(this)
  }

  fun value(): Boolean = translationLoader.getBoolean(key)

  override fun reset() {}

  override fun getKeys(): Array<String> = arrayOf(key)
}
