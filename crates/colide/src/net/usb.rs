//! USB WiFi Dongle Support
//! 
//! Direct support for USB WiFi dongles like RTL8188EU and MT7601U.
//! Requires USB host controller (XHCI/EHCI) driver.

use super::wifi::WifiNetwork;

/// USB WiFi dongle state
static mut USB_INITIALIZED: bool = false;
static mut USB_DEVICE_TYPE: UsbWifiDevice = UsbWifiDevice::None;

/// Supported USB WiFi chipsets
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum UsbWifiDevice {
    None,
    Rtl8188eu,  // TP-Link TL-WN725N, Panda PAU05
    Rtl8192cu,  // Various adapters
    Mt7601u,    // Generic cheap dongles
}

/// USB device descriptor (simplified)
#[repr(C)]
pub struct UsbDeviceDescriptor {
    pub length: u8,
    pub descriptor_type: u8,
    pub usb_version: u16,
    pub device_class: u8,
    pub device_subclass: u8,
    pub device_protocol: u8,
    pub max_packet_size: u8,
    pub vendor_id: u16,
    pub product_id: u16,
    pub device_version: u16,
    pub manufacturer_index: u8,
    pub product_index: u8,
    pub serial_index: u8,
    pub num_configurations: u8,
}

/// Known USB WiFi device IDs
const RTL8188EU_IDS: &[(u16, u16)] = &[
    (0x0bda, 0x8179), // Realtek default
    (0x2357, 0x010c), // TP-Link TL-WN725N v2
    (0x2357, 0x0111), // TP-Link TL-WN725N v3
    (0x0df6, 0x0076), // Sitecom
    (0x0b05, 0x18f0), // ASUS
];

const MT7601U_IDS: &[(u16, u16)] = &[
    (0x148f, 0x7601), // MediaTek default
    (0x148f, 0x760b), // MediaTek alternate
    (0x0e8d, 0x7610), // MediaTek
];

/// Initialize USB WiFi subsystem
pub fn init() -> Result<(), &'static str> {
    // Step 1: Initialize USB host controller (XHCI or EHCI)
    if !init_usb_host_controller() {
        return Err("Failed to initialize USB host controller");
    }
    
    // Step 2: Enumerate USB devices and find WiFi dongle
    let device = find_wifi_dongle();
    
    unsafe {
        USB_INITIALIZED = device != UsbWifiDevice::None;
        USB_DEVICE_TYPE = device;
    }
    
    if device == UsbWifiDevice::None {
        return Err("No supported USB WiFi dongle found");
    }
    
    // Step 3: Initialize the specific driver
    match device {
        UsbWifiDevice::Rtl8188eu => init_rtl8188eu(),
        UsbWifiDevice::Mt7601u => init_mt7601u(),
        _ => Err("Unsupported device"),
    }
}

/// Initialize USB host controller (XHCI preferred, EHCI fallback)
fn init_usb_host_controller() -> bool {
    // Scan PCI for USB controllers
    // XHCI: Class 0C, Subclass 03, ProgIF 30
    // EHCI: Class 0C, Subclass 03, ProgIF 20
    
    // Try XHCI first (USB 3.x support)
    if let Some(xhci_base) = scan_pci_for_class(0x0C, 0x03, 0x30) {
        if init_xhci_controller(xhci_base) {
            return true;
        }
    }
    
    // Fall back to EHCI (USB 2.0)
    if let Some(ehci_base) = scan_pci_for_class(0x0C, 0x03, 0x20) {
        if init_ehci_controller(ehci_base) {
            return true;
        }
    }
    
    false
}

/// Scan PCI bus for device with given class/subclass/progif
fn scan_pci_for_class(class: u8, subclass: u8, progif: u8) -> Option<usize> {
    for bus in 0..256u16 {
        for device in 0..32u8 {
            for function in 0..8u8 {
                let vendor = pci_read_config_word(bus as u8, device, function, 0x00);
                if vendor == 0xFFFF {
                    continue;
                }
                
                let class_code = pci_read_config_word(bus as u8, device, function, 0x0A);
                let found_class = (class_code >> 8) as u8;
                let found_subclass = (class_code & 0xFF) as u8;
                let found_progif = pci_read_config_byte(bus as u8, device, function, 0x09);
                
                if found_class == class && found_subclass == subclass && found_progif == progif {
                    // Read BAR0 for MMIO base
                    let bar0 = pci_read_config_dword(bus as u8, device, function, 0x10);
                    if bar0 & 1 == 0 {
                        // Memory-mapped
                        return Some((bar0 & 0xFFFFFFF0) as usize);
                    }
                }
            }
        }
    }
    None
}

