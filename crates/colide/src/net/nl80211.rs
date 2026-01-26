//! NL80211 User-Space Interface Shim for Colide OS
//!
//! NL80211 is the Linux netlink-based interface for WiFi configuration.
//! This module provides a compatibility layer allowing Linux WiFi tools
//! and libraries (like iw, wpa_supplicant) to work with Colide drivers.
//!
//! Reference: Linux kernel include/uapi/linux/nl80211.h

/// NL80211 command types (from linux/nl80211.h)
#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Nl80211Command {
    Unspec = 0,
    GetWiphy = 1,
    SetWiphy = 2,
    NewWiphy = 3,
    DelWiphy = 4,
    GetInterface = 5,
    SetInterface = 6,
    NewInterface = 7,
    DelInterface = 8,
    GetKey = 9,
    SetKey = 10,
    NewKey = 11,
    DelKey = 12,
    GetBeacon = 13,
    SetBeacon = 14,
    StartAp = 15,
    StopAp = 16,
    GetStation = 17,
    SetStation = 18,
    NewStation = 19,
    DelStation = 20,
    GetMpath = 21,
    SetMpath = 22,
    NewMpath = 23,
    DelMpath = 24,
    SetBss = 25,
    SetReg = 26,
    ReqSetReg = 27,
    GetMeshConfig = 28,
    SetMeshConfig = 29,
    SetMgmtExtraIe = 30,
    GetReg = 31,
    GetScan = 32,
    TriggerScan = 33,
    NewScanResults = 34,
    ScanAborted = 35,
    RegChange = 36,
    Authenticate = 37,
    Associate = 38,
    Deauthenticate = 39,
    Disassociate = 40,
    MichaelMicFailure = 41,
    RegBeaconHint = 42,
    JoinIbss = 43,
    LeaveIbss = 44,
    Testmode = 45,
    Connect = 46,
    Roam = 47,
    Disconnect = 48,
    SetWiphyNetns = 49,
    GetSurvey = 50,
    NewSurveyResults = 51,
    SetPmksa = 52,
    DelPmksa = 53,
    FlushPmksa = 54,
    RemainOnChannel = 55,
    CancelRemainOnChannel = 56,
    SetTxBitrateMask = 57,
    RegisterFrame = 58,
    Frame = 59,
    FrameTxStatus = 60,
    SetPowerSave = 61,
    GetPowerSave = 62,
    SetCqm = 63,
    NotifyCqm = 64,
    SetChannel = 65,
    SetWdsPeer = 66,
    FrameWaitCancel = 67,
    JoinMesh = 68,
    LeaveMesh = 69,
    UnprotDeauthenticate = 70,
    UnprotDisassociate = 71,
    NewPeerCandidate = 72,
    GetWowlan = 73,
    SetWowlan = 74,
    StartSchedScan = 75,
    StopSchedScan = 76,
    SchedScanResults = 77,
    SchedScanStopped = 78,
    SetRekeyOffload = 79,
    PmksaCandidate = 80,
    TdlsOper = 81,
    TdlsMgmt = 82,
    UnexpectedFrame = 83,
    ProbeClient = 84,
    RegisterBeacons = 85,
    Unexpected4AddrFrame = 86,
    SetNoackMap = 87,
    ChSwitchNotify = 88,
    StartP2pDevice = 89,
    StopP2pDevice = 90,
    ConnFailed = 91,
    SetMcastRate = 92,
    SetMacAcl = 93,
    RadarDetect = 94,
    GetProtocolFeatures = 95,
    UpdateFtIes = 96,
    FtEvent = 97,
    CritProtocolStart = 98,
    CritProtocolStop = 99,
    GetCoalesce = 100,
    SetCoalesce = 101,
    ChannelSwitch = 102,
    Vendor = 103,
    SetQosMap = 104,
    AddTxTs = 105,
    DelTxTs = 106,
    GetMpp = 107,
    JoinOcb = 108,
    LeaveOcb = 109,
    ChSwitchStartedNotify = 110,
    TdlsChannelSwitch = 111,
    TdlsCancelChannelSwitch = 112,
    WiphyRegChange = 113,
    AbortScan = 114,
    StartNan = 115,
    StopNan = 116,
    AddNanFunction = 117,
    DelNanFunction = 118,
    ChangeNanConfig = 119,
    NanMatch = 120,
    SetMulticastToUnicast = 121,
    UpdateConnectParams = 122,
    SetPmk = 123,
    DelPmk = 124,
    PortAuthorized = 125,
    ReloadRegdb = 126,
    ExternalAuth = 127,
    StaOpmodeChanged = 128,
    ControlPortFrame = 129,
    GetFtmResponderStats = 130,
    PeerMeasurementStart = 131,
    PeerMeasurementResult = 132,
    PeerMeasurementComplete = 133,
    NotifyRadar = 134,
    UpdateOweInfo = 135,
    ProbeMeshLink = 136,
    SetTidConfig = 137,
    UnprotBeacon = 138,
    ControlPortFrameTxStatus = 139,
    SetSarSpecs = 140,
    ObssColorCollision = 141,
    ColorChangeRequest = 142,
    ColorChangeStarted = 143,
    ColorChangeAborted = 144,
    ColorChangeCompleted = 145,
}

