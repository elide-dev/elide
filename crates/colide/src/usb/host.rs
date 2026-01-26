// USB Host Controller Driver for Colide OS
// Supports xHCI (USB 3.x) and EHCI (USB 2.0) controllers
// Essential for USB WiFi dongles like mt7601u, rtl8188eu

use core::ptr::{read_volatile, write_volatile};

/// USB speed types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum UsbSpeed {
    Low,        // 1.5 Mbps (USB 1.0)
    Full,       // 12 Mbps (USB 1.1)
    High,       // 480 Mbps (USB 2.0)
    Super,      // 5 Gbps (USB 3.0)
    SuperPlus,  // 10 Gbps (USB 3.1)
}

/// USB device class codes
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum UsbClass {
    PerInterface = 0x00,
    Audio = 0x01,
    Cdc = 0x02,          // Communications
    Hid = 0x03,          // Human Interface Device
    Physical = 0x05,
    Image = 0x06,
    Printer = 0x07,
    MassStorage = 0x08,
    Hub = 0x09,
    CdcData = 0x0A,
    SmartCard = 0x0B,
    ContentSecurity = 0x0D,
    Video = 0x0E,
    PersonalHealthcare = 0x0F,
    AudioVideo = 0x10,
    Billboard = 0x11,
    TypeCBridge = 0x12,
    Diagnostic = 0xDC,
    WirelessController = 0xE0,  // WiFi, Bluetooth
    Miscellaneous = 0xEF,
    ApplicationSpecific = 0xFE,
    VendorSpecific = 0xFF,
}

/// USB endpoint types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum EndpointType {
    Control,
    Isochronous,
    Bulk,
    Interrupt,
}

/// USB endpoint direction
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum EndpointDirection {
    Out,  // Host to device
    In,   // Device to host
}

/// USB endpoint descriptor
#[derive(Debug, Clone)]
pub struct UsbEndpoint {
    pub address: u8,
    pub direction: EndpointDirection,
    pub ep_type: EndpointType,
    pub max_packet_size: u16,
    pub interval: u8,
}

impl UsbEndpoint {
    pub fn from_descriptor(desc: &[u8]) -> Option<Self> {
        if desc.len() < 7 || desc[1] != 0x05 {
            return None;
        }
        
        let address = desc[2] & 0x0F;
        let direction = if desc[2] & 0x80 != 0 {
            EndpointDirection::In
        } else {
            EndpointDirection::Out
        };
        let ep_type = match desc[3] & 0x03 {
            0 => EndpointType::Control,
            1 => EndpointType::Isochronous,
            2 => EndpointType::Bulk,
            3 => EndpointType::Interrupt,
            _ => return None,
        };
        let max_packet_size = u16::from_le_bytes([desc[4], desc[5]]);
        let interval = desc[6];
        
        Some(Self {
            address,
            direction,
            ep_type,
            max_packet_size,
            interval,
        })
    }
}

/// USB interface descriptor
#[derive(Debug, Clone)]
pub struct UsbInterface {
    pub interface_number: u8,
    pub alternate_setting: u8,
    pub interface_class: u8,
    pub interface_subclass: u8,
    pub interface_protocol: u8,
    pub endpoints: Vec<UsbEndpoint>,
}

/// USB configuration descriptor
#[derive(Debug, Clone)]
pub struct UsbConfiguration {
    pub config_value: u8,
    pub max_power_ma: u16,
    pub self_powered: bool,
    pub remote_wakeup: bool,
    pub interfaces: Vec<UsbInterface>,
}

/// USB device descriptor
#[derive(Debug, Clone)]
pub struct UsbDeviceDescriptor {
    pub usb_version: u16,
    pub device_class: u8,
    pub device_subclass: u8,
    pub device_protocol: u8,
    pub max_packet_size0: u8,
    pub vendor_id: u16,
    pub product_id: u16,
    pub device_version: u16,
    pub manufacturer_index: u8,
    pub product_index: u8,
    pub serial_index: u8,
    pub num_configurations: u8,
}

