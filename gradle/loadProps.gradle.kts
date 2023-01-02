file("${System.getProperty("elide.home", "..")}/gradle.properties").inputStream().use {
    java.util.Properties().apply { load(it) }.forEach { (key, value) ->
        extra.set(key as String, value as String)
    }
}
