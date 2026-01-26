//! 802.11s Mesh Networking Support
//!
//! Implements WiFi mesh networking for multi-hop ad-hoc networks
//! without central infrastructure.

/// Mesh state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum MeshState {
    #[default]
    Disabled,
    Idle,
    Scanning,
    Joining,
    Peering,
    Established,
}

/// Mesh configuration
#[derive(Debug, Clone)]
pub struct MeshConfig {
    /// Mesh ID (like SSID)
    pub mesh_id: Vec<u8>,
    /// Mesh channel
    pub channel: u8,
    /// Maximum number of peer links
    pub max_peer_links: u8,
    /// Path selection protocol
    pub path_protocol: PathProtocol,
    /// Path metric
    pub path_metric: PathMetric,
    /// Congestion control mode
    pub congestion_mode: CongestionMode,
    /// Enable mesh forwarding
    pub forwarding: bool,
    /// Enable mesh gating (root/portal)
    pub gate_announce: bool,
    /// Beacon interval (TUs)
    pub beacon_interval: u16,
    /// DTIM period
    pub dtim_period: u8,
}

impl Default for MeshConfig {
    fn default() -> Self {
        Self {
            mesh_id: Vec::new(),
            channel: 6,
            max_peer_links: 15,
            path_protocol: PathProtocol::Hwmp,
            path_metric: PathMetric::Airtime,
            congestion_mode: CongestionMode::None,
            forwarding: true,
            gate_announce: false,
            beacon_interval: 1000,
            dtim_period: 2,
        }
    }
}

/// Path selection protocol
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum PathProtocol {
    #[default]
    Hwmp,  // Hybrid Wireless Mesh Protocol
    VendorSpecific,
}

impl PathProtocol {
    pub fn oui(&self) -> [u8; 4] {
        match self {
            Self::Hwmp => [0x00, 0x0F, 0xAC, 0x01],
            Self::VendorSpecific => [0x00, 0x00, 0x00, 0x00],
        }
    }
}

/// Path metric type
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum PathMetric {
    #[default]
    Airtime,
    VendorSpecific,
}

impl PathMetric {
    pub fn oui(&self) -> [u8; 4] {
        match self {
            Self::Airtime => [0x00, 0x0F, 0xAC, 0x01],
            Self::VendorSpecific => [0x00, 0x00, 0x00, 0x00],
        }
    }
}

/// Congestion control mode
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum CongestionMode {
    #[default]
    None,
    SignalingBased,
    VendorSpecific,
}

/// Mesh peer link state
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum PeerLinkState {
    #[default]
    Idle,
    OpenSent,
    OpenReceived,
    ConfirmReceived,
    Established,
    Holding,
}

/// Mesh peer info
#[derive(Debug, Clone)]
pub struct MeshPeer {
    pub addr: [u8; 6],
    pub link_state: PeerLinkState,
    pub local_link_id: u16,
    pub peer_link_id: u16,
    pub signal: i8,
    pub metric: u32,
    pub last_beacon: u64,
    pub inactive_time: u64,
}

impl MeshPeer {
    pub fn new(addr: [u8; 6]) -> Self {
        Self {
            addr,
            link_state: PeerLinkState::Idle,
            local_link_id: 0,
            peer_link_id: 0,
            signal: -100,
            metric: u32::MAX,
            last_beacon: 0,
            inactive_time: 0,
        }
    }
}

/// Mesh path entry (routing table)
#[derive(Debug, Clone)]
pub struct MeshPath {
    pub dst: [u8; 6],
    pub next_hop: [u8; 6],
    pub metric: u32,
    pub sn: u32,  // Sequence number
    pub hop_count: u8,
    pub flags: PathFlags,
    pub expiry: u64,
    pub discovery_timeout: u64,
    pub discovery_retries: u8,
}

/// Path flags
#[derive(Debug, Clone, Copy, Default)]
pub struct PathFlags(u8);

impl PathFlags {
    pub const ACTIVE: u8 = 1 << 0;
    pub const RESOLVING: u8 = 1 << 1;
    pub const SN_VALID: u8 = 1 << 2;
    pub const FIXED: u8 = 1 << 3;
    
