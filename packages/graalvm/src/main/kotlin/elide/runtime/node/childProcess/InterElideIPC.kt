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
package elide.runtime.node.childProcess

import org.graalvm.polyglot.Value
import java.net.MalformedURLException
import java.rmi.Naming
import java.rmi.Remote
import java.rmi.RemoteException
import java.rmi.registry.LocateRegistry
import java.rmi.server.UnicastRemoteObject
import kotlinx.atomicfu.atomic
import elide.runtime.Logging
import elide.runtime.intrinsics.js.node.childProcess.ProcessChannel

// Default RMI bind port for IPC.
private const val RMI_BIND_PORT: Int = 0
private const val RMI_LOCATOR_PORT: Int = 1337

// Resolve an IPC URL given a process ID.
private fun urlForPid(pid: Long): String = "//elide-$pid/ElideIPC"

// Shared IPC logger.
private val logger by lazy { Logging.of(InterElideIPC::class) }

/**
 * ## Inter-Elide IPC
 *
 * Implements a [ProcessChannel] for forked Elide processes; when a fork is created and a process channel is opened, the
 * parent and child processes can communicate with each other using this channel, similar to Node's IPC.
 */
internal interface InterElideIPC : ProcessChannel, Remote {
  /**
   * Initialize an inter-Elide IPC channel.
   *
   * @param port Symbolic port to use for the IPC channel locator.
   */
  fun initialize(port: Long = RMI_BIND_PORT.toLong()): InterElideIPC
}

// Whether RMI services initialized yet (server-side).
private val rmiServerInitialized = atomic(false)

// Implementation of `InterElideIPC` as a `ProcessChannel`.
internal class InterElideIPCClient private constructor(private val rpc: InterElideIPC) : InterElideIPC by rpc {
  companion object {
    /**
     * Connect to an inter-Elide IPC channel at the provided process ID.
     *
     * @param pid Process ID to connect to.
     * @return The connected IPC channel.
     */
    @JvmStatic fun connect(pid: Long): InterElideIPCClient = urlForPid(pid).let { url ->
      try {
        InterElideIPCClient(Naming.lookup(url) as InterElideIPC)
      } catch (rxe: RemoteException) {
        // registry already exists
        logger.trace("IPC lookup failed at URL '$url'")
        throw rxe
      } catch (nfe: MalformedURLException) {
        logger.error("Malformed URL for IPC registry at port $RMI_LOCATOR_PORT: '$url'")
        throw nfe
      }
    }
  }
}

// Implementation of `InterElideIPC` as a `ProcessChannel`.
internal class InterElideIPCServer : InterElideIPC, UnicastRemoteObject(RMI_BIND_PORT) {
  override fun initialize(port: Long): InterElideIPC = apply {
    // guard for single init
    if (rmiServerInitialized.value) {
      return@apply
    }
    rmiServerInitialized.value = true

    (try {
      LocateRegistry.createRegistry(RMI_LOCATOR_PORT)
    } catch (rxe: RemoteException) {
      // registry already exists
      logger.trace("IPC registry already exists", rxe)
    }).also {
      urlForPid(ProcessHandle.current().pid()).let { url ->
        try {
          Naming.rebind(url, this)
        } catch (rxe: RemoteException) {
          logger.error("Failed to bind IPC registry at URL '$url'", rxe)
          throw rxe
        } catch (nfe: MalformedURLException) {
          logger.error("Malformed URL for IPC registry at port $RMI_LOCATOR_PORT: '$url'", nfe)
          throw nfe
        }
      }
    }
  }

  override fun ref(): InterElideIPC {
    TODO("Not yet implemented: `InterElideRPC.ref`")
  }

  override fun unref(): InterElideIPC {
    TODO("Not yet implemented: `InterElideRPC.unref`")
  }

  override fun send(message: Value, sendHandle: Value?, options: Value?, callback: Value?): Boolean {
    TODO("Not yet implemented: `InterElideRPC.send`")
  }
}