impl UsbDeviceDescriptor {
    pub fn from_bytes(data: &[u8]) -> Option<Self> {
        if data.len() < 18 || data[1] != 0x01 {
            return None;
        }
        
        Some(Self {
            usb_version: u16::from_le_bytes([data[2], data[3]]),
            device_class: data[4],
            device_subclass: data[5],
            device_protocol: data[6],
            max_packet_size0: data[7],
            vendor_id: u16::from_le_bytes([data[8], data[9]]),
            product_id: u16::from_le_bytes([data[10], data[11]]),
            device_version: u16::from_le_bytes([data[12], data[13]]),
            manufacturer_index: data[14],
            product_index: data[15],
            serial_index: data[16],
            num_configurations: data[17],
        })
    }
}

/// USB device state
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum UsbDeviceState {
    Attached,
    Powered,
    Default,
    Address,
    Configured,
    Suspended,
}

/// USB device handle
#[derive(Debug)]
pub struct UsbDevice {
    pub address: u8,
    pub speed: UsbSpeed,
    pub state: UsbDeviceState,
    pub descriptor: Option<UsbDeviceDescriptor>,
    pub configurations: Vec<UsbConfiguration>,
    pub current_config: u8,
    port: u8,
    hub_address: u8,
}

impl UsbDevice {
    pub fn new(port: u8, speed: UsbSpeed) -> Self {
        Self {
            address: 0,
            speed,
            state: UsbDeviceState::Attached,
            descriptor: None,
            configurations: Vec::new(),
            current_config: 0,
            port,
            hub_address: 0,
        }
    }
    
    /// Check if device is a WiFi adapter
    pub fn is_wifi_adapter(&self) -> bool {
        if let Some(desc) = &self.descriptor {
            // Check known WiFi dongle VID/PIDs
            let known_wifi = [
                (0x148f, 0x7601),  // MediaTek MT7601U
                (0x0bda, 0x8179),  // Realtek RTL8188EUS
                (0x0bda, 0x8176),  // Realtek RTL8188CUS
                (0x0cf3, 0x9271),  // Atheros AR9271
                (0x2357, 0x010c),  // TP-Link TL-WN722N v2
                (0x2357, 0x0109),  // TP-Link TL-WN823N v2
            ];
            
            for (vid, pid) in known_wifi {
                if desc.vendor_id == vid && desc.product_id == pid {
                    return true;
                }
            }
            
            // Check class code for wireless controller
            if desc.device_class == UsbClass::WirelessController as u8 {
                return true;
            }
            
            // Check interface class
            for config in &self.configurations {
                for iface in &config.interfaces {
                    if iface.interface_class == UsbClass::WirelessController as u8 {
                        return true;
                    }
                }
            }
        }
        false
    }
}

/// USB transfer request
#[derive(Debug)]
pub struct UsbTransfer {
    pub device_address: u8,
    pub endpoint: u8,
    pub direction: EndpointDirection,
    pub transfer_type: EndpointType,
    pub buffer: Vec<u8>,
    pub length: usize,
    pub actual_length: usize,
    pub status: UsbTransferStatus,
}

/// USB transfer status
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum UsbTransferStatus {
    Pending,
    Complete,
    Error,
    Stall,
    Timeout,
    Cancelled,
}

/// USB setup packet for control transfers
#[derive(Debug, Clone, Copy)]
#[repr(C, packed)]
pub struct UsbSetupPacket {
    pub request_type: u8,
    pub request: u8,
    pub value: u16,
    pub index: u16,
    pub length: u16,
}

impl UsbSetupPacket {
    pub const GET_STATUS: u8 = 0x00;
    pub const CLEAR_FEATURE: u8 = 0x01;
    pub const SET_FEATURE: u8 = 0x03;
    pub const SET_ADDRESS: u8 = 0x05;
    pub const GET_DESCRIPTOR: u8 = 0x06;
    pub const SET_DESCRIPTOR: u8 = 0x07;
    pub const GET_CONFIGURATION: u8 = 0x08;
    pub const SET_CONFIGURATION: u8 = 0x09;
    pub const GET_INTERFACE: u8 = 0x0A;
    pub const SET_INTERFACE: u8 = 0x0B;
    
