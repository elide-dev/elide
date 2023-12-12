val pkgst = false

if (pkgst) allprojects {
  repositories {
    maven("https://maven.pkg.st")
  }
}
