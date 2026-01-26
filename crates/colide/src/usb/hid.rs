/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

//! # USB HID Class Driver
//!
//! Implements the USB Human Interface Device (HID) class for keyboards, mice,
//! and other input devices.

use super::host::{UsbHostController, UsbDevice, UsbError, UsbSetupPacket, EndpointDirection};
use std::sync::{Arc, Mutex};
use std::collections::VecDeque;

/// HID subclass codes.
#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum HidSubclass {
    NoSubclass = 0x00,
    BootInterface = 0x01,
}

/// HID protocol codes.
#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum HidProtocol {
    None = 0x00,
    Keyboard = 0x01,
    Mouse = 0x02,
}

/// HID report types.
#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum HidReportType {
    Input = 0x01,
    Output = 0x02,
    Feature = 0x03,
}

/// HID class requests.
pub mod hid_request {
    pub const GET_REPORT: u8 = 0x01;
    pub const GET_IDLE: u8 = 0x02;
    pub const GET_PROTOCOL: u8 = 0x03;
    pub const SET_REPORT: u8 = 0x09;
    pub const SET_IDLE: u8 = 0x0A;
    pub const SET_PROTOCOL: u8 = 0x0B;
}

/// Keyboard modifier keys.
pub mod keyboard_mod {
    pub const LEFT_CTRL: u8 = 0x01;
    pub const LEFT_SHIFT: u8 = 0x02;
    pub const LEFT_ALT: u8 = 0x04;
    pub const LEFT_GUI: u8 = 0x08;
    pub const RIGHT_CTRL: u8 = 0x10;
    pub const RIGHT_SHIFT: u8 = 0x20;
    pub const RIGHT_ALT: u8 = 0x40;
    pub const RIGHT_GUI: u8 = 0x80;
}

/// Keyboard LED states.
pub mod keyboard_led {
    pub const NUM_LOCK: u8 = 0x01;
    pub const CAPS_LOCK: u8 = 0x02;
    pub const SCROLL_LOCK: u8 = 0x04;
    pub const COMPOSE: u8 = 0x08;
    pub const KANA: u8 = 0x10;
}

/// Keyboard scan code to ASCII mapping (US layout).
const KEYCODE_TO_ASCII: [u8; 128] = [
    0, 0, 0, 0,                                                                 // 0-3
    b'a', b'b', b'c', b'd', b'e', b'f', b'g', b'h', b'i', b'j', b'k', b'l', b'm', // 4-16
    b'n', b'o', b'p', b'q', b'r', b's', b't', b'u', b'v', b'w', b'x', b'y', b'z', // 17-29
    b'1', b'2', b'3', b'4', b'5', b'6', b'7', b'8', b'9', b'0',                  // 30-39
    b'\n', 27, 8, b'\t', b' ',                                                   // 40-44
    b'-', b'=', b'[', b']', b'\\',                                               // 45-49
    0, b';', b'\'', b'`', b',', b'.', b'/',                                      // 50-56
    0,                                                                           // 57: Caps Lock
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,                                          // 58-69: F1-F12
    0, 0, 0,                                                                     // 70-72: PrtSc, ScrLk, Pause
    0, 0, 0, 0,                                                                  // 73-76: Ins, Home, PgUp, Del
    0, 0,                                                                        // 77-78: End, PgDn
    0, 0, 0, 0,                                                                  // 79-82: Arrows
    0,                                                                           // 83: Num Lock
    b'/', b'*', b'-', b'+', b'\n',                                               // 84-88
    b'1', b'2', b'3', b'4', b'5', b'6', b'7', b'8', b'9', b'0', b'.',             // 89-99
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 100-127
];

const KEYCODE_TO_ASCII_SHIFT: [u8; 128] = [
    0, 0, 0, 0,                                                                 // 0-3
    b'A', b'B', b'C', b'D', b'E', b'F', b'G', b'H', b'I', b'J', b'K', b'L', b'M', // 4-16
    b'N', b'O', b'P', b'Q', b'R', b'S', b'T', b'U', b'V', b'W', b'X', b'Y', b'Z', // 17-29
    b'!', b'@', b'#', b'$', b'%', b'^', b'&', b'*', b'(', b')',                  // 30-39
    b'\n', 27, 8, b'\t', b' ',                                                   // 40-44
    b'_', b'+', b'{', b'}', b'|',                                                // 45-49
    0, b':', b'"', b'~', b'<', b'>', b'?',                                       // 50-56
    0,                                                                           // 57: Caps Lock
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,                                          // 58-69: F1-F12
    0, 0, 0,                                                                     // 70-72: PrtSc, ScrLk, Pause
    0, 0, 0, 0,                                                                  // 73-76: Ins, Home, PgUp, Del
    0, 0,                                                                        // 77-78: End, PgDn
    0, 0, 0, 0,                                                                  // 79-82: Arrows
    0,                                                                           // 83: Num Lock
    b'/', b'*', b'-', b'+', b'\n',                                               // 84-88
    b'1', b'2', b'3', b'4', b'5', b'6', b'7', b'8', b'9', b'0', b'.',             // 89-99
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 100-127
];

