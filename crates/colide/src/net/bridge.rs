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

/// USB CDC class codes
const USB_CLASS_CDC: u8 = 0x02;
const USB_SUBCLASS_ACM: u8 = 0x02;

/// Serial port base addresses (COM1-COM4)
const SERIAL_PORTS: [u16; 4] = [0x3F8, 0x2F8, 0x3E8, 0x2E8];

/// Initialize bridge connection
/// 
/// Looks for a USB CDC device or serial port connected to bridge daemon
pub fn init() -> Result<(), &'static str> {
    // First, try USB CDC devices
    if let Some(cdc_endpoint) = find_usb_cdc_device() {
        unsafe {
            BRIDGE_INITIALIZED = true;
            BRIDGE_PORT = cdc_endpoint;
        }
        return Ok(());
    }
    
    // Fall back to serial port detection
    for &port in &SERIAL_PORTS {
        if probe_serial_port(port) {
            unsafe {
                BRIDGE_INITIALIZED = true;
                BRIDGE_PORT = port;
            }
            return Ok(());
        }
    }
    
    Err("No bridge device found")
}

/// Find USB CDC ACM device
fn find_usb_cdc_device() -> Option<u16> {
    // Scan USB devices for CDC ACM class
    // Would enumerate USB devices and check class/subclass
    // Return endpoint number if found
    None
}

/// Probe serial port for bridge daemon
fn probe_serial_port(port: u16) -> bool {
    #[cfg(target_arch = "x86_64")]
    unsafe {
        // Read Line Status Register (LSR) at port+5
        let lsr: u8;
        core::arch::asm!("in al, dx", out("al") lsr, in("dx") port + 5);
        
        // Check if UART is present (bits 5-6 should be set when TX empty)
        if lsr == 0xFF || lsr == 0x00 {
            return false;
        }
        
        // Try to detect bridge by sending identification request
        // Send ENQ (0x05) and wait for ACK (0x06)
        core::arch::asm!("out dx, al", in("al") 0x05u8, in("dx") port);
        
        // Brief wait
        for _ in 0..10000 {
            core::arch::asm!("nop");
        }
        
        // Check for response
        let status: u8;
        core::arch::asm!("in al, dx", out("al") status, in("dx") port + 5);
        if status & 1 != 0 {
            let response: u8;
            core::arch::asm!("in al, dx", out("al") response, in("dx") port);
            return response == 0x06;
        }
    }
    false
}

/// Send command to bridge and receive response
fn send_command(cmd: BridgeCommand, payload: &[u8]) -> Result<Vec<u8>, &'static str> {
    let port = unsafe {
        if !BRIDGE_INITIALIZED {
            return Err("Bridge not initialized");
        }
        BRIDGE_PORT
    };
    
    // Build command packet: [CMD:1][LEN:2][PAYLOAD:N]
    let mut packet = Vec::with_capacity(3 + payload.len());
    packet.push(cmd as u8);
    packet.push((payload.len() >> 8) as u8);
    packet.push((payload.len() & 0xFF) as u8);
    packet.extend_from_slice(payload);
    
    // Send packet over serial port
    for byte in &packet {
        serial_write_byte(port, *byte)?;
    }
    
    // Read response header: [STATUS:1][LEN:2]
    let status = serial_read_byte(port)?;
    if status != BridgeStatus::Ok as u8 {
        return Err("Bridge command failed");
    }
    
    let len_hi = serial_read_byte(port)?;
    let len_lo = serial_read_byte(port)?;
    let len = ((len_hi as usize) << 8) | (len_lo as usize);
    
    // Read payload
    let mut response = Vec::with_capacity(len);
    for _ in 0..len {
        response.push(serial_read_byte(port)?);
    }
    
    Ok(response)
}

/// Write byte to serial port
fn serial_write_byte(port: u16, byte: u8) -> Result<(), &'static str> {
    #[cfg(target_arch = "x86_64")]
    unsafe {
        // Wait for TX buffer empty
        for _ in 0..100000 {
            let lsr: u8;
            core::arch::asm!("in al, dx", out("al") lsr, in("dx") port + 5);
            if lsr & 0x20 != 0 {
                // Write byte
                core::arch::asm!("out dx, al", in("al") byte, in("dx") port);
                return Ok(());
            }
        }
        return Err("Serial TX timeout");
    }
    #[cfg(not(target_arch = "x86_64"))]
    { let _ = (port, byte); Err("Not implemented") }
}

/// Read byte from serial port with timeout
fn serial_read_byte(port: u16) -> Result<u8, &'static str> {
    #[cfg(target_arch = "x86_64")]
    unsafe {
        // Wait for RX data available
        for _ in 0..100000 {
            let lsr: u8;
            core::arch::asm!("in al, dx", out("al") lsr, in("dx") port + 5);
            if lsr & 0x01 != 0 {
                // Read byte
                let byte: u8;
                core::arch::asm!("in al, dx", out("al") byte, in("dx") port);
                return Ok(byte);
            }
        }
        return Err("Serial RX timeout");
    }
    #[cfg(not(target_arch = "x86_64"))]
    { let _ = port; Err("Not implemented") }
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