    pub fn is_active(&self) -> bool {
        (self.0 & Self::ACTIVE) != 0
    }
    
    pub fn is_resolving(&self) -> bool {
        (self.0 & Self::RESOLVING) != 0
    }
}

/// HWMP (Hybrid Wireless Mesh Protocol) action types
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum HwmpAction {
    PathRequest,
    PathReply,
    PathError,
    RootAnnouncement,
}

impl HwmpAction {
    pub fn element_id(&self) -> u8 {
        match self {
            Self::PathRequest => 130,
            Self::PathReply => 131,
            Self::PathError => 132,
            Self::RootAnnouncement => 133,
        }
    }
}

/// Path Request (PREQ) element
#[derive(Debug, Clone)]
pub struct PathRequest {
    pub flags: u8,
    pub hop_count: u8,
    pub ttl: u8,
    pub preq_id: u32,
    pub originator: [u8; 6],
    pub originator_sn: u32,
    pub lifetime: u32,
    pub metric: u32,
    pub target_count: u8,
    pub targets: Vec<PathTarget>,
}

/// Path target in PREQ
#[derive(Debug, Clone)]
pub struct PathTarget {
    pub flags: u8,
    pub addr: [u8; 6],
    pub sn: u32,
}

impl PathRequest {
    pub fn new(originator: [u8; 6], target: [u8; 6]) -> Self {
        Self {
            flags: 0,
            hop_count: 0,
            ttl: 31,
            preq_id: 0,
            originator,
            originator_sn: 0,
            lifetime: 0x1FFFFFFF,
            metric: 0,
            target_count: 1,
            targets: vec![PathTarget {
                flags: 0,
                addr: target,
                sn: 0,
            }],
        }
    }
    
    pub fn build(&self) -> Vec<u8> {
        let mut element = Vec::with_capacity(64);
        element.push(HwmpAction::PathRequest.element_id());
        let len_pos = element.len();
        element.push(0);  // Length placeholder
        
        element.push(self.flags);
        element.push(self.hop_count);
        element.push(self.ttl);
        element.extend_from_slice(&self.preq_id.to_le_bytes());
        element.extend_from_slice(&self.originator);
        element.extend_from_slice(&self.originator_sn.to_le_bytes());
        element.extend_from_slice(&self.lifetime.to_le_bytes());
        element.extend_from_slice(&self.metric.to_le_bytes());
        element.push(self.target_count);
        
        for target in &self.targets {
            element.push(target.flags);
            element.extend_from_slice(&target.addr);
            element.extend_from_slice(&target.sn.to_le_bytes());
        }
        
        element[len_pos] = (element.len() - len_pos - 1) as u8;
        element
    }
}

/// Path Reply (PREP) element
#[derive(Debug, Clone)]
pub struct PathReply {
    pub flags: u8,
    pub hop_count: u8,
    pub ttl: u8,
    pub target: [u8; 6],
    pub target_sn: u32,
    pub lifetime: u32,
    pub metric: u32,
    pub originator: [u8; 6],
    pub originator_sn: u32,
}

impl PathReply {
    pub fn build(&self) -> Vec<u8> {
        let mut element = Vec::with_capacity(48);
        element.push(HwmpAction::PathReply.element_id());
        let len_pos = element.len();
        element.push(0);  // Length placeholder
        
        element.push(self.flags);
        element.push(self.hop_count);
        element.push(self.ttl);
        element.extend_from_slice(&self.target);
        element.extend_from_slice(&self.target_sn.to_le_bytes());
        element.extend_from_slice(&self.lifetime.to_le_bytes());
        element.extend_from_slice(&self.metric.to_le_bytes());
        element.extend_from_slice(&self.originator);
        element.extend_from_slice(&self.originator_sn.to_le_bytes());
        
        element[len_pos] = (element.len() - len_pos - 1) as u8;
        element
    }
}

/// Path Error (PERR) element
#[derive(Debug, Clone)]
pub struct PathError {
    pub ttl: u8,
    pub destinations: Vec<PathErrorDest>,
}