    pub const DESC_DEVICE: u16 = 0x0100;
    pub const DESC_CONFIGURATION: u16 = 0x0200;
    pub const DESC_STRING: u16 = 0x0300;
    
    pub fn get_descriptor(desc_type: u16, index: u8, length: u16) -> Self {
        Self {
            request_type: 0x80,  // Device to host, standard, device
            request: Self::GET_DESCRIPTOR,
            value: desc_type | (index as u16),
            index: 0,
            length,
        }
    }
    
    pub fn set_address(address: u8) -> Self {
        Self {
            request_type: 0x00,  // Host to device, standard, device
            request: Self::SET_ADDRESS,
            value: address as u16,
            index: 0,
            length: 0,
        }
    }
    
    pub fn set_configuration(config: u8) -> Self {
        Self {
            request_type: 0x00,
            request: Self::SET_CONFIGURATION,
            value: config as u16,
            index: 0,
            length: 0,
        }
    }
    
    pub fn to_bytes(&self) -> [u8; 8] {
        [
            self.request_type,
            self.request,
            self.value as u8,
            (self.value >> 8) as u8,
            self.index as u8,
            (self.index >> 8) as u8,
            self.length as u8,
            (self.length >> 8) as u8,
        ]
    }
}

/// USB Host Controller trait
pub trait UsbHostController: Send + Sync {
    /// Initialize the controller
    fn init(&mut self) -> Result<(), UsbError>;
    
    /// Start the controller
    fn start(&mut self) -> Result<(), UsbError>;
    
    /// Stop the controller
    fn stop(&mut self) -> Result<(), UsbError>;
    
    /// Get number of ports
    fn port_count(&self) -> u8;
    
    /// Check if port has device connected
    fn port_connected(&self, port: u8) -> bool;
    
    /// Get port speed
    fn port_speed(&self, port: u8) -> Option<UsbSpeed>;
    
    /// Reset port
    fn reset_port(&mut self, port: u8) -> Result<(), UsbError>;
    
    /// Enable port
    fn enable_port(&mut self, port: u8) -> Result<(), UsbError>;
    
    /// Disable port
    fn disable_port(&mut self, port: u8) -> Result<(), UsbError>;
    
    /// Submit control transfer
    fn control_transfer(
        &mut self,
        device: u8,
        setup: &UsbSetupPacket,
        data: Option<&mut [u8]>,
        timeout_ms: u32,
    ) -> Result<usize, UsbError>;
    
    /// Submit bulk transfer
    fn bulk_transfer(
        &mut self,
        device: u8,
        endpoint: u8,
        data: &mut [u8],
        timeout_ms: u32,
    ) -> Result<usize, UsbError>;
    
    /// Submit interrupt transfer
    fn interrupt_transfer(
        &mut self,
        device: u8,
        endpoint: u8,
        data: &mut [u8],
        timeout_ms: u32,
    ) -> Result<usize, UsbError>;
}

/// USB error types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum UsbError {
    NotInitialized,
    NotConnected,
    InvalidPort,
    InvalidEndpoint,
    InvalidDevice,
    Timeout,
    Stall,
    DataToggle,
    Crc,
    BitStuff,
    NoResponse,
    Overrun,
    BufferTooSmall,
    NotSupported,
    IoError,
}

/// xHCI Host Controller (USB 3.x)
pub struct XhciController {
    base_addr: usize,
    operational_base: usize,
    runtime_base: usize,
    doorbell_base: usize,
    port_count: u8,
    max_slots: u8,
    initialized: bool,
}

impl XhciController {
    pub fn new(base_addr: usize) -> Self {
        Self {
            base_addr,
            operational_base: 0,
            runtime_base: 0,
            doorbell_base: 0,
            port_count: 0,
            max_slots: 0,
            initialized: false,
        }
    }
    
    fn read_cap_reg(&self, offset: usize) -> u32 {
        unsafe { read_volatile((self.base_addr + offset) as *const u32) }
    }
    
    fn read_op_reg(&self, offset: usize) -> u32 {
        unsafe { read_volatile((self.operational_base + offset) as *const u32) }
    }
    
    fn write_op_reg(&mut self, offset: usize, value: u32) {
        unsafe { write_volatile((self.operational_base + offset) as *mut u32, value) }
    }
}

