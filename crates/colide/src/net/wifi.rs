//! WiFi Common Types and Utilities

use jni::JNIEnv;
use jni::objects::JObject;

/// WiFi network information
#[derive(Debug, Clone)]
pub struct WifiNetwork {
    pub ssid: String,
    pub bssid: [u8; 6],
    pub signal: i32,       // dBm (-30 to -90)
    pub security: u8,      // 0=Open, 1=WEP, 2=WPA, 3=WPA2, 4=WPA3
    pub channel: i32,
    pub frequency: i32,    // MHz
}

impl WifiNetwork {
    /// Create from scan result bytes
    pub fn from_beacon(beacon: &[u8]) -> Option<Self> {
        if beacon.len() < 36 {
            return None;
        }
        
        // Parse 802.11 beacon frame
        // Frame: [FC:2][Duration:2][DA:6][SA:6][BSSID:6][SeqCtrl:2][Timestamp:8][Interval:2][Cap:2][IEs...]
        
        let mut bssid = [0u8; 6];
        bssid.copy_from_slice(&beacon[16..22]);
        
        // Parse Information Elements to find SSID and other data
        let mut ssid = String::new();
        let mut channel = 0i32;
        let mut offset = 36;
        
        while offset + 2 <= beacon.len() {
            let ie_id = beacon[offset];
            let ie_len = beacon[offset + 1] as usize;
            
            if offset + 2 + ie_len > beacon.len() {
                break;
            }
            
            match ie_id {
                0 => {
                    // SSID
                    ssid = String::from_utf8_lossy(&beacon[offset + 2..offset + 2 + ie_len]).to_string();
                }
                3 => {
                    // DS Parameter Set (channel)
                    if ie_len >= 1 {
                        channel = beacon[offset + 2] as i32;
                    }
                }
                _ => {}
            }
            
            offset += 2 + ie_len;
        }
        
        Some(WifiNetwork {
            ssid,
            bssid,
            signal: 0, // Set by caller from RSSI
            security: 0, // Determined from capabilities
            channel,
            frequency: channel_to_frequency(channel),
        })
    }
}

/// Convert channel number to frequency in MHz
pub fn channel_to_frequency(channel: i32) -> i32 {
    match channel {
        1..=13 => 2407 + channel * 5,  // 2.4 GHz
        14 => 2484,                     // Japan only
        36..=64 => 5000 + channel * 5,  // 5 GHz UNII-1/2
        100..=144 => 5000 + channel * 5, // 5 GHz UNII-2C/3
        149..=165 => 5000 + channel * 5, // 5 GHz UNII-3
        _ => 0,
    }
}

/// Convert frequency to channel number
pub fn frequency_to_channel(freq: i32) -> i32 {
    match freq {
        2412..=2472 => (freq - 2407) / 5,
        2484 => 14,
        5180..=5320 => (freq - 5000) / 5,
        5500..=5720 => (freq - 5000) / 5,
        5745..=5825 => (freq - 5000) / 5,
        _ => 0,
    }
}

/// Convert network list to Java ArrayList
pub fn networks_to_java_list<'a>(env: JNIEnv<'a>, networks: &[WifiNetwork]) -> JObject<'a> {
    // Create ArrayList
    let list_class = env.find_class("java/util/ArrayList").unwrap();
    let list = env.new_object(list_class, "()V", &[]).unwrap();
    
    // Find WifiNetwork class and constructor
    let network_class = match env.find_class("elide/colide/net/WifiNetwork") {
        Ok(c) => c,
        Err(_) => return list, // Return empty list if class not found
    };
    
    for network in networks {
        // Create BSSID byte array
        let bssid = env.byte_array_from_slice(&network.bssid).unwrap();
        
        // Create SSID string
        let ssid = env.new_string(&network.ssid).unwrap();
        
        // Create WifiSecurity enum value
        let security_class = env.find_class("elide/colide/net/WifiSecurity").unwrap();
        let security_name = match network.security {
            0 => "OPEN",
            1 => "WEP",
            2 => "WPA",
            3 => "WPA2",
            4 => "WPA3",
            _ => "OPEN",
        };
        let security = env.get_static_field(
            security_class,
            security_name,
            "Lelide/colide/net/WifiSecurity;"
        ).unwrap().l().unwrap();
        
        // Create WifiNetwork object
        let net_obj = env.new_object(
            network_class,
            "(Ljava/lang/String;[BILelide/colide/net/WifiSecurity;II)V",
            &[
                (&ssid).into(),
                (&bssid).into(),
                (network.signal as i32).into(),
                (&security).into(),
                (network.channel as i32).into(),
                (network.frequency as i32).into(),
            ],
        ).unwrap();
        
        // Add to list
        env.call_method(
            &list,
            "add",
            "(Ljava/lang/Object;)Z",
            &[(&net_obj).into()],
        ).unwrap();
    }
    
    list
}

/// Current connection state
static mut CURRENT_IP: Option<String> = None;
static mut CURRENT_SIGNAL: i32 = 0;

/// Get current IP address
pub fn get_ip_address() -> Option<String> {
    unsafe { CURRENT_IP.clone() }
}

/// Get current signal strength
pub fn get_signal_strength() -> i32 {
    unsafe { CURRENT_SIGNAL }
}

/// Set IP address (called by DHCP)
pub fn set_ip_address(ip: Option<String>) {
    unsafe { CURRENT_IP = ip; }
}

/// Update signal strength
pub fn set_signal_strength(signal: i32) {
    unsafe { CURRENT_SIGNAL = signal; }
}