/// PCI configuration space read (word)
fn pci_read_config_word(bus: u8, device: u8, function: u8, offset: u8) -> u16 {
    let address = 0x80000000u32
        | ((bus as u32) << 16)
        | ((device as u32) << 11)
        | ((function as u32) << 8)
        | ((offset as u32) & 0xFC);
    
    #[cfg(target_arch = "x86_64")]
    unsafe {
        // Write address to CONFIG_ADDRESS (0xCF8)
        std::arch::asm!("out dx, eax", in("dx") 0xCF8u16, in("eax") address);
        // Read from CONFIG_DATA (0xCFC) + offset
        let value: u32;
        std::arch::asm!("in eax, dx", out("eax") value, in("dx") 0xCFCu16);
        ((value >> ((offset & 2) * 8)) & 0xFFFF) as u16
    }
    #[cfg(not(target_arch = "x86_64"))]
    { 0xFFFF }
}

/// PCI configuration space read (byte)
fn pci_read_config_byte(bus: u8, device: u8, function: u8, offset: u8) -> u8 {
    let word = pci_read_config_word(bus, device, function, offset & 0xFE);
    if offset & 1 == 0 {
        (word & 0xFF) as u8
    } else {
        (word >> 8) as u8
    }
}

/// PCI configuration space read (dword)
fn pci_read_config_dword(bus: u8, device: u8, function: u8, offset: u8) -> u32 {
    let low = pci_read_config_word(bus, device, function, offset) as u32;
    let high = pci_read_config_word(bus, device, function, offset + 2) as u32;
    low | (high << 16)
}

/// Initialize XHCI controller
fn init_xhci_controller(base: usize) -> bool {
    // Read capability registers
    let cap_length = unsafe { core::ptr::read_volatile(base as *const u8) } as usize;
    let op_base = base + cap_length;
    
    // Stop controller
    let usbcmd = unsafe { core::ptr::read_volatile((op_base) as *const u32) };
    if usbcmd & 1 != 0 {
        unsafe { core::ptr::write_volatile(op_base as *mut u32, usbcmd & !1) };
        // Wait for halt
        for _ in 0..10000 {
            let status = unsafe { core::ptr::read_volatile((op_base + 4) as *const u32) };
            if status & 1 != 0 {
                break;
            }
        }
    }
    
    // Reset controller
    unsafe { core::ptr::write_volatile(op_base as *mut u32, 2) };
    for _ in 0..10000 {
        let cmd = unsafe { core::ptr::read_volatile(op_base as *const u32) };
        if cmd & 2 == 0 {
            break;
        }
    }
    
    true
}

/// Initialize EHCI controller
fn init_ehci_controller(base: usize) -> bool {
    // Read capability length
    let cap_length = unsafe { core::ptr::read_volatile(base as *const u8) } as usize;
    let op_base = base + cap_length;
    
    // Stop controller
    let usbcmd = unsafe { core::ptr::read_volatile(op_base as *const u32) };
    unsafe { core::ptr::write_volatile(op_base as *mut u32, usbcmd & !1) };
    
    // Wait for halt
    for _ in 0..10000 {
        let status = unsafe { core::ptr::read_volatile((op_base + 4) as *const u32) };
        if status & (1 << 12) != 0 {
            break;
        }
    }
    
    // Reset controller
    unsafe { core::ptr::write_volatile(op_base as *mut u32, 2) };
    for _ in 0..10000 {
        let cmd = unsafe { core::ptr::read_volatile(op_base as *const u32) };
        if cmd & 2 == 0 {
            break;
        }
    }
    
    true
}

/// USB device address counter
static mut NEXT_USB_ADDRESS: u8 = 1;

/// Detected USB devices
static mut USB_DEVICES: [(u16, u16); 16] = [(0, 0); 16];
static mut USB_DEVICE_COUNT: usize = 0;

