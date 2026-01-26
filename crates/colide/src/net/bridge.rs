//! USB/Serial Bridge Protocol for WiFi
//! 
//! Communicates with an external device (Raspberry Pi, host machine)
//! that handles actual WiFi via Linux stack, forwarding packets to Colide OS.

use super::wifi::WifiNetwork;

/// Bridge protocol commands
#[repr(u8)]
pub enum BridgeCommand {
    Scan = 0x01,
    Connect = 0x02,
    Disconnect = 0x03,
    Status = 0x04,
    SendFrame = 0x10,
    RecvFrame = 0x11,
}

/// Bridge protocol response status
#[repr(u8)]
pub enum BridgeStatus {
    Ok = 0x00,
    Error = 0x01,
    NotConnected = 0x02,
    Timeout = 0x03,
    InvalidCommand = 0x04,
}

/// Bridge connection state
static mut BRIDGE_INITIALIZED: bool = false;
static mut BRIDGE_PORT: u16 = 0; // COM port or USB CDC endpoint

/// Initialize bridge connection
/// 
/// Looks for a USB CDC device or serial port connected to bridge daemon
pub fn init() -> Result<(), &'static str> {
    // TODO: Scan for USB CDC devices or open serial port
    // For now, simulate initialization
    unsafe {
        BRIDGE_INITIALIZED = true;
        BRIDGE_PORT = 1;
    }
    Ok(())
}

/// Send command to bridge and receive response
fn send_command(cmd: BridgeCommand, payload: &[u8]) -> Result<Vec<u8>, &'static str> {
    unsafe {
        if !BRIDGE_INITIALIZED {
            return Err("Bridge not initialized");
        }
    }
    
    // Build command packet: [CMD:1][LEN:2][PAYLOAD:N]
    let mut packet = Vec::with_capacity(3 + payload.len());
    packet.push(cmd as u8);
    packet.push((payload.len() >> 8) as u8);
    packet.push((payload.len() & 0xFF) as u8);
    packet.extend_from_slice(payload);
    
    // TODO: Actually send over serial/USB CDC
    // For now, return empty response
    
    // Parse response: [STATUS:1][LEN:2][PAYLOAD:N]
    // TODO: Read from serial/USB CDC
    
    Ok(Vec::new())
}

/// Scan for WiFi networks via bridge
pub fn scan() -> Vec<WifiNetwork> {
    match send_command(BridgeCommand::Scan, &[]) {
        Ok(response) => parse_scan_response(&response),
        Err(_) => Vec::new(),
    }
}

/// Parse scan response into network list
fn parse_scan_response(data: &[u8]) -> Vec<WifiNetwork> {
    let mut networks = Vec::new();
    let mut offset = 0;
    
    while offset + 40 <= data.len() {
        // Each network entry: [SSID:32][BSSID:6][SIGNAL:1][SECURITY:1][CHANNEL:1][FREQ:2]
        let ssid_bytes = &data[offset..offset + 32];
        let ssid_len = ssid_bytes.iter().position(|&b| b == 0).unwrap_or(32);
        let ssid = String::from_utf8_lossy(&ssid_bytes[..ssid_len]).to_string();
        
        let mut bssid = [0u8; 6];
        bssid.copy_from_slice(&data[offset + 32..offset + 38]);
        
        let signal = data[offset + 38] as i8 as i32;
        let security = data[offset + 39];
        let channel = data[offset + 40] as i32;
        let frequency = ((data[offset + 41] as u16) << 8 | data[offset + 42] as u16) as i32;
        
        networks.push(WifiNetwork {
            ssid,
            bssid,
            signal,
            security,
            channel,
            frequency,
        });
        
        offset += 43;
    }
    
    networks
}

/// Connect to WiFi network via bridge
pub fn connect(ssid: &str, password: &str) -> bool {
    // Build connect payload: [SSID_LEN:1][SSID:N][PASS_LEN:1][PASS:N]
    let mut payload = Vec::new();
    payload.push(ssid.len() as u8);
    payload.extend_from_slice(ssid.as_bytes());
    payload.push(password.len() as u8);
    payload.extend_from_slice(password.as_bytes());
    
    match send_command(BridgeCommand::Connect, &payload) {
        Ok(response) => !response.is_empty() && response[0] == BridgeStatus::Ok as u8,
        Err(_) => false,
    }
}

/// Disconnect from WiFi via bridge
pub fn disconnect() -> bool {
    match send_command(BridgeCommand::Disconnect, &[]) {
        Ok(response) => !response.is_empty() && response[0] == BridgeStatus::Ok as u8,
        Err(_) => false,
    }
}

/// Send Ethernet frame via bridge
pub fn send_frame(frame: &[u8]) -> bool {
    match send_command(BridgeCommand::SendFrame, frame) {
        Ok(response) => !response.is_empty() && response[0] == BridgeStatus::Ok as u8,
        Err(_) => false,
    }
}

/// Receive Ethernet frame from bridge (non-blocking)
pub fn receive_frame() -> Option<Vec<u8>> {
    match send_command(BridgeCommand::RecvFrame, &[]) {
        Ok(response) if response.len() > 1 => Some(response),
        _ => None,
    }
}

/// Get current IP address from bridge
pub fn get_ip_address() -> Option<String> {
    match send_command(BridgeCommand::Status, &[]) {
        Ok(response) if response.len() > 4 => {
            // Parse IP from response: [STATUS:1][IP:4]
            Some(format!("{}.{}.{}.{}", 
                response[1], response[2], response[3], response[4]))
        }
        _ => None,
    }
}
