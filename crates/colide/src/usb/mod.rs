// USB subsystem for Colide OS
// Provides USB host controller support for WiFi dongles and peripherals

pub mod host;
pub mod mass_storage;
pub mod hid;

pub use host::{
    UsbHostController, UsbDevice, UsbDeviceDescriptor, UsbManager,
    UsbSpeed, UsbClass, UsbError, UsbSetupPacket,
    XhciController, EhciController,
    EndpointType, EndpointDirection, UsbEndpoint,
};

pub use mass_storage::{UsbMassStorage, UsbStorageBlockDevice};
pub use hid::{UsbKeyboard, UsbMouse, KeyboardEvent, MouseEvent};