#[derive(Debug, Clone)]
pub struct PathErrorDest {
    pub flags: u8,
    pub addr: [u8; 6],
    pub sn: u32,
    pub reason: u16,
}

impl PathError {
    pub fn build(&self) -> Vec<u8> {
        let mut element = Vec::with_capacity(32);
        element.push(HwmpAction::PathError.element_id());
        let len_pos = element.len();
        element.push(0);  // Length placeholder
        
        element.push(self.ttl);
        element.push(self.destinations.len() as u8);
        
        for dest in &self.destinations {
            element.push(dest.flags);
            element.extend_from_slice(&dest.addr);
            element.extend_from_slice(&dest.sn.to_le_bytes());
            element.extend_from_slice(&dest.reason.to_le_bytes());
        }
        
        element[len_pos] = (element.len() - len_pos - 1) as u8;
        element
    }
}

/// Mesh Manager
pub struct MeshManager {
    state: MeshState,
    config: MeshConfig,
    our_addr: [u8; 6],
    peers: Vec<MeshPeer>,
    paths: Vec<MeshPath>,
    preq_id: u32,
    our_sn: u32,
}

impl MeshManager {
    pub fn new(addr: [u8; 6]) -> Self {
        Self {
            state: MeshState::Disabled,
            config: MeshConfig::default(),
            our_addr: addr,
            peers: Vec::new(),
            paths: Vec::new(),
            preq_id: 0,
            our_sn: 0,
        }
    }
    
    /// Enable mesh with configuration
    pub fn enable(&mut self, config: MeshConfig) -> MeshAction {
        self.config = config;
        self.state = MeshState::Idle;
        MeshAction::SetMeshId(self.config.mesh_id.clone())
    }
    
    /// Disable mesh
    pub fn disable(&mut self) -> MeshAction {
        self.state = MeshState::Disabled;
        self.peers.clear();
        self.paths.clear();
        MeshAction::Disabled
    }
    
    /// Join existing mesh
    pub fn join(&mut self) -> MeshAction {
        self.state = MeshState::Scanning;
        MeshAction::StartScan
    }
    
    /// Add discovered mesh peer
    pub fn add_peer(&mut self, addr: [u8; 6], signal: i8) -> Option<MeshAction> {
        if self.peers.len() >= self.config.max_peer_links as usize {
            return None;
        }
        
        if self.peers.iter().any(|p| p.addr == addr) {
            return None;
        }
        
        let mut peer = MeshPeer::new(addr);
        peer.signal = signal;
        peer.local_link_id = self.generate_link_id();
        self.peers.push(peer);
        
        Some(MeshAction::SendPeerOpen(addr))
    }
    
    /// Process peer link open frame
    pub fn on_peer_open(&mut self, peer_addr: [u8; 6], peer_link_id: u16) -> MeshAction {
        let new_link_id = self.generate_link_id();
        
        if let Some(peer) = self.peers.iter_mut().find(|p| p.addr == peer_addr) {
            peer.peer_link_id = peer_link_id;
            match peer.link_state {
                PeerLinkState::Idle => {
                    peer.link_state = PeerLinkState::OpenReceived;
                    peer.local_link_id = new_link_id;
                    return MeshAction::SendPeerOpen(peer_addr);
                }
                PeerLinkState::OpenSent => {
                    peer.link_state = PeerLinkState::OpenReceived;
                    return MeshAction::SendPeerConfirm(peer_addr);
                }
                _ => {}
            }
        } else {
            // New peer
            let mut peer = MeshPeer::new(peer_addr);
            peer.peer_link_id = peer_link_id;
            peer.local_link_id = new_link_id;
            peer.link_state = PeerLinkState::OpenReceived;
            self.peers.push(peer);
            return MeshAction::SendPeerOpen(peer_addr);
        }
        MeshAction::None
    }
    
