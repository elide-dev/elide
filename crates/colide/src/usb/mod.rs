// USB subsystem for Colide OS
// Provides USB host controller support for WiFi dongles and peripherals

pub mod host;

pub use host::{
    UsbHostController, UsbDevice, UsbDeviceDescriptor, UsbManager,
    UsbSpeed, UsbClass, UsbError, UsbSetupPacket,
    XhciController, EhciController,
    EndpointType, EndpointDirection, UsbEndpoint,
};