/// Scan USB bus for supported WiFi dongles
fn find_wifi_dongle() -> UsbWifiDevice {
    // Enumerate USB devices on all ports
    enumerate_usb_devices();
    
    // Check detected devices against known WiFi IDs
    unsafe {
        for i in 0..USB_DEVICE_COUNT {
            let (vid, pid) = USB_DEVICES[i];
            
            // Check RTL8188EU IDs
            for &(known_vid, known_pid) in RTL8188EU_IDS {
                if vid == known_vid && pid == known_pid {
                    return UsbWifiDevice::Rtl8188eu;
                }
            }
            
            // Check MT7601U IDs
            for &(known_vid, known_pid) in MT7601U_IDS {
                if vid == known_vid && pid == known_pid {
                    return UsbWifiDevice::Mt7601u;
                }
            }
        }
    }
    
    UsbWifiDevice::None
}

/// Enumerate all USB devices
fn enumerate_usb_devices() {
    unsafe {
        USB_DEVICE_COUNT = 0;
        NEXT_USB_ADDRESS = 1;
    }
    
    // For each root hub port, check for devices
    // This is simplified - real implementation would use XHCI/EHCI port registers
    for port in 0..8 {
        if let Some((vid, pid)) = probe_usb_port(port) {
            unsafe {
                if USB_DEVICE_COUNT < 16 {
                    USB_DEVICES[USB_DEVICE_COUNT] = (vid, pid);
                    USB_DEVICE_COUNT += 1;
                }
            }
        }
    }
}

/// Probe a USB port for connected device
fn probe_usb_port(port: u8) -> Option<(u16, u16)> {
    // In a real implementation:
    // 1. Check port status register for device connected
    // 2. Reset port
    // 3. Wait for reset complete
    // 4. Read device descriptor using control transfer
    // 5. Return VID/PID
    
    // For bare metal, we'd read from the actual USB controller registers
    // This is a placeholder that would be filled in during hardware testing
    let _ = port;
    None
}

/// Initialize RTL8188EU driver
fn init_rtl8188eu() -> Result<(), &'static str> {
    // RTL8188EU initialization sequence:
    // 1. Reset device
    // 2. Load firmware (if needed - some have ROM firmware)
    // 3. Configure radio
    // 4. Set MAC address
    // 5. Enable RX/TX
    
    Ok(())
}

/// Initialize MT7601U driver
fn init_mt7601u() -> Result<(), &'static str> {
    // MT7601U initialization sequence:
    // 1. Load firmware
    // 2. Initialize MAC
    // 3. Initialize BBP (baseband processor)
    // 4. Initialize RF
    // 5. Calibrate
    
    Ok(())
}

/// Scan for WiFi networks
pub fn scan() -> Vec<WifiNetwork> {
    unsafe {
        if !USB_INITIALIZED {
            return Vec::new();
        }
        
        match USB_DEVICE_TYPE {
            UsbWifiDevice::Rtl8188eu => rtl8188eu_scan(),
            UsbWifiDevice::Mt7601u => mt7601u_scan(),
            _ => Vec::new(),
        }
    }
}

/// Scan results buffer
static mut SCAN_RESULTS: [Option<WifiNetwork>; 32] = [const { None }; 32];
static mut SCAN_COUNT: usize = 0;

/// 802.11 channel frequencies (2.4GHz band, channels 1-14)
const CHANNEL_FREQS: [u16; 14] = [
    2412, 2417, 2422, 2427, 2432, 2437, 2442,
    2447, 2452, 2457, 2462, 2467, 2472, 2484
];

/// RTL8188EU scan implementation
fn rtl8188eu_scan() -> Vec<WifiNetwork> {
    // 802.11 active/passive scan implementation
    unsafe {
        SCAN_COUNT = 0;
        for i in 0..32 {
            SCAN_RESULTS[i] = None;
        }
    }
    
    // Scan each channel
    for (ch_idx, &freq) in CHANNEL_FREQS.iter().enumerate() {
        let channel = (ch_idx + 1) as u8;
        
        // Set channel
        rtl8188eu_set_channel(channel, freq);
        
        // Send probe request (active scan)
        rtl8188eu_send_probe_request();
        
        // Wait for responses (simplified - real impl would use timers)
        for _ in 0..1000 {
            if let Some(frame) = rtl8188eu_rx_mgmt() {
                parse_beacon_or_probe_response(&frame);
            }
        }
    }
    
    // Collect results
    let mut results = Vec::new();
    unsafe {
        for i in 0..SCAN_COUNT {
            if let Some(ref network) = SCAN_RESULTS[i] {
                results.push(network.clone());
            }
        }
    }
    results
}

