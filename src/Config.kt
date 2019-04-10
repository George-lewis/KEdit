import java.io.File
import java.lang.Exception

// Stores global configuration information
object Config {

    // Actually holds config values
    val dict = mutableMapOf<String, String>()

    init {

        // Read in config
        File("config.txt").forEachLine {

            val (key, value) = it.split(":").map { it.trim() }

            dict[key] = value

        }

    }

    /**
     *
     * Returns a specified configuration value and casts it
     * @param T The type of the config value
     * @param key The key for the config value
     * @return The casted config value, null if value is not present
     */
    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline operator fun <reified T> get(key: String): T? {

        val v = dict[key]

        return when(T::class) {

            Int::class -> v?.toInt()

            Double::class -> v?.toDouble()

            Boolean::class -> v?.toBoolean()

            String::class -> v

            else -> throw Exception("Attempted to cast configuration value to an unknown type.")

        } as T?

    }

}