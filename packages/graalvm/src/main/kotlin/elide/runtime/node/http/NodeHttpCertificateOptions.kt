/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.node.http

import elide.runtime.http.server.CertificateSource
import elide.runtime.intrinsics.js.err.TypeError
import org.graalvm.polyglot.Value
import java.time.Instant
import kotlin.io.path.Path

internal data object NodeHttpCertificateOptions {
  private const val MEMBER_CERTIFICATE_TYPE = "kind"
  private const val TYPE_FILE = "file"
  private const val TYPE_SELF_SIGNED = "selfSigned"

  private const val MEMBER_CERT_FILE = "certFile"
  private const val MEMBER_KEY_FILE = "keyFile"
  private const val MEMBER_KEY_PASS = "keyPassphrase"

  private const val MEMBER_SUBJECT = "subject"
  private const val MEMBER_NOT_BEFORE = "notBefore"
  private const val MEMBER_NOT_AFTER = "notAfter"

  fun unwrapCertificateOptions(value: Value): CertificateSource {
    if (!value.hasMembers() || !value.hasMember(MEMBER_CERTIFICATE_TYPE))
      throw TypeError.Companion.create("Certificate options must specify a '${MEMBER_CERTIFICATE_TYPE}'")

    val kind = value.takeIf { it.hasMember(MEMBER_CERTIFICATE_TYPE) }
      ?.getMember(MEMBER_CERTIFICATE_TYPE)
      ?.asString()
      ?: throw TypeError.Companion.create("Certificate kind value must be string")

    return when (kind) {
      TYPE_FILE -> {
        val certFile = value.takeIf { it.hasMember(MEMBER_CERT_FILE) }?.getMember(MEMBER_CERT_FILE)
          ?.takeIf { it.isString }
          ?.asString()
          ?: throw TypeError.Companion.create("Certificate file path must be specified as a string")

        val keyFile = value.takeIf { it.hasMember(MEMBER_KEY_FILE) }?.getMember(MEMBER_KEY_FILE)
          ?.takeIf { it.isString }
          ?.asString()
          ?: throw TypeError.Companion.create("Key file path must be specified as a string")

        val keyPass = value.takeIf { it.hasMember(MEMBER_KEY_PASS) }?.getMember(MEMBER_KEY_PASS)
          ?.takeIf { it.isString }
          ?.asString()

        CertificateSource.File(Path(certFile), Path(keyFile), keyPass)
      }

      TYPE_SELF_SIGNED -> {
        val subject = value.takeIf { it.hasMember(MEMBER_SUBJECT) }?.getMember(MEMBER_SUBJECT)
          ?.takeIf { it.isString }
          ?.asString()

        val notBefore = value.takeIf { it.hasMember(MEMBER_NOT_BEFORE) }?.getMember(MEMBER_NOT_BEFORE)
          ?.takeIf { it.isNumber && it.fitsInLong() }
          ?.asLong()

        val notAfter = value.takeIf { it.hasMember(MEMBER_NOT_AFTER) }?.getMember(MEMBER_NOT_AFTER)
          ?.takeIf { it.isNumber && it.fitsInLong() }
          ?.asLong()

        val now = Instant.now()
        CertificateSource.SelfSigned(
          subject = subject ?: CertificateSource.SelfSigned.DEFAULT_SUBJECT,
          notBefore = now.minusSeconds(notBefore ?: CertificateSource.SelfSigned.DEFAULT_START_OFFSET_SECONDS),
          notAfter = now.plusSeconds(notAfter ?: CertificateSource.SelfSigned.DEFAULT_END_OFFSET_SECONDS),
        )
      }

      else -> throw TypeError.Companion.create("Unknown certificate kind: $kind")
    }
  }
}
