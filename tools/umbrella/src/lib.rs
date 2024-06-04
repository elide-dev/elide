/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
#![allow(non_snake_case, dead_code)]

mod diagnostics;
mod tools;
mod transport;

use crate::tools::{ToolInfo, API_VERSION, LIB_VERSION, OXY_INFO, RUFF_INFO, UV_INFO};
use jni::objects::{JClass, JString};
use jni::sys::{jint, jobjectArray, jstring};
use jni::JNIEnv;
use lazy_static::lazy_static;
use std::collections::HashMap;

lazy_static! {
    static ref TOOL_MAP: HashMap<&'static str, &'static ToolInfo> = {
        let mut m: HashMap<&'static str, &ToolInfo> = HashMap::new();
        m.insert("uv", &UV_INFO);
        m.insert("oxy", &OXY_INFO);
        m.insert("ruff", &RUFF_INFO);
        m
    };
}

// -- Entrypoint Functions
fn supportedTools() -> Vec<&'static str> {
    TOOL_MAP.keys().map(|&x| x).collect()
}

fn runUvOnSingleFile(mut env: JNIEnv, file: &JString) -> jint {
    let input: String = env
        .get_string(&file)
        .expect("Couldn't get file string")
        .into();
    println!("Running uv on file: {}", input);
    0
}

fn runOxyOnSingleFile(mut env: JNIEnv, file: &JString) -> jint {
    let input: String = env
        .get_string(&file)
        .expect("Couldn't get file string")
        .into();
    println!("Running oxy on file: {}", input);
    0
}

fn runRuffOnSingleFile(mut env: JNIEnv, file: &JString) -> jint {
    let input: String = env
        .get_string(&file)
        .expect("Couldn't get file string")
        .into();
    println!("Running ruff on file: {}", input);
    return 0;
    // let checkCommand: CheckCommand = CheckCommand {
    //   files: vec![PathBuf::from(input)],
    //   // output_format: SerializationFormat::Json,
    // };
    // let result = check(
    //   checkCommand,
    //   RUFF_CFG.clone(),
    // );
    // if result.is_ok() {
    //   let exit: ExitStatus = result.unwrap();
    //   match exit {
    //     ExitStatus::Success => 0,
    //     ExitStatus::Failure => 1,
    //     ExitStatus::Error => 2,
    //   }
    // } else {
    //   1
    // }
}

// -- JNI Aliases

#[no_mangle]
pub extern "C" fn Java_dev_elide_cli_bridge_CliNativeBridge_libVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    env.new_string(LIB_VERSION).unwrap().into_raw()
}

#[no_mangle]
pub extern "C" fn Java_dev_elide_cli_bridge_CliNativeBridge_apiVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    env.new_string(API_VERSION).unwrap().into_raw()
}

#[no_mangle]
pub extern "C" fn Java_dev_elide_cli_bridge_CliNativeBridge_supportedTools<'local>(
    mut env: JNIEnv,
    _class: JClass,
) -> jobjectArray {
    let tools = supportedTools();
    let array = env
        .new_object_array(
            tools.len() as i32,
            "java/lang/String",
            env.new_string("").unwrap(),
        )
        .unwrap();

    for (i, tool) in tools.iter().enumerate() {
        let tool = env.new_string(*tool).unwrap();
        env.set_object_array_element(&array, i as i32, tool)
            .unwrap();
    }
    array.into_raw()
}

#[no_mangle]
pub extern "C" fn Java_dev_elide_cli_bridge_CliNativeBridge_relatesTo<'local>(
    mut env: JNIEnv,
    _class: JClass,
    tool: JString<'local>,
) -> jobjectArray {
    let input: String = env
        .get_string(&tool)
        .expect("Couldn't get tool string")
        .into();
    let toolInfo = TOOL_MAP.get(input.as_str());
    let tool = match toolInfo {
        Some(tool) => tool,
        None => panic!("Tool not found"),
    };
    let array = env
        .new_object_array(1, "java/lang/String", env.new_string("").unwrap())
        .unwrap();

    let tool = env.new_string(tool.language).unwrap();
    env.set_object_array_element(&array, 0, tool).unwrap();

    array.into_raw()
}

#[no_mangle]
pub extern "C" fn Java_dev_elide_cli_bridge_CliNativeBridge_toolVersion<'local>(
    mut env: JNIEnv,
    _class: JClass,
    tool: JString<'local>,
) -> jstring {
    let input: String = env
        .get_string(&tool)
        .expect("Couldn't get tool string")
        .into();
    let toolInfo = TOOL_MAP.get(input.as_str());
    let tool = match toolInfo {
        Some(tool) => tool,
        None => panic!("Tool not found"),
    };
    env.new_string(tool.version).unwrap().into_raw()
}

#[no_mangle]
pub extern "C" fn Java_dev_elide_cli_bridge_CliNativeBridge_runToolOnFile<'local>(
    mut env: JNIEnv,
    _class: JClass,
    tool: JString<'local>,
    file: JString<'local>,
) -> jint {
    let input: String = env
        .get_string(&tool)
        .expect("Couldn't get tool string")
        .into();
    let toolInfo = TOOL_MAP.get(input.as_str());
    let tool = match toolInfo {
        Some(tool) => tool,
        None => panic!("Tool not found"),
    };

    // switch by tool name
    match tool.name {
        "uv" => runUvOnSingleFile(env, &file),
        "oxy" => runOxyOnSingleFile(env, &file),
        "ruff" => runRuffOnSingleFile(env, &file),
        _ => 1,
    }
}