impl UsbHostController for XhciController {
    fn init(&mut self) -> Result<(), UsbError> {
        // Read capability registers
        let cap_length = (self.read_cap_reg(0x00) & 0xFF) as usize;
        let hcs_params1 = self.read_cap_reg(0x04);
        let hcs_params2 = self.read_cap_reg(0x08);
        let hcc_params1 = self.read_cap_reg(0x10);
        let db_offset = self.read_cap_reg(0x14);
        let rts_offset = self.read_cap_reg(0x18);
        
        // Calculate register base addresses
        self.operational_base = self.base_addr + cap_length;
        self.runtime_base = self.base_addr + (rts_offset & !0x1F) as usize;
        self.doorbell_base = self.base_addr + (db_offset & !0x03) as usize;
        
        // Extract capabilities
        self.max_slots = (hcs_params1 & 0xFF) as u8;
        self.port_count = ((hcs_params1 >> 24) & 0xFF) as u8;
        
        // Stop controller if running
        let usbcmd = self.read_op_reg(0x00);
        if usbcmd & 0x01 != 0 {
            self.write_op_reg(0x00, usbcmd & !0x01);
            // Wait for halt
            for _ in 0..1000 {
                if self.read_op_reg(0x04) & 0x01 != 0 {
                    break;
                }
            }
        }
        
        // Reset controller
        self.write_op_reg(0x00, 0x02);
        for _ in 0..1000 {
            if self.read_op_reg(0x00) & 0x02 == 0 {
                break;
            }
        }
        
        self.initialized = true;
        Ok(())
    }
    
    fn start(&mut self) -> Result<(), UsbError> {
        if !self.initialized {
            return Err(UsbError::NotInitialized);
        }
        
        // Enable interrupts and start controller
        let usbcmd = self.read_op_reg(0x00);
        self.write_op_reg(0x00, usbcmd | 0x05);  // Run + Interrupt Enable
        
        Ok(())
    }
    
    fn stop(&mut self) -> Result<(), UsbError> {
        let usbcmd = self.read_op_reg(0x00);
        self.write_op_reg(0x00, usbcmd & !0x01);
        Ok(())
    }
    
    fn port_count(&self) -> u8 {
        self.port_count
    }
    
    fn port_connected(&self, port: u8) -> bool {
        if port >= self.port_count {
            return false;
        }
        let portsc = self.read_op_reg(0x400 + (port as usize * 0x10));
        portsc & 0x01 != 0  // Current Connect Status
    }
    
    fn port_speed(&self, port: u8) -> Option<UsbSpeed> {
        if port >= self.port_count || !self.port_connected(port) {
            return None;
        }
        let portsc = self.read_op_reg(0x400 + (port as usize * 0x10));
        let speed = (portsc >> 10) & 0x0F;
        match speed {
            1 => Some(UsbSpeed::Full),
            2 => Some(UsbSpeed::Low),
            3 => Some(UsbSpeed::High),
            4 => Some(UsbSpeed::Super),
            5 => Some(UsbSpeed::SuperPlus),
            _ => None,
        }
    }
    
    fn reset_port(&mut self, port: u8) -> Result<(), UsbError> {
        if port >= self.port_count {
            return Err(UsbError::InvalidPort);
        }
        let offset = 0x400 + (port as usize * 0x10);
        let portsc = self.read_op_reg(offset);
        self.write_op_reg(offset, portsc | 0x10);  // Port Reset
        
        // Wait for reset to complete
        for _ in 0..100 {
            let portsc = self.read_op_reg(offset);
            if portsc & 0x10 == 0 {
                return Ok(());
            }
        }
        Err(UsbError::Timeout)
    }
    
    fn enable_port(&mut self, port: u8) -> Result<(), UsbError> {
        if port >= self.port_count {
            return Err(UsbError::InvalidPort);
        }
        // xHCI ports are enabled automatically after reset
        Ok(())
    }
    
