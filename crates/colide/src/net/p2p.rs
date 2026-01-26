//! WiFi Direct (P2P) Support
//!
//! Implements Wi-Fi Peer-to-Peer (Wi-Fi Direct) for device-to-device
//! communication without traditional infrastructure.

/// P2P device role
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum P2pRole {
    #[default]
    Device,
    GroupOwner,
    Client,
}

/// P2P state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum P2pState {
    #[default]
    Disabled,
    Idle,
    Discovering,
    Negotiating,
    Forming,
    GroupOwner,
    Client,
}

/// P2P device info
#[derive(Debug, Clone)]
pub struct P2pDevice {
    /// Device address (P2P Interface Address)
    pub device_addr: [u8; 6],
    /// Device name
    pub name: String,
    /// Primary device type
    pub device_type: DeviceType,
    /// Configuration methods supported
    pub config_methods: ConfigMethods,
    /// Device capability bitmap
    pub capability: u8,
    /// Group capability bitmap
    pub group_capability: u8,
    /// WPS device password ID
    pub wps_method: WpsMethod,
    /// Signal strength (dBm)
    pub signal: i8,
    /// Last seen timestamp (ms)
    pub last_seen: u64,
}

/// Primary device type category
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum DeviceType {
    #[default]
    Computer,
    InputDevice,
    PrinterScanner,
    Camera,
    Storage,
    NetworkInfra,
    Display,
    MultimediaDevice,
    Gaming,
    Telephone,
    Audio,
    Docking,
    Other,
}

impl DeviceType {
    pub fn category_id(&self) -> u16 {
        match self {
            Self::Computer => 1,
            Self::InputDevice => 2,
            Self::PrinterScanner => 3,
            Self::Camera => 4,
            Self::Storage => 5,
            Self::NetworkInfra => 6,
            Self::Display => 7,
            Self::MultimediaDevice => 8,
            Self::Gaming => 9,
            Self::Telephone => 10,
            Self::Audio => 11,
            Self::Docking => 12,
            Self::Other => 255,
        }
    }
    
    pub fn from_category_id(id: u16) -> Self {
        match id {
            1 => Self::Computer,
            2 => Self::InputDevice,
            3 => Self::PrinterScanner,
            4 => Self::Camera,
            5 => Self::Storage,
            6 => Self::NetworkInfra,
            7 => Self::Display,
            8 => Self::MultimediaDevice,
            9 => Self::Gaming,
            10 => Self::Telephone,
            11 => Self::Audio,
            12 => Self::Docking,
            _ => Self::Other,
        }
    }
}

/// WPS configuration methods
#[derive(Debug, Clone, Copy, Default)]
pub struct ConfigMethods(u16);

impl ConfigMethods {
    pub const DISPLAY: u16 = 0x0008;
    pub const PUSH_BUTTON: u16 = 0x0080;
    pub const KEYPAD: u16 = 0x0100;
    pub const VIRTUAL_PUSH_BUTTON: u16 = 0x0280;
    pub const PHYSICAL_PUSH_BUTTON: u16 = 0x0480;
    pub const P2PS_DEFAULT: u16 = 0x1000;
    
    pub fn new(methods: u16) -> Self {
        Self(methods)
    }
    
    pub fn supports_display(&self) -> bool {
        (self.0 & Self::DISPLAY) != 0
    }
    
    pub fn supports_pbc(&self) -> bool {
        (self.0 & (Self::PUSH_BUTTON | Self::VIRTUAL_PUSH_BUTTON | Self::PHYSICAL_PUSH_BUTTON)) != 0
    }
    
    pub fn supports_keypad(&self) -> bool {
        (self.0 & Self::KEYPAD) != 0
    }
}

/// WPS method for connection
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum WpsMethod {
    #[default]
    NotSpecified,
    PushButton,
    DisplayPin,
    KeypadPin,
    P2ps,
}

/// P2P Group Owner Negotiation intent
#[derive(Debug, Clone, Copy)]
pub struct GoIntent(u8);

impl GoIntent {
    /// Create with intent value (0-15)
    pub fn new(intent: u8) -> Self {
        Self(intent.min(15))
    }
    
    /// Force Group Owner role
    pub fn force_go() -> Self {
        Self(15)
    }
    
    /// Force Client role
    pub fn force_client() -> Self {
        Self(0)
    }
    
    /// Balanced intent
    pub fn balanced() -> Self {
        Self(7)
    }
    