/// Keyboard event.
#[derive(Debug, Clone, Copy)]
pub struct KeyboardEvent {
    pub keycode: u8,
    pub modifiers: u8,
    pub pressed: bool,
    pub ascii: Option<char>,
}

/// Mouse event.
#[derive(Debug, Clone, Copy)]
pub struct MouseEvent {
    pub buttons: u8,
    pub dx: i8,
    pub dy: i8,
    pub wheel: i8,
}

/// USB HID keyboard device.
pub struct UsbKeyboard {
    controller: Arc<Mutex<dyn UsbHostController>>,
    device_address: u8,
    interrupt_endpoint: u8,
    interface_num: u8,
    led_state: u8,
    last_report: [u8; 8],
    event_queue: VecDeque<KeyboardEvent>,
}

impl UsbKeyboard {
    /// Create a new USB keyboard device.
    pub fn new(
        controller: Arc<Mutex<dyn UsbHostController>>,
        device: &UsbDevice,
    ) -> Result<Self, UsbError> {
        let mut interrupt_endpoint = None;
        let mut interface_num = 0;
        
        for config in &device.configurations {
            for iface in &config.interfaces {
                if iface.interface_class == 0x03 && iface.interface_protocol == HidProtocol::Keyboard as u8 {
                    interface_num = iface.interface_number;
                    for ep in &iface.endpoints {
                        if ep.direction == EndpointDirection::In {
                            interrupt_endpoint = Some(ep.address | 0x80);
                            break;
                        }
                    }
                }
            }
        }
        
        let interrupt_endpoint = interrupt_endpoint.ok_or(UsbError::InvalidEndpoint)?;
        
        let mut kbd = Self {
            controller,
            device_address: device.address,
            interrupt_endpoint,
            interface_num,
            led_state: 0,
            last_report: [0; 8],
            event_queue: VecDeque::new(),
        };
        
        // Set boot protocol
        kbd.set_protocol(0)?;
        
        // Set idle rate to 0 (report only on change)
        kbd.set_idle(0)?;
        
        Ok(kbd)
    }
    
    /// Set HID protocol (0 = boot, 1 = report).
    fn set_protocol(&self, protocol: u8) -> Result<(), UsbError> {
        let mut ctrl = self.controller.lock().unwrap();
        
        let setup = UsbSetupPacket {
            request_type: 0x21, // Host to device, class, interface
            request: hid_request::SET_PROTOCOL,
            value: protocol as u16,
            index: self.interface_num as u16,
            length: 0,
        };
        
        ctrl.control_transfer(self.device_address, &setup, None, 1000)?;
        Ok(())
    }
    
    /// Set idle rate.
    fn set_idle(&self, rate: u8) -> Result<(), UsbError> {
        let mut ctrl = self.controller.lock().unwrap();
        
        let setup = UsbSetupPacket {
            request_type: 0x21,
            request: hid_request::SET_IDLE,
            value: (rate as u16) << 8,
            index: self.interface_num as u16,
            length: 0,
        };
        
        ctrl.control_transfer(self.device_address, &setup, None, 1000)?;
        Ok(())
    }
    
    /// Set keyboard LEDs.
    pub fn set_leds(&mut self, leds: u8) -> Result<(), UsbError> {
        self.led_state = leds;
        
        let mut ctrl = self.controller.lock().unwrap();
        
        let setup = UsbSetupPacket {
            request_type: 0x21,
            request: hid_request::SET_REPORT,
            value: ((HidReportType::Output as u16) << 8) | 0,
            index: self.interface_num as u16,
            length: 1,
        };
        
        let mut data = [leds];
        ctrl.control_transfer(self.device_address, &setup, Some(&mut data), 1000)?;
        Ok(())
    }
    