/// Set RTL8188EU channel
fn rtl8188eu_set_channel(channel: u8, freq: u16) {
    // Write to RF registers to set channel
    // This involves writing to RTL8188EU-specific registers
    let _ = (channel, freq);
}

/// Send probe request frame
fn rtl8188eu_send_probe_request() {
    // Build and send 802.11 probe request
    // Frame: FC=0x0040, broadcast DA, wildcard SSID
}

/// Receive management frame
fn rtl8188eu_rx_mgmt() -> Option<Vec<u8>> {
    // Poll RX buffer for management frames
    None
}

/// Parse beacon or probe response frame
fn parse_beacon_or_probe_response(frame: &[u8]) {
    if frame.len() < 36 {
        return;
    }
    
    // Frame control at offset 0
    let fc = u16::from_le_bytes([frame[0], frame[1]]);
    let frame_type = (fc >> 2) & 0x3;
    let frame_subtype = (fc >> 4) & 0xF;
    
    // Management frame (type 0), beacon (subtype 8) or probe response (subtype 5)
    if frame_type != 0 || (frame_subtype != 8 && frame_subtype != 5) {
        return;
    }
    
    // BSSID at offset 16
    let mut bssid = [0u8; 6];
    bssid.copy_from_slice(&frame[16..22]);
    
    // Parse information elements starting at offset 36
    let mut ssid = String::new();
    let mut channel = 0u8;
    let mut rssi = -100i8;
    let mut security: u8 = 0; // 0=Open, 1=WEP, 2=WPA, 3=WPA2, 4=WPA3
    
    let mut ie_offset = 36;
    while ie_offset + 2 <= frame.len() {
        let ie_id = frame[ie_offset];
        let ie_len = frame[ie_offset + 1] as usize;
        
        if ie_offset + 2 + ie_len > frame.len() {
            break;
        }
        
        let ie_data = &frame[ie_offset + 2..ie_offset + 2 + ie_len];
        
        match ie_id {
            0 => { // SSID
                ssid = String::from_utf8_lossy(ie_data).to_string();
            }
            3 => { // DS Parameter Set (channel)
                if ie_len >= 1 {
                    channel = ie_data[0];
                }
            }
            48 => { // RSN (WPA2)
                security = 3; // WPA2
            }
            221 => { // Vendor specific (check for WPA)
                if ie_len >= 4 && ie_data[0..4] == [0x00, 0x50, 0xF2, 0x01] {
                    if security == 0 { // Open
                        security = 2; // WPA
                    }
                }
            }
            _ => {}
        }
        
        ie_offset += 2 + ie_len;
    }
    
    // Add to scan results
    let network = WifiNetwork {
        ssid,
        bssid,
        signal: rssi as i32,
        security,
        channel: channel as i32,
        frequency: super::wifi::channel_to_frequency(channel as i32),
    };
    
    unsafe {
        if SCAN_COUNT < 32 {
            // Check for duplicates
            let mut found = false;
            for i in 0..SCAN_COUNT {
                if let Some(ref existing) = SCAN_RESULTS[i] {
                    if existing.bssid == network.bssid {
                        found = true;
                        break;
                    }
                }
            }
            if !found {
                SCAN_RESULTS[SCAN_COUNT] = Some(network);
                SCAN_COUNT += 1;
            }
        }
    }
}

/// MT7601U scan implementation
fn mt7601u_scan() -> Vec<WifiNetwork> {
    // MT7601U uses similar 802.11 scanning
    // Reuse the RTL8188EU scan logic with MT7601U-specific register access
    unsafe {
        SCAN_COUNT = 0;
        for i in 0..32 {
            SCAN_RESULTS[i] = None;
        }
    }
    
    for (ch_idx, &freq) in CHANNEL_FREQS.iter().enumerate() {
        let channel = (ch_idx + 1) as u8;
        mt7601u_set_channel(channel, freq);
        mt7601u_send_probe_request();
        
        for _ in 0..1000 {
            if let Some(frame) = mt7601u_rx_mgmt() {
                parse_beacon_or_probe_response(&frame);
            }
        }
    }
    
    let mut results = Vec::new();
    unsafe {
        for i in 0..SCAN_COUNT {
            if let Some(ref network) = SCAN_RESULTS[i] {
                results.push(network.clone());
            }
        }
    }
    results
}

/// Set MT7601U channel
fn mt7601u_set_channel(channel: u8, freq: u16) {
    let _ = (channel, freq);
}

/// Send MT7601U probe request
fn mt7601u_send_probe_request() {}

