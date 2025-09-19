package dev.elide.secrets.dto.persisted

/** @author Lauri Heino <datafox> */
internal interface SecretMetadata : Named {
  val profiles: Map<String, ProfileMetadata>
}
