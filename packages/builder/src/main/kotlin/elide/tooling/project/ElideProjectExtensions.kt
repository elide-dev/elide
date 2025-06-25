package elide.tooling.project

public suspend fun ElideProject.load(): ElideConfiguredProject = load(DefaultProjectLoader)