    pub fn value(&self) -> u8 {
        self.0
    }
    
    /// Compare intents to determine GO
    pub fn wins_against(&self, other: Self, tie_breaker: bool) -> bool {
        if self.0 > other.0 {
            true
        } else if self.0 < other.0 {
            false
        } else {
            tie_breaker
        }
    }
}

impl Default for GoIntent {
    fn default() -> Self {
        Self::balanced()
    }
}

/// P2P connection request
#[derive(Debug, Clone)]
pub struct P2pConnectRequest {
    pub peer_addr: [u8; 6],
    pub wps_method: WpsMethod,
    pub pin: Option<String>,
    pub go_intent: GoIntent,
    pub persistent: bool,
}

impl P2pConnectRequest {
    pub fn pbc(peer: [u8; 6]) -> Self {
        Self {
            peer_addr: peer,
            wps_method: WpsMethod::PushButton,
            pin: None,
            go_intent: GoIntent::balanced(),
            persistent: false,
        }
    }
    
    pub fn pin(peer: [u8; 6], pin: &str, display: bool) -> Self {
        Self {
            peer_addr: peer,
            wps_method: if display { WpsMethod::DisplayPin } else { WpsMethod::KeypadPin },
            pin: Some(pin.to_string()),
            go_intent: GoIntent::balanced(),
            persistent: false,
        }
    }
    
    pub fn with_go_intent(mut self, intent: GoIntent) -> Self {
        self.go_intent = intent;
        self
    }
    
    pub fn persistent(mut self) -> Self {
        self.persistent = true;
        self
    }
}

/// P2P Group info
#[derive(Debug, Clone)]
pub struct P2pGroup {
    /// Group ID (GO device address + SSID)
    pub go_addr: [u8; 6],
    pub ssid: Vec<u8>,
    /// Operating channel
    pub frequency: u32,
    /// Our role in the group
    pub role: P2pRole,
    /// Group clients (if we're GO)
    pub clients: Vec<P2pDevice>,
    /// PSK for the group
    pub psk: Option<[u8; 32]>,
    /// Passphrase (if available)
    pub passphrase: Option<String>,
    /// Is persistent group
    pub persistent: bool,
    /// Persistent group ID
    pub persistent_id: Option<u32>,
}

impl P2pGroup {
    pub fn new_go(ssid: &[u8], frequency: u32, psk: [u8; 32]) -> Self {
        Self {
            go_addr: [0; 6],  // Set later
            ssid: ssid.to_vec(),
            frequency,
            role: P2pRole::GroupOwner,
            clients: Vec::new(),
            psk: Some(psk),
            passphrase: None,
            persistent: false,
            persistent_id: None,
        }
    }
    
    pub fn new_client(go_addr: [u8; 6], ssid: &[u8], frequency: u32) -> Self {
        Self {
            go_addr,
            ssid: ssid.to_vec(),
            frequency,
            role: P2pRole::Client,
            clients: Vec::new(),
            psk: None,
            passphrase: None,
            persistent: false,
            persistent_id: None,
        }
    }
}

/// P2P discovery result
#[derive(Debug, Clone)]
pub struct P2pDiscoveryResult {
    pub devices: Vec<P2pDevice>,
    pub groups: Vec<P2pGroupInfo>,
}

/// Discovered P2P group info
#[derive(Debug, Clone)]
pub struct P2pGroupInfo {
    pub go_addr: [u8; 6],
    pub ssid: Vec<u8>,
    pub frequency: u32,
    pub signal: i8,
}

/// P2P action frame types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum P2pActionType {
    GoNegotiationRequest,
    GoNegotiationResponse,
    GoNegotiationConfirm,
    InvitationRequest,
    InvitationResponse,
    DeviceDiscoverabilityRequest,
    DeviceDiscoverabilityResponse,
    ProvisionDiscoveryRequest,
    ProvisionDiscoveryResponse,
    PresenceRequest,
    PresenceResponse,
}

impl P2pActionType {
    pub fn subtype(&self) -> u8 {
        match self {
            Self::GoNegotiationRequest => 0,
            Self::GoNegotiationResponse => 1,
            Self::GoNegotiationConfirm => 2,
            Self::InvitationRequest => 3,
            Self::InvitationResponse => 4,
            Self::DeviceDiscoverabilityRequest => 5,
            Self::DeviceDiscoverabilityResponse => 6,
            Self::ProvisionDiscoveryRequest => 7,
            Self::ProvisionDiscoveryResponse => 8,
            Self::PresenceRequest => 9,
            Self::PresenceResponse => 10,
        }
    }
}