/// Receive MT7601U management frame
fn mt7601u_rx_mgmt() -> Option<Vec<u8>> {
    None
}

/// Connect to WiFi network
pub fn connect(ssid: &str, password: &str) -> bool {
    unsafe {
        if !USB_INITIALIZED {
            return false;
        }
        
        match USB_DEVICE_TYPE {
            UsbWifiDevice::Rtl8188eu => rtl8188eu_connect(ssid, password),
            UsbWifiDevice::Mt7601u => mt7601u_connect(ssid, password),
            _ => false,
        }
    }
}

/// RTL8188EU connect implementation
fn rtl8188eu_connect(ssid: &str, password: &str) -> bool {
    // 802.11 association sequence:
    // 1. Find BSS from scan results
    // 2. Authenticate (Open System or Shared Key)
    // 3. Associate
    // 4. 4-way handshake (WPA2)
    // 5. Install keys
    
    let _ = (ssid, password); // Suppress warnings
    false
}

/// MT7601U connect implementation
fn mt7601u_connect(ssid: &str, password: &str) -> bool {
    let _ = (ssid, password);
    false
}

/// Disconnect from WiFi
pub fn disconnect() -> bool {
    unsafe {
        if !USB_INITIALIZED {
            return false;
        }
    }
    
    // Send deauthentication frame
    // Reset state
    
    true
}

/// Send Ethernet frame
pub fn send_frame(frame: &[u8]) -> bool {
    unsafe {
        if !USB_INITIALIZED {
            return false;
        }
        
        match USB_DEVICE_TYPE {
            UsbWifiDevice::Rtl8188eu => rtl8188eu_tx(frame),
            UsbWifiDevice::Mt7601u => mt7601u_tx(frame),
            _ => false,
        }
    }
}

/// RTL8188EU transmit
fn rtl8188eu_tx(frame: &[u8]) -> bool {
    // 1. Build 802.11 data frame from Ethernet frame
    // 2. Add TX descriptor
    // 3. Submit to USB bulk endpoint
    let _ = frame;
    false
}

/// MT7601U transmit
fn mt7601u_tx(frame: &[u8]) -> bool {
    let _ = frame;
    false
}

/// Receive Ethernet frame (non-blocking)
pub fn receive_frame() -> Option<Vec<u8>> {
    unsafe {
        if !USB_INITIALIZED {
            return None;
        }
        
        match USB_DEVICE_TYPE {
            UsbWifiDevice::Rtl8188eu => rtl8188eu_rx(),
            UsbWifiDevice::Mt7601u => mt7601u_rx(),
            _ => None,
        }
    }
}

/// RTL8188EU receive
fn rtl8188eu_rx() -> Option<Vec<u8>> {
    // 1. Poll USB bulk IN endpoint
    // 2. Parse RX descriptor
    // 3. Extract 802.11 frame
    // 4. Convert to Ethernet frame
    None
}

/// MT7601U receive
fn mt7601u_rx() -> Option<Vec<u8>> {
    None
}

// =============================================================================
// XHCI USB Host Controller Driver (Future Implementation)
// =============================================================================

/// XHCI capability registers
#[repr(C)]
pub struct XhciCapRegs {
    pub caplength: u8,
    pub reserved: u8,
    pub hciversion: u16,
    pub hcsparams1: u32,
    pub hcsparams2: u32,
    pub hcsparams3: u32,
    pub hccparams1: u32,
    pub dboff: u32,
    pub rtsoff: u32,
    pub hccparams2: u32,
}

/// XHCI operational registers
#[repr(C)]
pub struct XhciOpRegs {
    pub usbcmd: u32,
    pub usbsts: u32,
    pub pagesize: u32,
    pub reserved1: [u32; 2],
    pub dnctrl: u32,
    pub crcr: u64,
    pub reserved2: [u32; 4],
    pub dcbaap: u64,
    pub config: u32,
}

/// Transfer Request Block (TRB)
#[repr(C)]
pub struct Trb {
    pub parameter: u64,
    pub status: u32,
    pub control: u32,
}

/// Device context
#[repr(C)]
pub struct DeviceContext {
    pub slot: SlotContext,
    pub endpoints: [EndpointContext; 31],
}

#[repr(C)]
pub struct SlotContext {
    pub data: [u32; 8],
}

#[repr(C)]
pub struct EndpointContext {
    pub data: [u32; 8],
}
