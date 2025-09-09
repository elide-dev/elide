/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.punycode

// Minimal RFC 3492 Punycode encoder/decoder adapted for our use (ASCII only; no Unicode table).
// Note: For domain labels, use IDN higher-level functions in NodePunycode where appropriate.

internal object PunycodeAlgo {
  private const val BASE = 36
  private const val TMIN = 1
  private const val TMAX = 26
  private const val SKEW = 38
  private const val DAMP = 700
  private const val INITIAL_BIAS = 72
  private const val INITIAL_N = 128
  private const val DELIMITER = '-' // 0x2D

  private fun adapt(delta: Int, numPoints: Int, firstTime: Boolean): Int {
    var d = if (firstTime) delta / DAMP else delta / 2
    d += d / numPoints
    var k = 0
    while (d > ((BASE - TMIN) * TMAX) / 2) {
      d /= BASE - TMIN
      k += BASE
    }
    return k + ((BASE - TMIN + 1) * d) / (d + SKEW)
  }

  private fun digitToBasic(d: Int): Char = (if (d < 26) 'a'.code + d else '0'.code + (d - 26)).toChar()
  private fun basicToDigit(c: Int): Int = when {
    c in '0'.code..'9'.code -> c - '0'.code + 26
    c in 'a'.code..'z'.code -> c - 'a'.code
    c in 'A'.code..'Z'.code -> c - 'A'.code
    else -> BASE
  }

  fun encode(input: String): String {
    val codePoints = input.codePoints().toArray()
    var n = INITIAL_N
    var delta = 0
    var bias = INITIAL_BIAS
    val basic = StringBuilder()
    var handled = 0

    for (cp in codePoints) {
      if (cp < 0x80) {
        basic.append(cp.toChar())
        handled++
      }
    }

    val output = StringBuilder(basic)
    val basicLength = basic.length
    if (basicLength > 0) output.append(DELIMITER)

    while (handled < codePoints.size) {
      var m = Int.MAX_VALUE
      for (cp in codePoints) if (cp >= n && cp < m) m = cp
      val inc = m - n
      if (inc > (Int.MAX_VALUE - delta) / (handled + 1)) throw ArithmeticException("overflow")
      delta += inc * (handled + 1)
      n = m
      for (cp in codePoints) {
        if (cp < n) {
          delta++
          if (delta == 0) throw ArithmeticException("overflow")
        }
        if (cp == n) {
          var q = delta
          var k = BASE
          while (true) {
            val t = when {
              k <= bias -> TMIN
              k >= bias + TMAX -> TMAX
              else -> k - bias
            }
            if (q < t) break
            val code = t + ((q - t) % (BASE - t))
            output.append(digitToBasic(code))
            q = (q - t) / (BASE - t)
            k += BASE
          }
          output.append(digitToBasic(q))
          bias = adapt(delta, handled + 1, handled == basicLength)
          delta = 0
          handled++
        }
      }
      delta++
      n++
    }

    return output.toString()
  }

  fun decode(input: String): String {
    val n = intArrayOf(INITIAL_N)
    var bias = INITIAL_BIAS
    var i = 0
    val out = ArrayList<Int>()
    val d = input.lastIndexOf(DELIMITER)
    val b = if (d >= 0) d else 0
    if (d >= 0) for (j in 0 until d) out.add(input[j].code)
    var index = if (d >= 0) d + 1 else 0
    while (index < input.length) {
      var oldi = i
      var w = 1
      var k = BASE
      while (true) {
        if (index >= input.length) throw IllegalArgumentException("bad input")
        val digit = basicToDigit(input[index++].code)
        if (digit >= BASE) throw IllegalArgumentException("bad input")
        if (digit > (Int.MAX_VALUE - i) / w) throw ArithmeticException("overflow")
        i += digit * w
        val t = when {
          k <= bias -> TMIN
          k >= bias + TMAX -> TMAX
          else -> k - bias
        }
        if (digit < t) break
        if (w > Int.MAX_VALUE / (BASE - t)) throw ArithmeticException("overflow")
        w *= (BASE - t)
        k += BASE
      }
      val outLen = out.size + 1
      bias = adapt(i - oldi, outLen, oldi == 0)
      val cp = if (i / outLen > Int.MAX_VALUE - n[0]) throw ArithmeticException("overflow") else n[0] + (i / outLen)
      i %= outLen
      out.add(i, cp)
      i++
    }
    val sb = StringBuilder()
    for (cp in out) sb.append(cp.toChar())
    return sb.toString()
  }
}