/// NL80211 attribute types
#[repr(u16)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Nl80211Attr {
    Unspec = 0,
    Wiphy = 1,
    WiphyName = 2,
    Ifindex = 3,
    Ifname = 4,
    Iftype = 5,
    Mac = 6,
    KeyData = 7,
    KeyIdx = 8,
    KeyCipher = 9,
    KeySeq = 10,
    KeyDefault = 11,
    BeaconInterval = 12,
    DtimPeriod = 13,
    BeaconHead = 14,
    BeaconTail = 15,
    StaAid = 16,
    StaFlags = 17,
    StaListenInterval = 18,
    StaSupportedRates = 19,
    StaVlan = 20,
    StaInfo = 21,
    WiphyBands = 22,
    MntrFlags = 23,
    MeshId = 24,
    StaPlinkAction = 25,
    MpathNextHop = 26,
    MpathInfo = 27,
    BssCtsProt = 28,
    BssShortPreamble = 29,
    BssShortSlotTime = 30,
    HtCapability = 31,
    SupportedIftypes = 32,
    RegAlpha2 = 33,
    RegRules = 34,
    MeshConfig = 35,
    BssBasicRates = 36,
    WiphyTxqParams = 37,
    WiphyFreq = 38,
    WiphyChannelType = 39,
    KeyDefaultMgmt = 40,
    MgmtSubtype = 41,
    Ie = 42,
    MaxNumScanSsids = 43,
    ScanFrequencies = 44,
    ScanSsids = 45,
    Generation = 46,
    Bss = 47,
    RegInitiator = 48,
    RegType = 49,
    SupportedCommands = 50,
    Frame = 51,
    Ssid = 52,
    AuthType = 53,
    ReasonCode = 54,
    KeyType = 55,
    MaxScanIeLen = 56,
    CipherSuites = 57,
    FreqBefore = 58,
    FreqAfter = 59,
    FreqFixed = 60,
    WiphyRetryShort = 61,
    WiphyRetryLong = 62,
    WiphyFragThreshold = 63,
    WiphyRtsThreshold = 64,
    TimedOut = 65,
    UseMfp = 66,
    StaFlags2 = 67,
    ControlPort = 68,
    Testdata = 69,
    Privacy = 70,
    DisconnectedByAp = 71,
    StatusCode = 72,
    CipherSuitesPairwise = 73,
    CipherSuiteGroup = 74,
    WpaVersions = 75,
    AkmSuites = 76,
    ReqIe = 77,
    RespIe = 78,
    PrevBssid = 79,
    Key = 80,
    Keys = 81,
    Pid = 82,
    FourAddr = 83,
    SurveyInfo = 84,
    Pmkid = 85,
    MaxNumPmkids = 86,
    Duration = 87,
    Cookie = 88,
    WiphyCoverageClass = 89,
    TxRates = 90,
    FrameMatch = 91,
    Ack = 92,
    PsState = 93,
    Cqm = 94,
    LocalStateChange = 95,
    ApIsolate = 96,
    WiphyTxPowerSetting = 97,
    WiphyTxPowerLevel = 98,
    TxFrameTypes = 99,
    RxFrameTypes = 100,
    FrameType = 101,
    ControlPortEthertype = 102,
    ControlPortNoEncrypt = 103,
    SupportIbssRsn = 104,
    WiphyAntennaTx = 105,
    WiphyAntennaRx = 106,
    McastRate = 107,
    OffchannelTxOk = 108,
    BssHtOpmode = 109,
    KeyDefaultTypes = 110,
    MaxRemainOnChannelDuration = 111,
    MeshSetup = 112,
    WiphyAntennaAvailTx = 113,
    WiphyAntennaAvailRx = 114,
    SupportMeshAuth = 115,
    StaPlinkState = 116,
    WowlanTriggers = 117,
    WowlanTriggersSupported = 118,
    SchedScanInterval = 119,
    InterfaceCombinations = 120,
    SoftwareIftypes = 121,
    RekeyData = 122,
    MaxNumSchedScanSsids = 123,
    MaxSchedScanIeLen = 124,
    ScanSuppRates = 125,
    HiddenSsid = 126,
    IeProbeResp = 127,
    IeAssocResp = 128,
    StaWme = 129,
    SupportApUapsd = 130,
    RoamSupport = 131,
    SchedScanMatch = 132,
    MaxMatchSets = 133,
    PmksaCandidate = 134,
    TxNoCckRate = 135,
    TdlsAction = 136,
    TdlsDialogToken = 137,
    TdlsOperation = 138,
    TdlsSupport = 139,
    TdlsExternalSetup = 140,
    DeviceApSme = 141,
    DontWaitForAck = 142,
    FeatureFlags = 143,
    ProbeRespOffload = 144,
    ProbeResp = 145,
    DfsRegion = 146,
    DisableHt = 147,
    HtCapabilityMask = 148,
    NoackMap = 149,
    InactivityTimeout = 150,
    RxSignalDbm = 151,
    BgScanPeriod = 152,
    Wdev = 153,
    UserRegHintType = 154,
    ConnFailedReason = 155,
    AuthData = 156,
    VhtCapability = 157,
    ScanFlags = 158,
    ChannelWidth = 159,
    CenterFreq1 = 160,
    CenterFreq2 = 161,
    P2pCtwindow = 162,
    P2pOppps = 163,
    LocalMeshPowerMode = 164,
    AclPolicy = 165,
    MacAddrs = 166,
    MacAclMax = 167,
    RadarEvent = 168,
    ExtCapa = 169,
    ExtCapaMask = 170,
    StaCapability = 171,
    StaExtCapability = 172,
    ProtocolFeatures = 173,
    SplitWiphyDump = 174,
    DisableVht = 175,
    VhtCapabilityMask = 176,
    Mdid = 177,
    IeRic = 178,
    CritProtId = 179,
    MaxCritProtDuration = 180,
    PeerAid = 181,
    CoalesceRule = 182,
    ChSwitchCount = 183,
    ChSwitchBlockTx = 184,
    CsaIes = 185,
    CsaCOffBeacon = 186,
    CsaCOffPresp = 187,
    RxmgmtFlags = 188,
    StaSupportedChannels = 189,
    StaSupportedOperClasses = 190,
    HandleDfs = 191,
    Support5Mhz = 192,
    Support10Mhz = 193,
    OpmodeNotif = 194,
    VendorId = 195,
    VendorSubcmd = 196,
    VendorData = 197,
    VendorEvents = 198,
    QosMap = 199,
}