    fn disable_port(&mut self, port: u8) -> Result<(), UsbError> {
        if port >= self.port_count {
            return Err(UsbError::InvalidPort);
        }
        let offset = 0x400 + (port as usize * 0x10);
        let portsc = self.read_op_reg(offset);
        self.write_op_reg(offset, portsc & !0x02);  // Clear Port Enabled
        Ok(())
    }
    
    fn control_transfer(
        &mut self,
        _device: u8,
        _setup: &UsbSetupPacket,
        _data: Option<&mut [u8]>,
        _timeout_ms: u32,
    ) -> Result<usize, UsbError> {
        // Full implementation requires TRB ring management
        // Placeholder for now
        Err(UsbError::NotSupported)
    }
    
    fn bulk_transfer(
        &mut self,
        _device: u8,
        _endpoint: u8,
        _data: &mut [u8],
        _timeout_ms: u32,
    ) -> Result<usize, UsbError> {
        Err(UsbError::NotSupported)
    }
    
    fn interrupt_transfer(
        &mut self,
        _device: u8,
        _endpoint: u8,
        _data: &mut [u8],
        _timeout_ms: u32,
    ) -> Result<usize, UsbError> {
        Err(UsbError::NotSupported)
    }
}

/// EHCI Host Controller (USB 2.0)
pub struct EhciController {
    base_addr: usize,
    operational_base: usize,
    port_count: u8,
    initialized: bool,
}

impl EhciController {
    pub fn new(base_addr: usize) -> Self {
        Self {
            base_addr,
            operational_base: 0,
            port_count: 0,
            initialized: false,
        }
    }
    
    fn read_cap_reg(&self, offset: usize) -> u32 {
        unsafe { read_volatile((self.base_addr + offset) as *const u32) }
    }
    
    fn read_op_reg(&self, offset: usize) -> u32 {
        unsafe { read_volatile((self.operational_base + offset) as *const u32) }
    }
    
    fn write_op_reg(&mut self, offset: usize, value: u32) {
        unsafe { write_volatile((self.operational_base + offset) as *mut u32, value) }
    }
}

impl UsbHostController for EhciController {
    fn init(&mut self) -> Result<(), UsbError> {
        let cap_length = (self.read_cap_reg(0x00) & 0xFF) as usize;
        let hcs_params = self.read_cap_reg(0x04);
        
        self.operational_base = self.base_addr + cap_length;
        self.port_count = (hcs_params & 0x0F) as u8;
        
        // Stop controller
        let usbcmd = self.read_op_reg(0x00);
        self.write_op_reg(0x00, usbcmd & !0x01);
        
        // Wait for halt
        for _ in 0..1000 {
            if self.read_op_reg(0x04) & 0x1000 != 0 {
                break;
            }
        }
        
        // Reset controller
        self.write_op_reg(0x00, 0x02);
        for _ in 0..1000 {
            if self.read_op_reg(0x00) & 0x02 == 0 {
                break;
            }
        }
        
        self.initialized = true;
        Ok(())
    }
    
    fn start(&mut self) -> Result<(), UsbError> {
        if !self.initialized {
            return Err(UsbError::NotInitialized);
        }
        self.write_op_reg(0x00, 0x01);  // Run
        Ok(())
    }
    
    fn stop(&mut self) -> Result<(), UsbError> {
        let usbcmd = self.read_op_reg(0x00);
        self.write_op_reg(0x00, usbcmd & !0x01);
        Ok(())
    }
    
    fn port_count(&self) -> u8 {
        self.port_count
    }
    
    fn port_connected(&self, port: u8) -> bool {
        if port >= self.port_count {
            return false;
        }
        let portsc = self.read_op_reg(0x44 + (port as usize * 4));
        portsc & 0x01 != 0
    }
    
    fn port_speed(&self, port: u8) -> Option<UsbSpeed> {
        if port >= self.port_count || !self.port_connected(port) {
            return None;
        }
        // EHCI only supports high-speed devices directly
        // Low/full speed devices are handled by companion controllers
        let portsc = self.read_op_reg(0x44 + (port as usize * 4));
        if portsc & 0x04 != 0 {  // Port Enabled
            Some(UsbSpeed::High)
        } else {
            // Device is low/full speed, needs companion controller
            None
        }
    }
    
