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

#![forbid(unsafe_op_in_unsafe_fn, unused_unsafe)]

use hickory_resolver::TokioResolver;
use hickory_resolver::config::{ResolverConfig, ResolverOpts};
use hickory_resolver::name_server::GenericConnector;
use hickory_resolver::proto::ProtoErrorKind;
use hickory_resolver::proto::runtime::TokioRuntimeProvider;
use hickory_resolver::proto::xfer::Protocol;
use hickory_resolver::ResolveErrorKind;

use java_native::jni;
use jni::JNIEnv;
use jni::objects::{JClass, JObjectArray, JString};
use jni::sys::jobjectArray;
use once_cell::sync::Lazy;
use std::net::IpAddr;
use std::sync::Mutex;
use std::time::Duration;
use tokio::runtime::Runtime;

/// Global Tokio runtime for async DNS operations.
static RUNTIME: Lazy<Runtime> = Lazy::new(|| {
  tokio::runtime::Builder::new_multi_thread()
    .enable_all()
    .build()
    .expect("Failed to create Tokio runtime for DNS")
});

/// Global DNS resolver instance.
static RESOLVER: Lazy<Mutex<Option<TokioResolver>>> = Lazy::new(|| Mutex::new(None));

/// Custom DNS servers (set via setServers).
static CUSTOM_SERVERS: Lazy<Mutex<Vec<String>>> = Lazy::new(|| Mutex::new(Vec::new()));

/// Default result order: "ipv4first", "ipv6first", or "verbatim".
static DEFAULT_RESULT_ORDER: Lazy<Mutex<String>> = Lazy::new(|| Mutex::new("verbatim".to_string()));

/// Resolver timeout in milliseconds.
static RESOLVER_TIMEOUT: Lazy<Mutex<u64>> = Lazy::new(|| Mutex::new(5000));

/// Resolver retry attempts.
static RESOLVER_TRIES: Lazy<Mutex<u8>> = Lazy::new(|| Mutex::new(4));

/// DNS error codes matching Node.js dns module.
mod error_codes {
  pub const ENODATA: &str = "ENODATA";
  pub const EFORMERR: &str = "EFORMERR";
  pub const ENOTFOUND: &str = "ENOTFOUND";
  pub const EBADNAME: &str = "EBADNAME";
  pub const ETIMEOUT: &str = "ETIMEOUT";
}

/// Convert hickory error to Node.js DNS error code.
fn resolve_error_to_code(err: &hickory_resolver::ResolveError) -> &'static str {
  match err.kind() {
    ResolveErrorKind::Proto(pr) => match pr.kind() {
      ProtoErrorKind::NoRecordsFound { .. } => error_codes::ENODATA,
      ProtoErrorKind::Timeout { .. } => error_codes::ETIMEOUT,
      _ => error_codes::EFORMERR,
    },
    _ => error_codes::ENOTFOUND,
  }
}

/// Get or create the DNS resolver.
/// Uses system resolver configuration by default (similar to libuv/Node.js).
fn get_resolver() -> TokioResolver {
  let mut resolver_guard = RESOLVER.lock().unwrap();
  if resolver_guard.is_none() {
    let custom_servers = CUSTOM_SERVERS.lock().unwrap();
    let timeout_ms = *RESOLVER_TIMEOUT.lock().unwrap();
    let tries = *RESOLVER_TRIES.lock().unwrap();

    let connector = GenericConnector::new(TokioRuntimeProvider::default());
    let mut opts = ResolverOpts::default();
    opts.timeout = Duration::from_millis(timeout_ms);
    opts.attempts = tries as usize;

    let resolver = if custom_servers.is_empty() {
      // Use system resolver configuration (reads /etc/resolv.conf on Unix)
      TokioResolver::builder(connector)
        .expect("failed to create resolver builder")
        .with_options(opts)
        .build()
    } else {
      // Build custom config from servers (for dns.setServers)
      let mut config = ResolverConfig::new();
      for server in custom_servers.iter() {
        if let Ok(addr) = server.parse::<std::net::SocketAddr>() {
          config.add_name_server(hickory_resolver::config::NameServerConfig::new(
            addr,
            Protocol::Udp,
          ));
        } else if let Ok(ip) = server.parse::<IpAddr>() {
          let addr = std::net::SocketAddr::new(ip, 53);
          config.add_name_server(hickory_resolver::config::NameServerConfig::new(
            addr,
            Protocol::Udp,
          ));
        }
      }
      TokioResolver::builder_with_config(
        config,
        GenericConnector::new(TokioRuntimeProvider::default()),
      )
      .with_options(opts)
      .build()
    };
    *resolver_guard = Some(resolver);
  }
  resolver_guard.as_ref().unwrap().clone()
}