/// Interface types
#[repr(u32)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Nl80211Iftype {
    Unspecified = 0,
    Adhoc = 1,
    Station = 2,
    Ap = 3,
    ApVlan = 4,
    Wds = 5,
    Monitor = 6,
    MeshPoint = 7,
    P2pClient = 8,
    P2pGo = 9,
    P2pDevice = 10,
    Ocb = 11,
    Nan = 12,
}

/// Authentication types
#[repr(u32)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Nl80211AuthType {
    OpenSystem = 0,
    SharedKey = 1,
    Ft = 2,
    NetworkEap = 3,
    Sae = 4,
    FilsSk = 5,
    FilsSkPfs = 6,
    FilsPk = 7,
}

/// Key types
#[repr(u32)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum Nl80211KeyType {
    Group = 0,
    Pairwise = 1,
    Peerkey = 2,
}

/// Cipher suites (from WLAN_CIPHER_SUITE_*)
pub mod CipherSuite {
    pub const USE_GROUP: u32 = 0x000FAC00;
    pub const WEP40: u32 = 0x000FAC01;
    pub const TKIP: u32 = 0x000FAC02;
    pub const CCMP: u32 = 0x000FAC04;
    pub const WEP104: u32 = 0x000FAC05;
    pub const AES_CMAC: u32 = 0x000FAC06;
    pub const GCMP: u32 = 0x000FAC08;
    pub const GCMP_256: u32 = 0x000FAC09;
    pub const CCMP_256: u32 = 0x000FAC0A;
    pub const BIP_GMAC_128: u32 = 0x000FAC0B;
    pub const BIP_GMAC_256: u32 = 0x000FAC0C;
    pub const BIP_CMAC_256: u32 = 0x000FAC0D;
}