/// P2P Manager
pub struct P2pManager {
    state: P2pState,
    our_device: P2pDevice,
    discovered: Vec<P2pDevice>,
    group: Option<P2pGroup>,
    pending_request: Option<P2pConnectRequest>,
    listen_channel: u8,
    operating_channel: u8,
}

impl P2pManager {
    pub fn new(device_addr: [u8; 6], name: &str) -> Self {
        Self {
            state: P2pState::Disabled,
            our_device: P2pDevice {
                device_addr,
                name: name.to_string(),
                device_type: DeviceType::Computer,
                config_methods: ConfigMethods::new(
                    ConfigMethods::DISPLAY | ConfigMethods::PUSH_BUTTON | ConfigMethods::KEYPAD
                ),
                capability: 0x25,  // Service Discovery, Invitation Procedure, Client Discoverability
                group_capability: 0,
                wps_method: WpsMethod::NotSpecified,
                signal: 0,
                last_seen: 0,
            },
            discovered: Vec::new(),
            group: None,
            pending_request: None,
            listen_channel: 6,  // Social channel
            operating_channel: 6,
        }
    }
    
    /// Enable P2P
    pub fn enable(&mut self) -> P2pAction {
        self.state = P2pState::Idle;
        P2pAction::SetListenChannel(self.listen_channel)
    }
    
    /// Disable P2P
    pub fn disable(&mut self) -> P2pAction {
        self.state = P2pState::Disabled;
        self.discovered.clear();
        self.group = None;
        P2pAction::Disabled
    }
    
    /// Start discovery
    pub fn start_discovery(&mut self) -> P2pAction {
        if self.state == P2pState::Disabled {
            return P2pAction::None;
        }
        self.state = P2pState::Discovering;
        self.discovered.clear();
        P2pAction::StartDiscovery
    }
    
    /// Stop discovery
    pub fn stop_discovery(&mut self) -> P2pAction {
        if self.state == P2pState::Discovering {
            self.state = P2pState::Idle;
        }
        P2pAction::StopDiscovery
    }
    
    /// Add discovered device
    pub fn add_device(&mut self, device: P2pDevice) {
        if let Some(existing) = self.discovered.iter_mut()
            .find(|d| d.device_addr == device.device_addr)
        {
            *existing = device;
        } else {
            self.discovered.push(device);
        }
    }
    
    /// Connect to peer
    pub fn connect(&mut self, request: P2pConnectRequest) -> P2pAction {
        self.pending_request = Some(request.clone());
        self.state = P2pState::Negotiating;
        P2pAction::SendGoNegRequest(request)
    }
    
    /// Handle GO negotiation response
    pub fn on_go_neg_response(&mut self, peer: [u8; 6], intent: GoIntent, tie_breaker: bool) -> P2pAction {
        let our_intent = self.pending_request.as_ref()
            .map(|r| r.go_intent)
            .unwrap_or_default();
        
        let we_are_go = our_intent.wins_against(intent, !tie_breaker);
        
        self.state = P2pState::Forming;
        
        if we_are_go {
            P2pAction::BecomeGo
        } else {
            P2pAction::BecomeClient(peer)
        }
    }
    
    /// Group formed
    pub fn on_group_formed(&mut self, group: P2pGroup) {
        self.state = match group.role {
            P2pRole::GroupOwner => P2pState::GroupOwner,
            P2pRole::Client => P2pState::Client,
            P2pRole::Device => P2pState::Idle,
        };
        self.group = Some(group);
    }
    
    /// Group removed
    pub fn on_group_removed(&mut self) {
        self.state = P2pState::Idle;
        self.group = None;
    }
    
    pub fn state(&self) -> P2pState {
        self.state
    }
    
    pub fn discovered_devices(&self) -> &[P2pDevice] {
        &self.discovered
    }
    
    pub fn current_group(&self) -> Option<&P2pGroup> {
        self.group.as_ref()
    }
}

/// P2P action
#[derive(Debug, Clone)]
pub enum P2pAction {
    None,
    Disabled,
    SetListenChannel(u8),
    StartDiscovery,
    StopDiscovery,
    SendGoNegRequest(P2pConnectRequest),
    SendGoNegResponse { peer: [u8; 6], accept: bool },
    BecomeGo,
    BecomeClient([u8; 6]),
    StartWps(WpsMethod),
    GroupFormed,
    GroupRemoved,
}

