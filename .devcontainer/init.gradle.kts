val pkgst = true

if (pkgst) allprojects {
  repositories {
    maven("https://maven.pkg.st/")
  }
}