/// AKM (Authentication and Key Management) suites
pub mod AkmSuite {
    pub const _8021X: u32 = 0x000FAC01;
    pub const PSK: u32 = 0x000FAC02;
    pub const FT_8021X: u32 = 0x000FAC03;
    pub const FT_PSK: u32 = 0x000FAC04;
    pub const _8021X_SHA256: u32 = 0x000FAC05;
    pub const PSK_SHA256: u32 = 0x000FAC06;
    pub const TDLS: u32 = 0x000FAC07;
    pub const SAE: u32 = 0x000FAC08;
    pub const FT_SAE: u32 = 0x000FAC09;
    pub const AP_PEER_KEY: u32 = 0x000FAC0A;
    pub const _8021X_SUITE_B: u32 = 0x000FAC0B;
    pub const _8021X_SUITE_B_192: u32 = 0x000FAC0C;
    pub const FT_8021X_SHA384: u32 = 0x000FAC0D;
    pub const FILS_SHA256: u32 = 0x000FAC0E;
    pub const FILS_SHA384: u32 = 0x000FAC0F;
    pub const FT_FILS_SHA256: u32 = 0x000FAC10;
    pub const FT_FILS_SHA384: u32 = 0x000FAC11;
    pub const OWE: u32 = 0x000FAC12;
}

/// Scan flags
pub mod ScanFlags {
    pub const LOW_PRIORITY: u32 = 1 << 0;
    pub const FLUSH: u32 = 1 << 1;
    pub const AP: u32 = 1 << 2;
    pub const RANDOM_ADDR: u32 = 1 << 3;
    pub const FILS_MAX_CHANNEL_TIME: u32 = 1 << 4;
    pub const ACCEPT_BCAST_PROBE_RESP: u32 = 1 << 5;
    pub const OCE_PROBE_REQ_HIGH_TX_RATE: u32 = 1 << 6;
    pub const OCE_PROBE_REQ_DEFERRAL_SUPPRESSION: u32 = 1 << 7;
    pub const LOW_SPAN: u32 = 1 << 8;
    pub const LOW_POWER: u32 = 1 << 9;
    pub const HIGH_ACCURACY: u32 = 1 << 10;
    pub const RANDOM_SN: u32 = 1 << 11;
    pub const MIN_PREQ_CONTENT: u32 = 1 << 12;
}

/// BSS (Basic Service Set) information element IDs
pub mod BssAttr {
    pub const BSSID: u16 = 1;
    pub const FREQUENCY: u16 = 2;
    pub const TSF: u16 = 3;
    pub const BEACON_INTERVAL: u16 = 4;
    pub const CAPABILITY: u16 = 5;
    pub const INFORMATION_ELEMENTS: u16 = 6;
    pub const SIGNAL_MBM: u16 = 7;
    pub const SIGNAL_UNSPEC: u16 = 8;
    pub const STATUS: u16 = 9;
    pub const SEEN_MS_AGO: u16 = 10;
    pub const BEACON_IES: u16 = 11;
    pub const CHAN_WIDTH: u16 = 12;
    pub const BEACON_TSF: u16 = 13;
    pub const PRESP_DATA: u16 = 14;
    pub const LAST_SEEN_BOOTTIME: u16 = 15;
    pub const PAD: u16 = 16;
    pub const PARENT_TSF: u16 = 17;
    pub const PARENT_BSSID: u16 = 18;
    pub const CHAIN_SIGNAL: u16 = 19;
}