    /// Process peer link confirm frame
    pub fn on_peer_confirm(&mut self, peer_addr: [u8; 6]) -> MeshAction {
        if let Some(peer) = self.peers.iter_mut().find(|p| p.addr == peer_addr) {
            match peer.link_state {
                PeerLinkState::OpenSent | PeerLinkState::OpenReceived => {
                    peer.link_state = PeerLinkState::ConfirmReceived;
                    return MeshAction::SendPeerConfirm(peer_addr);
                }
                PeerLinkState::ConfirmReceived => {
                    peer.link_state = PeerLinkState::Established;
                    self.update_state();
                    return MeshAction::PeerEstablished(peer_addr);
                }
                _ => {}
            }
        }
        MeshAction::None
    }
    
    /// Process peer link close frame
    pub fn on_peer_close(&mut self, peer_addr: [u8; 6]) {
        self.peers.retain(|p| p.addr != peer_addr);
        // Invalidate paths using this peer
        for path in &mut self.paths {
            if path.next_hop == peer_addr {
                path.flags = PathFlags(PathFlags::RESOLVING);
            }
        }
        self.update_state();
    }
    
    /// Find path to destination
    pub fn find_path(&mut self, dst: [u8; 6]) -> MeshAction {
        // Check if path exists
        if let Some(path) = self.paths.iter().find(|p| p.dst == dst && p.flags.is_active()) {
            return MeshAction::PathFound(path.next_hop);
        }
        
        // Initiate path discovery
        self.preq_id += 1;
        self.our_sn += 1;
        
        let preq = PathRequest::new(self.our_addr, dst);
        MeshAction::SendPreq(preq)
    }
    
    /// Process Path Request
    pub fn on_preq(&mut self, preq: &PathRequest, from: [u8; 6]) -> Option<MeshAction> {
        // Update path to originator
        self.update_path(preq.originator, from, preq.metric, preq.originator_sn, preq.hop_count);
        
        // Check if we're the target
        for target in &preq.targets {
            if target.addr == self.our_addr {
                // Send PREP back
                self.our_sn += 1;
                let prep = PathReply {
                    flags: 0,
                    hop_count: 0,
                    ttl: 31,
                    target: self.our_addr,
                    target_sn: self.our_sn,
                    lifetime: 0x1FFFFFFF,
                    metric: 0,
                    originator: preq.originator,
                    originator_sn: preq.originator_sn,
                };
                return Some(MeshAction::SendPrep(prep, from));
            }
            
            // Check if we have a path to target
            if let Some(path) = self.paths.iter().find(|p| p.dst == target.addr && p.flags.is_active()) {
                if path.sn >= target.sn {
                    // We have a fresh path, send PREP
                    let prep = PathReply {
                        flags: 0,
                        hop_count: path.hop_count,
                        ttl: 31,
                        target: target.addr,
                        target_sn: path.sn,
                        lifetime: 0x1FFFFFFF,
                        metric: path.metric,
                        originator: preq.originator,
                        originator_sn: preq.originator_sn,
                    };
                    return Some(MeshAction::SendPrep(prep, from));
                }
            }
        }
        
        // Forward PREQ if TTL > 0
        if preq.ttl > 1 {
            let mut fwd_preq = preq.clone();
            fwd_preq.hop_count += 1;
            fwd_preq.ttl -= 1;
            fwd_preq.metric = self.add_metric(fwd_preq.metric, from);
            return Some(MeshAction::ForwardPreq(fwd_preq));
        }
        
        None
    }
    
    /// Process Path Reply
    pub fn on_prep(&mut self, prep: &PathReply, from: [u8; 6]) -> Option<MeshAction> {
        // Update path to target
        self.update_path(prep.target, from, prep.metric, prep.target_sn, prep.hop_count);
        
        // If we're the originator, path discovery complete
        if prep.originator == self.our_addr {
            return Some(MeshAction::PathFound(from));
        }
        
        // Forward PREP toward originator
        if prep.ttl > 1 {
            if let Some(path) = self.paths.iter().find(|p| p.dst == prep.originator && p.flags.is_active()) {
                let mut fwd_prep = prep.clone();
                fwd_prep.hop_count += 1;
                fwd_prep.ttl -= 1;
                fwd_prep.metric = self.add_metric(fwd_prep.metric, from);
                return Some(MeshAction::SendPrep(fwd_prep, path.next_hop));
            }
        }
        
        None
    }
    
