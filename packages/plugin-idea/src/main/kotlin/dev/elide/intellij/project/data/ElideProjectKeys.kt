package dev.elide.intellij.project.data

import com.intellij.openapi.externalSystem.model.Key

object ElideProjectKeys {
  val ELIDE_DATA: Key<ElideProjectData> = Key.create(ElideProjectData::class.java, 100)
}