/// NL80211 message header
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct Nl80211MsgHeader {
    pub cmd: u8,
    pub version: u8,
    pub reserved: u16,
}

/// NL80211 attribute header
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct Nl80211AttrHeader {
    pub len: u16,
    pub attr_type: u16,
}

/// NL80211 request handler result
#[derive(Debug)]
pub enum Nl80211Result {
    Ok,
    Error(i32),
    Dump(Vec<u8>),
}

/// NL80211 handler trait for driver integration
pub trait Nl80211Handler {
    /// Handle GET_WIPHY command
    fn get_wiphy(&self, wiphy_idx: u32) -> Nl80211Result;
    
    /// Handle GET_INTERFACE command
    fn get_interface(&self, if_index: u32) -> Nl80211Result;
    
    /// Handle TRIGGER_SCAN command
    fn trigger_scan(&mut self, ssids: &[[u8; 32]], frequencies: &[u32]) -> Nl80211Result;
    
    /// Handle GET_SCAN command
    fn get_scan(&self, if_index: u32) -> Nl80211Result;
    
    /// Handle CONNECT command
    fn connect(&mut self, ssid: &[u8], bssid: Option<[u8; 6]>, 
               auth_type: Nl80211AuthType, key: Option<&[u8]>) -> Nl80211Result;
    
    /// Handle DISCONNECT command
    fn disconnect(&mut self, reason_code: u16) -> Nl80211Result;
    
    /// Handle SET_KEY command
    fn set_key(&mut self, key_idx: u8, key_type: Nl80211KeyType, 
               cipher: u32, key_data: &[u8]) -> Nl80211Result;
    
    /// Handle DEL_KEY command
    fn del_key(&mut self, key_idx: u8) -> Nl80211Result;
    
    /// Handle SET_POWER_SAVE command
    fn set_power_save(&mut self, enabled: bool) -> Nl80211Result;
    
    /// Handle GET_POWER_SAVE command
    fn get_power_save(&self) -> Nl80211Result;
}

/// Linux error codes (subset for nl80211)
pub mod Errno {
    pub const ENODEV: i32 = 19;
    pub const EINVAL: i32 = 22;
    pub const ENOENT: i32 = 2;
    pub const EBUSY: i32 = 16;
    pub const EOPNOTSUPP: i32 = 95;
}

/// Default no-op implementation for testing
pub struct Nl80211Stub;

impl Nl80211Handler for Nl80211Stub {
    fn get_wiphy(&self, _wiphy_idx: u32) -> Nl80211Result {
        Nl80211Result::Error(-Errno::ENODEV)
    }
    
    fn get_interface(&self, _if_index: u32) -> Nl80211Result {
        Nl80211Result::Error(-Errno::ENODEV)
    }
    
    fn trigger_scan(&mut self, _ssids: &[[u8; 32]], _frequencies: &[u32]) -> Nl80211Result {
        Nl80211Result::Error(-Errno::ENODEV)
    }
    
    fn get_scan(&self, _if_index: u32) -> Nl80211Result {
        Nl80211Result::Error(-Errno::ENODEV)
    }
    
    fn connect(&mut self, _ssid: &[u8], _bssid: Option<[u8; 6]>,
               _auth_type: Nl80211AuthType, _key: Option<&[u8]>) -> Nl80211Result {
        Nl80211Result::Error(-Errno::ENODEV)
    }
    
    fn disconnect(&mut self, _reason_code: u16) -> Nl80211Result {
        Nl80211Result::Error(-Errno::ENODEV)
    }
    
    fn set_key(&mut self, _key_idx: u8, _key_type: Nl80211KeyType,
               _cipher: u32, _key_data: &[u8]) -> Nl80211Result {
        Nl80211Result::Error(-Errno::ENODEV)
    }
    
    fn del_key(&mut self, _key_idx: u8) -> Nl80211Result {
        Nl80211Result::Error(-Errno::ENODEV)
    }
    
    fn set_power_save(&mut self, _enabled: bool) -> Nl80211Result {
        Nl80211Result::Error(-Errno::ENODEV)
    }
    
    fn get_power_save(&self) -> Nl80211Result {
        Nl80211Result::Error(-Errno::ENODEV)
    }
}