    /// Poll for keyboard input.
    pub fn poll(&mut self) -> Result<(), UsbError> {
        let mut report = [0u8; 8];
        
        {
            let mut ctrl = self.controller.lock().unwrap();
            match ctrl.interrupt_transfer(self.device_address, self.interrupt_endpoint, &mut report, 10) {
                Ok(_) => {},
                Err(UsbError::Timeout) => return Ok(()), // No data
                Err(e) => return Err(e),
            }
        }
        
        // Process report
        let modifiers = report[0];
        
        // Check for released keys
        for &old_key in &self.last_report[2..8] {
            if old_key != 0 && !report[2..8].contains(&old_key) {
                let ascii = self.keycode_to_ascii(old_key, modifiers);
                self.event_queue.push_back(KeyboardEvent {
                    keycode: old_key,
                    modifiers,
                    pressed: false,
                    ascii,
                });
            }
        }
        
        // Check for pressed keys
        for &new_key in &report[2..8] {
            if new_key != 0 && !self.last_report[2..8].contains(&new_key) {
                let ascii = self.keycode_to_ascii(new_key, modifiers);
                self.event_queue.push_back(KeyboardEvent {
                    keycode: new_key,
                    modifiers,
                    pressed: true,
                    ascii,
                });
            }
        }
        
        self.last_report = report;
        Ok(())
    }
    
    /// Convert keycode to ASCII.
    fn keycode_to_ascii(&self, keycode: u8, modifiers: u8) -> Option<char> {
        if keycode >= 128 {
            return None;
        }
        
        let shifted = (modifiers & (keyboard_mod::LEFT_SHIFT | keyboard_mod::RIGHT_SHIFT)) != 0;
        let table = if shifted { &KEYCODE_TO_ASCII_SHIFT } else { &KEYCODE_TO_ASCII };
        
        let ch = table[keycode as usize];
        if ch != 0 {
            Some(ch as char)
        } else {
            None
        }
    }
    
    /// Get next keyboard event.
    pub fn get_event(&mut self) -> Option<KeyboardEvent> {
        self.event_queue.pop_front()
    }
    
    /// Check if there are pending events.
    pub fn has_events(&self) -> bool {
        !self.event_queue.is_empty()
    }
}

/// USB HID mouse device.
pub struct UsbMouse {
    controller: Arc<Mutex<dyn UsbHostController>>,
    device_address: u8,
    interrupt_endpoint: u8,
    buttons: u8,
    x: i32,
    y: i32,
    event_queue: VecDeque<MouseEvent>,
}

impl UsbMouse {
    /// Create a new USB mouse device.
    pub fn new(
        controller: Arc<Mutex<dyn UsbHostController>>,
        device: &UsbDevice,
    ) -> Result<Self, UsbError> {
        let mut interrupt_endpoint = None;
        let mut interface_num = 0;
        
        for config in &device.configurations {
            for iface in &config.interfaces {
                if iface.interface_class == 0x03 && iface.interface_protocol == HidProtocol::Mouse as u8 {
                    interface_num = iface.interface_number;
                    for ep in &iface.endpoints {
                        if ep.direction == EndpointDirection::In {
                            interrupt_endpoint = Some(ep.address | 0x80);
                            break;
                        }
                    }
                }
            }
        }
        
        let interrupt_endpoint = interrupt_endpoint.ok_or(UsbError::InvalidEndpoint)?;
        
        let mouse = Self {
            controller,
            device_address: device.address,
            interrupt_endpoint,
            buttons: 0,
            x: 0,
            y: 0,
            event_queue: VecDeque::new(),
        };
        
        Ok(mouse)
    }
    
    /// Poll for mouse input.
    pub fn poll(&mut self) -> Result<(), UsbError> {
        let mut report = [0u8; 4];
        
        {
            let mut ctrl = self.controller.lock().unwrap();
            match ctrl.interrupt_transfer(self.device_address, self.interrupt_endpoint, &mut report, 10) {
                Ok(_) => {},
                Err(UsbError::Timeout) => return Ok(()),
                Err(e) => return Err(e),
            }
        }
        
        let buttons = report[0];
        let dx = report[1] as i8;
        let dy = report[2] as i8;
        let wheel = if report.len() > 3 { report[3] as i8 } else { 0 };
        
        self.buttons = buttons;
        self.x = self.x.saturating_add(dx as i32);
        self.y = self.y.saturating_add(dy as i32);
        
        self.event_queue.push_back(MouseEvent {
            buttons,
            dx,
            dy,
            wheel,
        });
        
        Ok(())
    }
    
    /// Get next mouse event.
    pub fn get_event(&mut self) -> Option<MouseEvent> {
        self.event_queue.pop_front()
    }
    
    /// Get current button state.
    pub fn buttons(&self) -> u8 {
        self.buttons
    }
    
    /// Get current X position.
    pub fn x(&self) -> i32 {
        self.x
    }
    
    /// Get current Y position.
    pub fn y(&self) -> i32 {
        self.y
    }
    
    /// Set position.
    pub fn set_position(&mut self, x: i32, y: i32) {
        self.x = x;
        self.y = y;
    }
}
