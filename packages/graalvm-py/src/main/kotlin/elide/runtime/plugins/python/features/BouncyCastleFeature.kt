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
package elide.runtime.plugins.python.features

import com.oracle.graal.python.builtins.objects.ssl.CertUtils
import com.oracle.graal.python.builtins.objects.ssl.LazyBouncyCastleProvider
import org.graalvm.nativeimage.ImageSingletons
import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.Feature.AfterRegistrationAccess
import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess
import org.graalvm.nativeimage.hosted.RuntimeReflection
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport
import java.security.Security

/**
 * # Python Feature: BouncyCastle
 *
 * Registers intrinsics and reflection configurations for BouncyCastle use via Python on Elide.
 */
@Suppress("LargeClass", "LongMethod", "TooGenericExceptionThrown", "deprecation")
public class BouncyCastleFeature : Feature {
  override fun getDescription(): String = "Registers BouncyCastle native libraries for use by GraalPython"

  // @TODO(sgammon): Remove once https://github.com/oracle/graal/issues/8795 is fixed
  override fun afterRegistration(access: AfterRegistrationAccess) {
    val support = ImageSingletons.lookup(
      RuntimeClassInitializationSupport::class.java,
    )
    support.initializeAtBuildTime("org.bouncycastle", "security provider")
    support.rerunInitialization("org.bouncycastle.jcajce.provider.drbg.DRBG\$Default", "RNG")
    support.rerunInitialization("org.bouncycastle.jcajce.provider.drbg.DRBG\$NonceAndIV", "RNG")

    // Register runtime reflection here, not in a config, so it can be easily disabled
    val reflectiveClasses = arrayOf(
      "org.bouncycastle.jcajce.provider.asymmetric.COMPOSITE\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.DH\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.DSA\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.DSTU4145\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.EC\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.ECGOST\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.EdEC\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.ElGamal\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.GM\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.GOST\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.IES\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.RSA\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.X509\$Mappings",
      "org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi\$ECDSA",
      "org.bouncycastle.jcajce.provider.asymmetric.ec.SignatureSpi\$ecDSA",
      "org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi",
      "org.bouncycastle.jcajce.provider.digest.Blake2b\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.Blake2s\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.DSTU7564\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.GOST3411\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.Haraka\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.Keccak\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.MD2\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.MD4\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.MD5\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.RIPEMD128\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.RIPEMD160\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.RIPEMD256\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.RIPEMD320\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.SHA1\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.SHA224\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.SHA256\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.SHA3\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.SHA384\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.SHA512\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.SM3\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.Skein\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.Tiger\$Mappings",
      "org.bouncycastle.jcajce.provider.digest.Whirlpool\$Mappings",
      "org.bouncycastle.jcajce.provider.drbg.DRBG\$Mappings",
      "org.bouncycastle.jcajce.provider.keystore.BC\$Mappings",
      "org.bouncycastle.jcajce.provider.keystore.BCFKS\$Mappings",
      "org.bouncycastle.jcajce.provider.keystore.PKCS12\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.AES\$AlgParams",
      "org.bouncycastle.jcajce.provider.symmetric.AES\$CBC",
      "org.bouncycastle.jcajce.provider.symmetric.AES\$ECB",
      "org.bouncycastle.jcajce.provider.symmetric.AES\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.ARC4\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.ARIA\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Blowfish\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.CAST5\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.CAST6\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Camellia\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.ChaCha\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.DES\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.DESede\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.DSTU7624\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.GOST28147\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.GOST3412_2015\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Grain128\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Grainv1\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.HC128\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.HC256\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.IDEA\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Noekeon\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.OpenSSLPBKDF\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.OpenSSLPBKDF\$PBKDF",
      "org.bouncycastle.jcajce.provider.symmetric.PBEPBKDF1\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.PBEPBKDF2\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.PBEPBKDF2\$PBKDF2withSHA256",
      "org.bouncycastle.jcajce.provider.symmetric.PBEPKCS12\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Poly1305\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.RC2\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.RC5\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.RC6\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Rijndael\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.SCRYPT\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.SEED\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.SM4\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Salsa20\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Serpent\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Shacal2\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.SipHash\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.SipHash128\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Skipjack\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.TEA\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.TLSKDF\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Threefish\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Twofish\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.VMPC\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.VMPCKSA3\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.XSalsa20\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.XTEA\$Mappings",
      "org.bouncycastle.jcajce.provider.symmetric.Zuc\$Mappings",
    )

    for (name in reflectiveClasses) {
      try {
        RuntimeReflection.register(Class.forName(name).getConstructor())
      } catch (e: NoSuchMethodException) {
        throw RuntimeException("Could not register $name constructor for reflective access!", e)
      } catch (e: SecurityException) {
        throw RuntimeException("Could not register $name constructor for reflective access!", e)
      } catch (e: ClassNotFoundException) {
        throw RuntimeException("Could not register $name constructor for reflective access!", e)
      }
    }
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess?) {
    Security.addProvider(LazyBouncyCastleProvider.initProvider())
  }
}
