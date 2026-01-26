//! IEEE 802.11 Frame Handling
//! 
//! Parsing and construction of 802.11 wireless frames.

/// 802.11 Frame Control field
#[derive(Debug, Clone, Copy)]
pub struct FrameControl {
    pub protocol_version: u8,
    pub frame_type: FrameType,
    pub frame_subtype: u8,
    pub to_ds: bool,
    pub from_ds: bool,
    pub more_fragments: bool,
    pub retry: bool,
    pub power_management: bool,
    pub more_data: bool,
    pub protected: bool,
    pub order: bool,
}

/// 802.11 Frame Types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum FrameType {
    Management = 0,
    Control = 1,
    Data = 2,
    Extension = 3,
}

/// Management Frame Subtypes
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum ManagementSubtype {
    AssociationRequest = 0,
    AssociationResponse = 1,
    ReassociationRequest = 2,
    ReassociationResponse = 3,
    ProbeRequest = 4,
    ProbeResponse = 5,
    Beacon = 8,
    Atim = 9,
    Disassociation = 10,
    Authentication = 11,
    Deauthentication = 12,
    Action = 13,
}

/// Data Frame Subtypes
#[derive(Debug, Clone, Copy, PartialEq)]
#[repr(u8)]
pub enum DataSubtype {
    Data = 0,
    DataCfAck = 1,
    DataCfPoll = 2,
    DataCfAckCfPoll = 3,
    Null = 4,
    CfAck = 5,
    CfPoll = 6,
    CfAckCfPoll = 7,
    QosData = 8,
    QosDataCfAck = 9,
    QosDataCfPoll = 10,
    QosDataCfAckCfPoll = 11,
    QosNull = 12,
}

/// 802.11 MAC Header
#[derive(Debug, Clone)]
pub struct MacHeader {
    pub frame_control: FrameControl,
    pub duration: u16,
    pub address1: [u8; 6],
    pub address2: [u8; 6],
    pub address3: [u8; 6],
    pub sequence_control: u16,
    pub address4: Option<[u8; 6]>,
    pub qos_control: Option<u16>,
}

impl MacHeader {
    /// Parse MAC header from bytes
    pub fn parse(data: &[u8]) -> Option<(Self, usize)> {
        if data.len() < 24 {
            return None;
        }
        
        let fc = parse_frame_control(u16::from_le_bytes([data[0], data[1]]));
        let duration = u16::from_le_bytes([data[2], data[3]]);
        
        let mut address1 = [0u8; 6];
        let mut address2 = [0u8; 6];
        let mut address3 = [0u8; 6];
        address1.copy_from_slice(&data[4..10]);
        address2.copy_from_slice(&data[10..16]);
        address3.copy_from_slice(&data[16..22]);
        
        let sequence_control = u16::from_le_bytes([data[22], data[23]]);
        
        let mut offset = 24;
        
        // Address4 present if both ToDS and FromDS are set
        let address4 = if fc.to_ds && fc.from_ds {
            if data.len() < 30 {
                return None;
            }
            let mut addr = [0u8; 6];
            addr.copy_from_slice(&data[24..30]);
            offset = 30;
            Some(addr)
        } else {
            None
        };
        
        // QoS control present for QoS data frames
        let qos_control = if fc.frame_type == FrameType::Data && (fc.frame_subtype & 0x08) != 0 {
            if data.len() < offset + 2 {
                return None;
            }
            let qos = u16::from_le_bytes([data[offset], data[offset + 1]]);
            offset += 2;
            Some(qos)
        } else {
            None
        };
        
        Some((MacHeader {
            frame_control: fc,
            duration,
            address1,
            address2,
            address3,
            sequence_control,
            address4,
            qos_control,
        }, offset))
    }
    
    /// Serialize MAC header to bytes
    pub fn to_bytes(&self) -> Vec<u8> {
        let mut data = Vec::with_capacity(32);
        
        let fc = serialize_frame_control(&self.frame_control);
        data.extend_from_slice(&fc.to_le_bytes());
        data.extend_from_slice(&self.duration.to_le_bytes());
        data.extend_from_slice(&self.address1);
        data.extend_from_slice(&self.address2);
        data.extend_from_slice(&self.address3);
        data.extend_from_slice(&self.sequence_control.to_le_bytes());
        
        if let Some(addr4) = self.address4 {
            data.extend_from_slice(&addr4);
        }
        
        if let Some(qos) = self.qos_control {
            data.extend_from_slice(&qos.to_le_bytes());
        }
        
        data
    }
}