/// P2P IE builder
pub struct P2pIeBuilder;

impl P2pIeBuilder {
    pub const OUI: [u8; 3] = [0x50, 0x6F, 0x9A];
    pub const OUI_TYPE: u8 = 9;
    
    // Attribute IDs
    pub const ATTR_STATUS: u8 = 0;
    pub const ATTR_MINOR_REASON: u8 = 1;
    pub const ATTR_CAPABILITY: u8 = 2;
    pub const ATTR_DEVICE_ID: u8 = 3;
    pub const ATTR_GO_INTENT: u8 = 4;
    pub const ATTR_CONFIG_TIMEOUT: u8 = 5;
    pub const ATTR_LISTEN_CHANNEL: u8 = 6;
    pub const ATTR_GROUP_BSSID: u8 = 7;
    pub const ATTR_INTENDED_INTERFACE_ADDR: u8 = 9;
    pub const ATTR_MANAGEABILITY: u8 = 10;
    pub const ATTR_CHANNEL_LIST: u8 = 11;
    pub const ATTR_DEVICE_INFO: u8 = 13;
    pub const ATTR_GROUP_INFO: u8 = 14;
    pub const ATTR_GROUP_ID: u8 = 15;
    pub const ATTR_OPERATING_CHANNEL: u8 = 17;
    
    /// Build P2P IE with device info
    pub fn device_info(device: &P2pDevice) -> Vec<u8> {
        let mut ie = Vec::with_capacity(64);
        
        // Vendor IE header
        ie.push(0xDD);  // Vendor Specific IE
        let len_pos = ie.len();
        ie.push(0);     // Length placeholder
        ie.extend_from_slice(&Self::OUI);
        ie.push(Self::OUI_TYPE);
        
        // Capability attribute
        ie.push(Self::ATTR_CAPABILITY);
        ie.extend_from_slice(&2u16.to_le_bytes());
        ie.push(device.capability);
        ie.push(device.group_capability);
        
        // Device Info attribute
        ie.push(Self::ATTR_DEVICE_INFO);
        let dev_info_len_pos = ie.len();
        ie.extend_from_slice(&0u16.to_le_bytes());  // Length placeholder
        
        // P2P Device Address
        ie.extend_from_slice(&device.device_addr);
        // Config Methods
        ie.extend_from_slice(&device.config_methods.0.to_be_bytes());
        // Primary Device Type (8 bytes)
        ie.extend_from_slice(&device.device_type.category_id().to_be_bytes());
        ie.extend_from_slice(&[0x00, 0x50, 0xF2, 0x04]);  // WFA OUI + subcat
        ie.extend_from_slice(&1u16.to_be_bytes());  // Subcategory
        // Number of secondary device types
        ie.push(0);
        // Device Name
        ie.extend_from_slice(&[0x10, 0x11]);  // Device Name attribute
        ie.extend_from_slice(&(device.name.len() as u16).to_be_bytes());
        ie.extend_from_slice(device.name.as_bytes());
        
        // Fix lengths
        let dev_info_len = ie.len() - dev_info_len_pos - 2;
        ie[dev_info_len_pos] = (dev_info_len & 0xFF) as u8;
        ie[dev_info_len_pos + 1] = ((dev_info_len >> 8) & 0xFF) as u8;
        
        ie[len_pos] = (ie.len() - len_pos - 1) as u8;
        
        ie
    }
    
    /// Build GO Negotiation Request frame body
    pub fn go_neg_request(
        intent: GoIntent,
        tie_breaker: bool,
        config_methods: ConfigMethods,
        device: &P2pDevice,
    ) -> Vec<u8> {
        let mut body = Vec::with_capacity(128);
        
        // P2P Public Action frame
        body.push(0x04);  // Category: Public Action
        body.push(0x09);  // Action: Vendor Specific
        body.extend_from_slice(&Self::OUI);
        body.push(Self::OUI_TYPE);
        body.push(P2pActionType::GoNegotiationRequest.subtype());
        body.push(0);     // Dialog Token
        
        // P2P IE
        body.extend_from_slice(&Self::device_info(device));
        
        // GO Intent attribute
        body.push(Self::ATTR_GO_INTENT);
        body.extend_from_slice(&1u16.to_le_bytes());
        body.push((intent.value() << 1) | (tie_breaker as u8));
        
        body
    }
}