    fn reset_port(&mut self, port: u8) -> Result<(), UsbError> {
        if port >= self.port_count {
            return Err(UsbError::InvalidPort);
        }
        let offset = 0x44 + (port as usize * 4);
        let portsc = self.read_op_reg(offset);
        
        // Set Port Reset
        self.write_op_reg(offset, portsc | 0x100);
        
        // Wait at least 50ms
        for _ in 0..50000 {
            core::hint::spin_loop();
        }
        
        // Clear Port Reset
        let portsc = self.read_op_reg(offset);
        self.write_op_reg(offset, portsc & !0x100);
        
        // Wait for reset to complete
        for _ in 0..100 {
            let portsc = self.read_op_reg(offset);
            if portsc & 0x100 == 0 {
                return Ok(());
            }
        }
        Err(UsbError::Timeout)
    }
    
    fn enable_port(&mut self, port: u8) -> Result<(), UsbError> {
        if port >= self.port_count {
            return Err(UsbError::InvalidPort);
        }
        // EHCI enables ports automatically after successful reset
        Ok(())
    }
    
    fn disable_port(&mut self, port: u8) -> Result<(), UsbError> {
        if port >= self.port_count {
            return Err(UsbError::InvalidPort);
        }
        let offset = 0x44 + (port as usize * 4);
        let portsc = self.read_op_reg(offset);
        self.write_op_reg(offset, portsc & !0x04);  // Clear Port Enabled
        Ok(())
    }
    
    fn control_transfer(
        &mut self,
        _device: u8,
        _setup: &UsbSetupPacket,
        _data: Option<&mut [u8]>,
        _timeout_ms: u32,
    ) -> Result<usize, UsbError> {
        // Full implementation requires QH/qTD management
        Err(UsbError::NotSupported)
    }
    
    fn bulk_transfer(
        &mut self,
        _device: u8,
        _endpoint: u8,
        _data: &mut [u8],
        _timeout_ms: u32,
    ) -> Result<usize, UsbError> {
        Err(UsbError::NotSupported)
    }
    
    fn interrupt_transfer(
        &mut self,
        _device: u8,
        _endpoint: u8,
        _data: &mut [u8],
        _timeout_ms: u32,
    ) -> Result<usize, UsbError> {
        Err(UsbError::NotSupported)
    }
}

/// USB device manager
pub struct UsbManager {
    devices: Vec<UsbDevice>,
    next_address: u8,
}

impl UsbManager {
    pub fn new() -> Self {
        Self {
            devices: Vec::new(),
            next_address: 1,
        }
    }
    
    /// Enumerate devices on controller
    pub fn enumerate<C: UsbHostController>(&mut self, controller: &mut C) -> Vec<u8> {
        let mut found_devices = Vec::new();
        
        for port in 0..controller.port_count() {
            if controller.port_connected(port) {
                if let Some(speed) = controller.port_speed(port) {
                    // Reset port
                    if controller.reset_port(port).is_ok() {
                        let mut device = UsbDevice::new(port, speed);
                        device.address = self.next_address;
                        self.next_address = self.next_address.wrapping_add(1);
                        if self.next_address == 0 {
                            self.next_address = 1;
                        }
                        
                        found_devices.push(device.address);
                        self.devices.push(device);
                    }
                }
            }
        }
        
        found_devices
    }
    
    /// Get device by address
    pub fn get_device(&self, address: u8) -> Option<&UsbDevice> {
        self.devices.iter().find(|d| d.address == address)
    }
    
    /// Get mutable device by address
    pub fn get_device_mut(&mut self, address: u8) -> Option<&mut UsbDevice> {
        self.devices.iter_mut().find(|d| d.address == address)
    }
    
    /// Find WiFi adapters
    pub fn find_wifi_adapters(&self) -> Vec<u8> {
        self.devices
            .iter()
            .filter(|d| d.is_wifi_adapter())
            .map(|d| d.address)
            .collect()
    }
    
    /// Remove disconnected device
    pub fn remove_device(&mut self, address: u8) {
        self.devices.retain(|d| d.address != address);
    }
}

impl Default for UsbManager {
    fn default() -> Self {
        Self::new()
    }
}