/// Parse Frame Control field
fn parse_frame_control(fc: u16) -> FrameControl {
    FrameControl {
        protocol_version: (fc & 0x03) as u8,
        frame_type: match (fc >> 2) & 0x03 {
            0 => FrameType::Management,
            1 => FrameType::Control,
            2 => FrameType::Data,
            _ => FrameType::Extension,
        },
        frame_subtype: ((fc >> 4) & 0x0F) as u8,
        to_ds: (fc & 0x0100) != 0,
        from_ds: (fc & 0x0200) != 0,
        more_fragments: (fc & 0x0400) != 0,
        retry: (fc & 0x0800) != 0,
        power_management: (fc & 0x1000) != 0,
        more_data: (fc & 0x2000) != 0,
        protected: (fc & 0x4000) != 0,
        order: (fc & 0x8000) != 0,
    }
}

/// Serialize Frame Control field
fn serialize_frame_control(fc: &FrameControl) -> u16 {
    let mut value: u16 = 0;
    value |= (fc.protocol_version as u16) & 0x03;
    value |= ((fc.frame_type as u16) & 0x03) << 2;
    value |= ((fc.frame_subtype as u16) & 0x0F) << 4;
    if fc.to_ds { value |= 0x0100; }
    if fc.from_ds { value |= 0x0200; }
    if fc.more_fragments { value |= 0x0400; }
    if fc.retry { value |= 0x0800; }
    if fc.power_management { value |= 0x1000; }
    if fc.more_data { value |= 0x2000; }
    if fc.protected { value |= 0x4000; }
    if fc.order { value |= 0x8000; }
    value
}

/// Information Element
#[derive(Debug, Clone)]
pub struct InformationElement {
    pub id: u8,
    pub data: Vec<u8>,
}

/// Common IE IDs
pub const IE_SSID: u8 = 0;
pub const IE_SUPPORTED_RATES: u8 = 1;
pub const IE_DS_PARAMETER_SET: u8 = 3;
pub const IE_TIM: u8 = 5;
pub const IE_COUNTRY: u8 = 7;
pub const IE_RSN: u8 = 48;
pub const IE_EXTENDED_RATES: u8 = 50;
pub const IE_VENDOR: u8 = 221;

/// Parse Information Elements from beacon/probe response
pub fn parse_information_elements(data: &[u8]) -> Vec<InformationElement> {
    let mut elements = Vec::new();
    let mut offset = 0;
    
    while offset + 2 <= data.len() {
        let id = data[offset];
        let len = data[offset + 1] as usize;
        
        if offset + 2 + len > data.len() {
            break;
        }
        
        elements.push(InformationElement {
            id,
            data: data[offset + 2..offset + 2 + len].to_vec(),
        });
        
        offset += 2 + len;
    }
    
    elements
}

/// Build probe request frame
pub fn build_probe_request(
    source: &[u8; 6],
    ssid: Option<&str>,
) -> Vec<u8> {
    let mut frame = Vec::new();
    
    // Frame Control: Probe Request (subtype 4)
    let fc = FrameControl {
        protocol_version: 0,
        frame_type: FrameType::Management,
        frame_subtype: ManagementSubtype::ProbeRequest as u8,
        to_ds: false,
        from_ds: false,
        more_fragments: false,
        retry: false,
        power_management: false,
        more_data: false,
        protected: false,
        order: false,
    };
    
    frame.extend_from_slice(&serialize_frame_control(&fc).to_le_bytes());
    frame.extend_from_slice(&0u16.to_le_bytes()); // Duration
    frame.extend_from_slice(&[0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]); // DA (broadcast)
    frame.extend_from_slice(source); // SA
    frame.extend_from_slice(&[0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF]); // BSSID (wildcard)
    frame.extend_from_slice(&0u16.to_le_bytes()); // Sequence control
    
    // SSID IE
    frame.push(IE_SSID);
    if let Some(s) = ssid {
        frame.push(s.len() as u8);
        frame.extend_from_slice(s.as_bytes());
    } else {
        frame.push(0); // Wildcard SSID
    }
    
    // Supported Rates IE
    frame.push(IE_SUPPORTED_RATES);
    frame.push(8);
    frame.extend_from_slice(&[0x82, 0x84, 0x8B, 0x96, 0x0C, 0x12, 0x18, 0x24]); // 1,2,5.5,11,6,9,12,18 Mbps
    
    frame
}

