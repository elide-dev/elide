//! Network subsystem for Colide OS
//! 
//! Provides WiFi connectivity through multiple backends:
//! - Bridge: External device (Pi/host) via USB/Serial
//! - USB Dongle: Direct RTL8188EU/MT7601U support
//! - Native: Intel AX211 PCIe (future)

pub mod bridge;
pub mod usb;
pub mod wifi;
pub mod ieee80211;
pub mod linux_compat;
pub mod cfg80211;
pub mod mac80211;
pub mod wpa;
pub mod aes;
pub mod drivers;
pub mod firmware;

use jni::JNIEnv;
use jni::objects::{JClass, JString, JByteArray, JObject};
use jni::sys::{jboolean, jint, JNI_TRUE, JNI_FALSE};

/// WiFi backend type
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum WifiBackend {
    None,
    Bridge,
    UsbDongle,
    Native,
}

/// Global WiFi state
static mut WIFI_BACKEND: WifiBackend = WifiBackend::None;
static mut WIFI_CONNECTED: bool = false;

// =============================================================================
// Bridge Mode JNI Functions
// =============================================================================

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_initBridge(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    unsafe {
        WIFI_BACKEND = WifiBackend::Bridge;
    }
    // TODO: Initialize serial/USB CDC connection to bridge device
    bridge::init().map(|_| JNI_TRUE).unwrap_or(JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_bridgeScan<'a>(
    env: JNIEnv<'a>,
    _class: JClass,
) -> JObject<'a> {
    // TODO: Send SCAN command to bridge, parse response
    let networks = bridge::scan();
    wifi::networks_to_java_list(env, &networks)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_bridgeConnect(
    mut env: JNIEnv,
    _class: JClass,
    ssid: JString,
    password: JString,
) -> jboolean {
    let ssid: String = env.get_string(&ssid).map(|s| s.into()).unwrap_or_default();
    let password: String = env.get_string(&password).map(|s| s.into()).unwrap_or_default();
    
    if bridge::connect(&ssid, &password) {
        unsafe { WIFI_CONNECTED = true; }
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_bridgeDisconnect(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    if bridge::disconnect() {
        unsafe { WIFI_CONNECTED = false; }
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_bridgeSendFrame(
    env: JNIEnv,
    _class: JClass,
    frame: JByteArray,
) -> jboolean {
    let data = env.convert_byte_array(&frame).unwrap_or_default();
    if bridge::send_frame(&data) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_bridgeReceiveFrame<'a>(
    env: JNIEnv<'a>,
    _class: JClass,
) -> JByteArray<'a> {
    match bridge::receive_frame() {
        Some(data) => env.byte_array_from_slice(&data).unwrap(),
        None => JByteArray::default(),
    }
}

// =============================================================================
// USB Dongle Mode JNI Functions
// =============================================================================

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_initUsbDongle(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    unsafe {
        WIFI_BACKEND = WifiBackend::UsbDongle;
    }
    // TODO: Initialize USB stack and detect WiFi dongle
    usb::init().map(|_| JNI_TRUE).unwrap_or(JNI_FALSE)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_usbScan<'a>(
    env: JNIEnv<'a>,
    _class: JClass,
) -> JObject<'a> {
    let networks = usb::scan();
    wifi::networks_to_java_list(env, &networks)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_usbConnect(
    mut env: JNIEnv,
    _class: JClass,
    ssid: JString,
    password: JString,
) -> jboolean {
    let ssid: String = env.get_string(&ssid).map(|s| s.into()).unwrap_or_default();
    let password: String = env.get_string(&password).map(|s| s.into()).unwrap_or_default();
    
    if usb::connect(&ssid, &password) {
        unsafe { WIFI_CONNECTED = true; }
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_usbDisconnect(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    if usb::disconnect() {
        unsafe { WIFI_CONNECTED = false; }
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_usbSendFrame(
    env: JNIEnv,
    _class: JClass,
    frame: JByteArray,
) -> jboolean {
    let data = env.convert_byte_array(&frame).unwrap_or_default();
    if usb::send_frame(&data) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_usbReceiveFrame<'a>(
    env: JNIEnv<'a>,
    _class: JClass,
) -> JByteArray<'a> {
    match usb::receive_frame() {
        Some(data) => env.byte_array_from_slice(&data).unwrap(),
        None => JByteArray::default(),
    }
}

// =============================================================================
// Native Intel WiFi JNI Functions (Future)
// =============================================================================

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_initNative(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    unsafe {
        WIFI_BACKEND = WifiBackend::Native;
    }
    // TODO: Initialize PCIe and Intel WiFi driver
    JNI_FALSE // Not yet implemented
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_nativeScan<'a>(
    env: JNIEnv<'a>,
    _class: JClass,
) -> JObject<'a> {
    // TODO: Intel WiFi scan
    wifi::networks_to_java_list(env, &[])
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_nativeConnect(
    _env: JNIEnv,
    _class: JClass,
    _ssid: JString,
    _password: JString,
) -> jboolean {
    JNI_FALSE // Not yet implemented
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_nativeDisconnect(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    JNI_FALSE // Not yet implemented
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_nativeSendFrame(
    _env: JNIEnv,
    _class: JClass,
    _frame: JByteArray,
) -> jboolean {
    JNI_FALSE // Not yet implemented
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_nativeReceiveFrame<'a>(
    env: JNIEnv<'a>,
    _class: JClass,
) -> JByteArray<'a> {
    JByteArray::default() // Not yet implemented
}

// =============================================================================
// Utility JNI Functions
// =============================================================================

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_getIpAddress<'a>(
    env: JNIEnv<'a>,
    _class: JClass,
) -> JString<'a> {
    match wifi::get_ip_address() {
        Some(ip) => env.new_string(ip).unwrap(),
        None => JString::default(),
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_elide_colide_net_WifiManager_getSignalStrength(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    wifi::get_signal_strength()
}