/// Reset the resolver (called after setServers or options change).
fn reset_resolver() {
  let mut resolver_guard = RESOLVER.lock().unwrap();
  *resolver_guard = None;
}

/// Helper to create a Java String array from a Vec<String>.
fn create_string_array<'a>(env: &mut JNIEnv<'a>, strings: Vec<String>) -> jobjectArray {
  let string_class = env.find_class("java/lang/String").unwrap();
  let array = env
    .new_object_array(strings.len() as i32, &string_class, JString::default())
    .unwrap();

  for (i, s) in strings.iter().enumerate() {
    let jstr = env.new_string(s).unwrap();
    env
      .set_object_array_element(&array, i as i32, jstr)
      .unwrap();
  }

  array.into_raw()
}

/// Result wrapper that includes error code for proper Node.js error handling.
/// Format: "OK:data" for success or "ERROR_CODE:message" for failure.
fn format_result(result: Result<String, (&'static str, String)>) -> String {
  match result {
    Ok(data) => format!("OK:{}", data),
    Err((code, msg)) => format!("{}:{}", code, msg),
  }
}

/// Result wrapper for arrays that includes error code.
/// Format: "OK" prefix for success with data, or "ERROR_CODE:message" for failure.
fn format_array_result(result: Result<Vec<String>, (&'static str, String)>) -> Vec<String> {
  match result {
    Ok(data) => {
      let mut res = vec!["OK".to_string()];
      res.extend(data);
      res
    }
    Err((code, msg)) => vec![format!("{}:{}", code, msg)],
  }
}

/// Resolve A records (IPv4 addresses) for a hostname.
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolve4<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver.ipv4_lookup(&host).await {
      Ok(response) => {
        let addrs: Vec<String> = response.iter().map(|ip| ip.to_string()).collect();
        if addrs.is_empty() {
          Err((error_codes::ENODATA, format!("queryA ENODATA {}", host)))
        } else {
          Ok(addrs)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryA {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve A records with TTL information.
/// Returns array: ["OK", "address:ttl", ...] on success, or ["ERROR_CODE:message"] on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolve4WithTtl<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver.ipv4_lookup(&host).await {
      Ok(response) => {
        // Get TTL from the first record's valid_until, convert to seconds
        let ttl = response
          .valid_until()
          .duration_since(std::time::Instant::now())
          .as_secs() as u32;
        let addrs: Vec<String> = response
          .iter()
          .map(|ip| format!("{}:{}", ip, ttl))
          .collect();
        if addrs.is_empty() {
          Err((error_codes::ENODATA, format!("queryA ENODATA {}", host)))
        } else {
          Ok(addrs)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryA {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve AAAA records (IPv6 addresses) for a hostname.
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolve6<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver.ipv6_lookup(&host).await {
      Ok(response) => {
        let addrs: Vec<String> = response.iter().map(|ip| ip.to_string()).collect();
        if addrs.is_empty() {
          Err((error_codes::ENODATA, format!("queryAAAA ENODATA {}", host)))
        } else {
          Ok(addrs)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryAAAA {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve AAAA records with TTL information.
/// Returns array: ["OK", "address:ttl", ...] on success, or ["ERROR_CODE:message"] on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolve6WithTtl<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver.ipv6_lookup(&host).await {
      Ok(response) => {
        let ttl = response
          .valid_until()
          .duration_since(std::time::Instant::now())
          .as_secs() as u32;
        let addrs: Vec<String> = response
          .iter()
          .map(|ip| format!("{}:{}", ip, ttl))
          .collect();
        if addrs.is_empty() {
          Err((error_codes::ENODATA, format!("queryAAAA ENODATA {}", host)))
        } else {
          Ok(addrs)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryAAAA {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve all record types and return as typed objects.
/// Each record is formatted as "TYPE:data" where TYPE is A, AAAA, MX, TXT, etc.
/// For complex types, data is further delimited with colons.
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolveAny<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    let mut results = Vec::new();
    let mut has_any = false;

    // Get A records with TTL
    if let Ok(response) = resolver.ipv4_lookup(&host).await {
      has_any = true;
      let ttl = response
        .valid_until()
        .duration_since(std::time::Instant::now())
        .as_secs() as u32;
      for ip in response.iter() {
        results.push(format!("A:{}:{}", ip, ttl));
      }
    }

    // Get AAAA records with TTL
    if let Ok(response) = resolver.ipv6_lookup(&host).await {
      has_any = true;
      let ttl = response
        .valid_until()
        .duration_since(std::time::Instant::now())
        .as_secs() as u32;
      for ip in response.iter() {
        results.push(format!("AAAA:{}:{}", ip, ttl));
      }
    }

    // Get MX records
    if let Ok(response) = resolver.mx_lookup(&host).await {
      has_any = true;
      for mx in response.iter() {
        results.push(format!("MX:{}:{}", mx.preference(), mx.exchange()));
      }
    }

    // Get TXT records
    if let Ok(response) = resolver.txt_lookup(&host).await {
      has_any = true;
      for txt in response.iter() {
        let data = txt
          .txt_data()
          .iter()
          .map(|d| String::from_utf8_lossy(d).to_string())
          .collect::<Vec<_>>()
          .join("");
        results.push(format!("TXT:{}", data));
      }
    }

    // Get NS records
    if let Ok(response) = resolver.ns_lookup(&host).await {
      has_any = true;
      for ns in response.iter() {
        results.push(format!("NS:{}", ns.to_string().trim_end_matches('.')));
      }
    }

    // Get SOA record
    if let Ok(response) = resolver.soa_lookup(&host).await {
      has_any = true;
      if let Some(soa) = response.iter().next() {
        results.push(format!(
          "SOA:{}:{}:{}:{}:{}:{}:{}",
          soa.mname(),
          soa.rname(),
          soa.serial(),
          soa.refresh(),
          soa.retry(),
          soa.expire(),
          soa.minimum()
        ));
      }
    }

    // Get CNAME records
    if let Ok(response) = resolver
      .lookup(&host, hickory_resolver::proto::rr::RecordType::CNAME)
      .await
    {
      has_any = true;
      for r in response.iter() {
        if let Some(cname) = r.as_cname() {
          results.push(format!("CNAME:{}", cname.to_string().trim_end_matches('.')));
        }
      }
    }

    if has_any {
      Ok(results)
    } else {
      Err((
        error_codes::ENOTFOUND,
        format!("queryAny ENOTFOUND {}", host),
      ))
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve MX records.
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
/// Format: ["OK", "priority:exchange", ...]
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolveMx<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver.mx_lookup(&host).await {
      Ok(response) => {
        let records: Vec<String> = response
          .iter()
          .map(|mx| {
            format!(
              "{}:{}",
              mx.preference(),
              mx.exchange().to_string().trim_end_matches('.')
            )
          })
          .collect();
        if records.is_empty() {
          Err((error_codes::ENODATA, format!("queryMx ENODATA {}", host)))
        } else {
          Ok(records)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryMx {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve TXT records.
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolveTxt<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver.txt_lookup(&host).await {
      Ok(response) => {
        let records: Vec<String> = response
          .iter()
          .map(|txt| {
            txt
              .txt_data()
              .iter()
              .map(|data| String::from_utf8_lossy(data).to_string())
              .collect::<Vec<_>>()
              .join("")
          })
          .collect();
        if records.is_empty() {
          Err((error_codes::ENODATA, format!("queryTxt ENODATA {}", host)))
        } else {
          Ok(records)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryTxt {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve SRV records.
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
/// Format: ["OK", "priority:weight:port:name", ...]
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolveSrv<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver.srv_lookup(&host).await {
      Ok(response) => {
        let records: Vec<String> = response
          .iter()
          .map(|srv| {
            format!(
              "{}:{}:{}:{}",
              srv.priority(),
              srv.weight(),
              srv.port(),
              srv.target().to_string().trim_end_matches('.')
            )
          })
          .collect();
        if records.is_empty() {
          Err((error_codes::ENODATA, format!("querySrv ENODATA {}", host)))
        } else {
          Ok(records)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("querySrv {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve NS records.
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolveNs<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver.ns_lookup(&host).await {
      Ok(response) => {
        let records: Vec<String> = response
          .iter()
          .map(|ns| ns.to_string().trim_end_matches('.').to_string())
          .collect();
        if records.is_empty() {
          Err((error_codes::ENODATA, format!("queryNs ENODATA {}", host)))
        } else {
          Ok(records)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryNs {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve CNAME records.
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolveCname<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver
      .lookup(&host, hickory_resolver::proto::rr::RecordType::CNAME)
      .await
    {
      Ok(response) => {
        let records: Vec<String> = response
          .iter()
          .filter_map(|r| {
            r.as_cname()
              .map(|c| c.to_string().trim_end_matches('.').to_string())
          })
          .collect();
        if records.is_empty() {
          Err((error_codes::ENODATA, format!("queryCname ENODATA {}", host)))
        } else {
          Ok(records)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryCname {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve CAA records.
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
/// Format: ["OK", "critical:tag:value", ...]
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolveCaa<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver
      .lookup(&host, hickory_resolver::proto::rr::RecordType::CAA)
      .await
    {
      Ok(response) => {
        let records: Vec<String> = response
          .iter()
          .filter_map(|r| {
            r.as_caa().map(|caa| {
              let critical = if caa.issuer_critical() { 128 } else { 0 };
              let tag = caa.tag();
              let value = String::from_utf8_lossy(caa.raw_value()).to_string();
              format!("{}:{}:{}", critical, tag, value)
            })
          })
          .collect();
        if records.is_empty() {
          Err((error_codes::ENODATA, format!("queryCaa ENODATA {}", host)))
        } else {
          Ok(records)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryCaa {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve PTR records (for hostname, not reverse lookup).
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolvePtr<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver
      .lookup(&host, hickory_resolver::proto::rr::RecordType::PTR)
      .await
    {
      Ok(response) => {
        let records: Vec<String> = response
          .iter()
          .filter_map(|r| {
            r.as_ptr()
              .map(|ptr| ptr.to_string().trim_end_matches('.').to_string())
          })
          .collect();
        if records.is_empty() {
          Err((error_codes::ENODATA, format!("queryPtr ENODATA {}", host)))
        } else {
          Ok(records)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryPtr {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve NAPTR records.
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
/// Format: ["OK", "order:preference:flags:services:regexp:replacement", ...]
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolveNaptr<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver
      .lookup(&host, hickory_resolver::proto::rr::RecordType::NAPTR)
      .await
    {
      Ok(response) => {
        let records: Vec<String> = response
          .iter()
          .filter_map(|r| {
            r.as_naptr().map(|naptr| {
              format!(
                "{}:{}:{}:{}:{}:{}",
                naptr.order(),
                naptr.preference(),
                String::from_utf8_lossy(naptr.flags()),
                String::from_utf8_lossy(naptr.services()),
                String::from_utf8_lossy(naptr.regexp()),
                naptr.replacement().to_string().trim_end_matches('.')
              )
            })
          })
          .collect();
        if records.is_empty() {
          Err((error_codes::ENODATA, format!("queryNaptr ENODATA {}", host)))
        } else {
          Ok(records)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryNaptr {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve TLSA records (certificate associations).
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
/// Format: ["OK", "certUsage:selector:matchingType:dataHex", ...]
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolveTlsa<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> jobjectArray {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver
      .lookup(&host, hickory_resolver::proto::rr::RecordType::TLSA)
      .await
    {
      Ok(response) => {
        let records: Vec<String> = response
          .iter()
          .filter_map(|r| {
            r.as_tlsa().map(|tlsa| {
              let data_hex = tlsa
                .cert_data()
                .iter()
                .map(|b| format!("{:02x}", b))
                .collect::<String>();
              format!(
                "{}:{}:{}:{}",
                u8::from(tlsa.cert_usage()),
                u8::from(tlsa.selector()),
                u8::from(tlsa.matching()),
                data_hex
              )
            })
          })
          .collect();
        if records.is_empty() {
          Err((error_codes::ENODATA, format!("queryTlsa ENODATA {}", host)))
        } else {
          Ok(records)
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("queryTlsa {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Resolve SOA record.
/// Returns "OK:nsname:hostmaster:serial:refresh:retry:expire:minttl" on success,
/// or "ERROR_CODE:message" on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn resolveSoa<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  hostname: JString<'a>,
) -> JString<'a> {
  let host: String = env.get_string(&hostname).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match resolver.soa_lookup(&host).await {
      Ok(response) => {
        if let Some(soa) = response.iter().next() {
          Ok(format!(
            "{}:{}:{}:{}:{}:{}:{}",
            soa.mname().to_string().trim_end_matches('.'),
            soa.rname().to_string().trim_end_matches('.'),
            soa.serial(),
            soa.refresh(),
            soa.retry(),
            soa.expire(),
            soa.minimum()
          ))
        } else {
          Err((error_codes::ENODATA, format!("querySoa ENODATA {}", host)))
        }
      }
      Err(e) => Err((
        resolve_error_to_code(&e),
        format!("querySoa {} {}", resolve_error_to_code(&e), host),
      )),
    }
  });

  env.new_string(format_result(result)).unwrap()
}

/// Reverse DNS lookup.
/// Returns array with first element "OK" on success, or "ERROR_CODE:message" on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn reverse<'a>(mut env: JNIEnv<'a>, _class: JClass<'a>, ip: JString<'a>) -> jobjectArray {
  let ip_str: String = env.get_string(&ip).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match ip_str.parse::<IpAddr>() {
      Ok(addr) => match resolver.reverse_lookup(addr).await {
        Ok(response) => {
          let hostnames: Vec<String> = response
            .iter()
            .map(|name| name.to_string().trim_end_matches('.').to_string())
            .collect();
          if hostnames.is_empty() {
            Err((
              error_codes::ENOTFOUND,
              format!("getHostByAddr ENOTFOUND {}", ip_str),
            ))
          } else {
            Ok(hostnames)
          }
        }
        Err(e) => Err((
          resolve_error_to_code(&e),
          format!("getHostByAddr {} {}", resolve_error_to_code(&e), ip_str),
        )),
      },
      Err(_) => Err((
        error_codes::EBADNAME,
        format!("getHostByAddr EBADNAME {}", ip_str),
      )),
    }
  });

  create_string_array(&mut env, format_array_result(result))
}

/// Get configured DNS servers.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn getServers<'a>(mut env: JNIEnv<'a>, _class: JClass<'a>) -> jobjectArray {
  let servers = CUSTOM_SERVERS.lock().unwrap().clone();
  create_string_array(&mut env, servers)
}

/// Set DNS servers.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn setServers<'a>(mut env: JNIEnv<'a>, _class: JClass<'a>, servers: JObjectArray<'a>) {
  let len = env.get_array_length(&servers).unwrap();
  let mut new_servers = Vec::with_capacity(len as usize);

  for i in 0..len {
    let obj = env.get_object_array_element(&servers, i).unwrap();
    let jstr = JString::from(obj);
    let server: String = env.get_string(&jstr).unwrap().into();
    new_servers.push(server);
  }

  *CUSTOM_SERVERS.lock().unwrap() = new_servers;
  reset_resolver();
}

/// Get default result order.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn getDefaultResultOrder<'a>(env: JNIEnv<'a>, _class: JClass<'a>) -> JString<'a> {
  let order = DEFAULT_RESULT_ORDER.lock().unwrap().clone();
  env.new_string(order).unwrap()
}

/// Set default result order.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn setDefaultResultOrder<'a>(mut env: JNIEnv<'a>, _class: JClass<'a>, order: JString<'a>) {
  let order_str: String = env.get_string(&order).unwrap().into();
  *DEFAULT_RESULT_ORDER.lock().unwrap() = order_str;
}

/// Set resolver timeout in milliseconds.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn setTimeout<'a>(_env: JNIEnv<'a>, _class: JClass<'a>, timeout_ms: i64) {
  *RESOLVER_TIMEOUT.lock().unwrap() = timeout_ms as u64;
  reset_resolver();
}

/// Get resolver timeout in milliseconds.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn getTimeout<'a>(_env: JNIEnv<'a>, _class: JClass<'a>) -> i64 {
  *RESOLVER_TIMEOUT.lock().unwrap() as i64
}

/// Set resolver retry attempts.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn setTries<'a>(_env: JNIEnv<'a>, _class: JClass<'a>, tries: i32) {
  *RESOLVER_TRIES.lock().unwrap() = tries as u8;
  reset_resolver();
}

/// Get resolver retry attempts.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn getTries<'a>(_env: JNIEnv<'a>, _class: JClass<'a>) -> i32 {
  *RESOLVER_TRIES.lock().unwrap() as i32
}

/// Lookup service name for port/protocol.
/// Returns "OK:hostname:service" on success, or "ERROR_CODE:message" on failure.
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn lookupService<'a>(
  mut env: JNIEnv<'a>,
  _class: JClass<'a>,
  address: JString<'a>,
  port: i32,
) -> JString<'a> {
  let addr_str: String = env.get_string(&address).unwrap().into();
  let resolver = get_resolver();

  let result = RUNTIME.block_on(async {
    match addr_str.parse::<IpAddr>() {
      Ok(addr) => {
        match resolver.reverse_lookup(addr).await {
          Ok(response) => {
            let hostname = response
              .iter()
              .next()
              .map(|name| name.to_string().trim_end_matches('.').to_string())
              .unwrap_or_default();

            if hostname.is_empty() {
              return Err((
                error_codes::ENOTFOUND,
                format!("getnameinfo ENOTFOUND {}", addr_str),
              ));
            }

            // Get service name from port (extended list)
            let service = match port {
              7 => "echo",
              20 => "ftp-data",
              21 => "ftp",
              22 => "ssh",
              23 => "telnet",
              25 => "smtp",
              53 => "domain",
              67 => "bootps",
              68 => "bootpc",
              69 => "tftp",
              80 => "http",
              110 => "pop3",
              119 => "nntp",
              123 => "ntp",
              137 => "netbios-ns",
              138 => "netbios-dgm",
              139 => "netbios-ssn",
              143 => "imap",
              161 => "snmp",
              162 => "snmptrap",
              179 => "bgp",
              389 => "ldap",
              443 => "https",
              445 => "microsoft-ds",
              465 => "smtps",
              514 => "syslog",
              587 => "submission",
              636 => "ldaps",
              993 => "imaps",
              995 => "pop3s",
              1433 => "ms-sql-s",
              1521 => "oracle",
              3306 => "mysql",
              3389 => "ms-wbt-server",
              5432 => "postgresql",
              5672 => "amqp",
              6379 => "redis",
              8080 => "http-proxy",
              8443 => "https-alt",
              27017 => "mongodb",
              _ => "",
            };

            let service_name = if service.is_empty() {
              port.to_string()
            } else {
              service.to_string()
            };

            Ok(format!("{}:{}", hostname, service_name))
          }
          Err(e) => Err((
            resolve_error_to_code(&e),
            format!("getnameinfo {} {}", resolve_error_to_code(&e), addr_str),
          )),
        }
      }
      Err(_) => Err((
        error_codes::EBADNAME,
        format!("getnameinfo EBADNAME {}", addr_str),
      )),
    }
  });

  env.new_string(format_result(result)).unwrap()
}

/// Cancel pending DNS queries (stub - queries complete synchronously).
#[jni("elide.runtime.node.dns.NativeDNS")]
pub fn cancel<'a>(_env: JNIEnv<'a>, _class: JClass<'a>) {
  // Currently all queries are blocking, so this is a no-op.
  // In a future async implementation, this would cancel pending queries.
}

#[cfg(test)]
mod tests {
  use super::*;

  #[test]
  fn test_runtime_creation() {
    // Just verify the runtime can be accessed
    let _rt = &*RUNTIME;
  }

  #[test]
  fn test_resolver_creation() {
    let resolver = get_resolver();
    // Just verify it doesn't panic
    drop(resolver);
  }

  #[test]
  fn test_default_result_order() {
    let order = DEFAULT_RESULT_ORDER.lock().unwrap().clone();
    assert_eq!(order, "verbatim");
  }
}