/// Build authentication frame
pub fn build_authentication(
    destination: &[u8; 6],
    source: &[u8; 6],
    bssid: &[u8; 6],
    auth_algorithm: u16,
    auth_seq: u16,
    status: u16,
) -> Vec<u8> {
    let mut frame = Vec::new();
    
    let fc = FrameControl {
        protocol_version: 0,
        frame_type: FrameType::Management,
        frame_subtype: ManagementSubtype::Authentication as u8,
        to_ds: false,
        from_ds: false,
        more_fragments: false,
        retry: false,
        power_management: false,
        more_data: false,
        protected: false,
        order: false,
    };
    
    frame.extend_from_slice(&serialize_frame_control(&fc).to_le_bytes());
    frame.extend_from_slice(&0u16.to_le_bytes()); // Duration
    frame.extend_from_slice(destination);
    frame.extend_from_slice(source);
    frame.extend_from_slice(bssid);
    frame.extend_from_slice(&0u16.to_le_bytes()); // Sequence control
    
    // Authentication algorithm (0 = Open System)
    frame.extend_from_slice(&auth_algorithm.to_le_bytes());
    // Authentication sequence number
    frame.extend_from_slice(&auth_seq.to_le_bytes());
    // Status code
    frame.extend_from_slice(&status.to_le_bytes());
    
    frame
}

/// Convert 802.11 data frame to Ethernet frame
pub fn wifi_to_ethernet(wifi_frame: &[u8]) -> Option<Vec<u8>> {
    let (header, header_len) = MacHeader::parse(wifi_frame)?;
    
    if header.frame_control.frame_type != FrameType::Data {
        return None;
    }
    
    // Skip header and any encryption overhead
    let mut payload_offset = header_len;
    if header.frame_control.protected {
        payload_offset += 8; // CCMP header
    }
    
    if wifi_frame.len() <= payload_offset + 8 {
        return None;
    }
    
    // Check for LLC/SNAP header
    let llc = &wifi_frame[payload_offset..];
    if llc.len() < 8 || llc[0] != 0xAA || llc[1] != 0xAA || llc[2] != 0x03 {
        return None;
    }
    
    let ethertype = [llc[6], llc[7]];
    let data_offset = payload_offset + 8;
    
    // Determine source and destination MAC based on ToDS/FromDS
    let (dst, src) = match (header.frame_control.to_ds, header.frame_control.from_ds) {
        (false, false) => (header.address1, header.address2), // IBSS
        (false, true) => (header.address1, header.address3),  // From AP
        (true, false) => (header.address3, header.address2),  // To AP
        (true, true) => (header.address3, header.address4?),  // WDS
    };
    
    // Build Ethernet frame
    let mut eth = Vec::with_capacity(14 + wifi_frame.len() - data_offset);
    eth.extend_from_slice(&dst);
    eth.extend_from_slice(&src);
    eth.extend_from_slice(&ethertype);
    eth.extend_from_slice(&wifi_frame[data_offset..]);
    
    Some(eth)
}

/// Convert Ethernet frame to 802.11 data frame
pub fn ethernet_to_wifi(
    eth_frame: &[u8],
    bssid: &[u8; 6],
    to_ap: bool,
) -> Option<Vec<u8>> {
    if eth_frame.len() < 14 {
        return None;
    }
    
    let mut dst = [0u8; 6];
    let mut src = [0u8; 6];
    dst.copy_from_slice(&eth_frame[0..6]);
    src.copy_from_slice(&eth_frame[6..12]);
    let ethertype = &eth_frame[12..14];
    
    let fc = FrameControl {
        protocol_version: 0,
        frame_type: FrameType::Data,
        frame_subtype: DataSubtype::QosData as u8,
        to_ds: to_ap,
        from_ds: !to_ap,
        more_fragments: false,
        retry: false,
        power_management: false,
        more_data: false,
        protected: false, // Set by encryption layer
        order: false,
    };
    
    let header = MacHeader {
        frame_control: fc,
        duration: 0,
        address1: if to_ap { *bssid } else { dst },
        address2: if to_ap { src } else { *bssid },
        address3: if to_ap { dst } else { src },
        sequence_control: 0, // Set by driver
        address4: None,
        qos_control: Some(0),
    };
    
    let mut frame = header.to_bytes();
    
    // LLC/SNAP header
    frame.extend_from_slice(&[0xAA, 0xAA, 0x03, 0x00, 0x00, 0x00]);
    frame.extend_from_slice(ethertype);
    
    // Data
    frame.extend_from_slice(&eth_frame[14..]);
    
    Some(frame)
}