    /// Update or add path entry
    fn update_path(&mut self, dst: [u8; 6], next_hop: [u8; 6], metric: u32, sn: u32, hop_count: u8) {
        if let Some(path) = self.paths.iter_mut().find(|p| p.dst == dst) {
            // Update if better or fresher
            if sn > path.sn || (sn == path.sn && metric < path.metric) {
                path.next_hop = next_hop;
                path.metric = metric;
                path.sn = sn;
                path.hop_count = hop_count;
                path.flags = PathFlags(PathFlags::ACTIVE | PathFlags::SN_VALID);
            }
        } else {
            self.paths.push(MeshPath {
                dst,
                next_hop,
                metric,
                sn,
                hop_count,
                flags: PathFlags(PathFlags::ACTIVE | PathFlags::SN_VALID),
                expiry: 0,
                discovery_timeout: 0,
                discovery_retries: 0,
            });
        }
    }
    
    /// Calculate airtime link metric
    fn add_metric(&self, current: u32, peer_addr: [u8; 6]) -> u32 {
        // Simplified airtime metric
        // Real implementation considers rate, error probability
        if let Some(peer) = self.peers.iter().find(|p| p.addr == peer_addr) {
            current.saturating_add(peer.metric)
        } else {
            current.saturating_add(1000)
        }
    }
    
    fn generate_link_id(&self) -> u16 {
        // Simple incrementing ID
        (self.peers.len() as u16).wrapping_add(1)
    }
    
    fn update_state(&mut self) {
        if self.peers.iter().any(|p| p.link_state == PeerLinkState::Established) {
            self.state = MeshState::Established;
        } else if !self.peers.is_empty() {
            self.state = MeshState::Peering;
        } else {
            self.state = MeshState::Idle;
        }
    }
    
    pub fn state(&self) -> MeshState {
        self.state
    }
    
    pub fn peers(&self) -> &[MeshPeer] {
        &self.peers
    }
    
    pub fn paths(&self) -> &[MeshPath] {
        &self.paths
    }
}

/// Mesh action
#[derive(Debug, Clone)]
pub enum MeshAction {
    None,
    Disabled,
    SetMeshId(Vec<u8>),
    StartScan,
    SendPeerOpen([u8; 6]),
    SendPeerConfirm([u8; 6]),
    SendPeerClose([u8; 6]),
    PeerEstablished([u8; 6]),
    SendPreq(PathRequest),
    ForwardPreq(PathRequest),
    SendPrep(PathReply, [u8; 6]),
    PathFound([u8; 6]),
    PathError(PathError),
}

/// Mesh IE builder
pub struct MeshIeBuilder;

impl MeshIeBuilder {
    /// Build Mesh ID element
    pub fn mesh_id(id: &[u8]) -> Vec<u8> {
        let mut ie = Vec::with_capacity(2 + id.len());
        ie.push(114);  // Mesh ID element ID
        ie.push(id.len() as u8);
        ie.extend_from_slice(id);
        ie
    }
    
    /// Build Mesh Configuration element
    pub fn mesh_config(config: &MeshConfig) -> Vec<u8> {
        let mut ie = Vec::with_capacity(16);
        ie.push(113);  // Mesh Configuration element ID
        ie.push(7);    // Length
        
        // Path Selection Protocol
        ie.push(config.path_protocol.oui()[3]);
        // Path Selection Metric
        ie.push(config.path_metric.oui()[3]);
        // Congestion Control Mode
        ie.push(config.congestion_mode as u8);
        // Synchronization Method
        ie.push(1);  // Neighbor offset
        // Authentication Protocol
        ie.push(0);  // No auth
        // Mesh Formation Info
        let mut formation = 0u8;
        if config.gate_announce { formation |= 0x01; }
        ie.push(formation);
        // Mesh Capability
        let mut cap = 0u8;
        if config.forwarding { cap |= 0x08; }
        ie.push(cap);
        
        ie
    }
}
