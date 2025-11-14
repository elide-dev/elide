import { useEffect, useRef, useState } from 'react'
import { Terminal as XTerm } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import '@xterm/xterm/css/xterm.css'
import type { BuildTool } from '@shared/types'

interface TerminalProps {
  jobId: string
  tool: BuildTool
  onBellRung?: () => void
}

export function Terminal({ jobId, tool, onBellRung }: TerminalProps) {
  const terminalRef = useRef<HTMLDivElement>(null)
  const xtermRef = useRef<XTerm | null>(null)
  const fitAddonRef = useRef<FitAddon | null>(null)
  const wsRef = useRef<WebSocket | null>(null)
  const [bellCount, setBellCount] = useState(0)
  const [keystrokeCount, setKeystrokeCount] = useState(0)
  const [buildStartTime, setBuildStartTime] = useState<number | null>(null)
  const [buildDuration, setBuildDuration] = useState<number | null>(null)
  const [elapsedTime, setElapsedTime] = useState<number>(0)
  const timerIntervalRef = useRef<NodeJS.Timeout | null>(null)
  const KEYSTROKE_LIMIT = 1000

  useEffect(() => {
    if (!terminalRef.current) return

    // Initialize terminal
    const term = new XTerm({
      cursorBlink: true,
      fontSize: 13,
      fontFamily: 'Menlo, Monaco, "Courier New", monospace',
      theme: {
        background: '#1e293b',
        foreground: '#e2e8f0',
        cursor: '#6366f1',
      },
      convertEol: true,
    })

    const fitAddon = new FitAddon()
    term.loadAddon(fitAddon)
    term.open(terminalRef.current)
    fitAddon.fit()

    xtermRef.current = term
    fitAddonRef.current = fitAddon

    // Handle terminal input (keyboard) with rate limiting
    term.onData((data) => {
      setKeystrokeCount((prev) => {
        const newCount = prev + data.length

        if (newCount > KEYSTROKE_LIMIT) {
          term.writeln('\r\n\x1b[1;31m⚠️  Keystroke limit reached (1000 characters max)\x1b[0m')
          term.writeln('\x1b[1;33mThis is a demo environment. Terminal input is read-only now.\x1b[0m')
          return prev
        }

        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
          // Send input to backend
          wsRef.current.send(
            JSON.stringify({
              type: 'terminal_input',
              payload: {
                jobId,
                tool,
                data,
              },
            })
          )
        }

        return newCount
      })
    })

    // Handle window resize
    const handleResize = () => {
      fitAddon.fit()
    }
    window.addEventListener('resize', handleResize)

    // Connect to WebSocket (using current window location for dynamic host)
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsHost = import.meta.env.DEV ? 'localhost:3001' : window.location.host
    const ws = new WebSocket(`${wsProtocol}//${wsHost}/ws`)
    wsRef.current = ws

    ws.onopen = () => {
      console.log(`WebSocket connected for ${tool}`)
      // Subscribe to job updates
      ws.send(
        JSON.stringify({
          type: 'subscribe',
          payload: { jobId },
        })
      )
    }

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)

        // Only process messages for this tool
        if (message.type === 'terminal_output') {
          const { payload } = message
          if (payload.tool === tool && payload.jobId === jobId) {
            term.write(payload.data)
          }
        } else if (message.type === 'build_started') {
          const { payload } = message
          if (payload.tool === tool && payload.jobId === jobId) {
            term.writeln(`\x1b[1;32m=== Build started at ${payload.timestamp} ===\x1b[0m`)
            // Start the timer
            setBuildStartTime(Date.now())
            setElapsedTime(0)
            setBuildDuration(null)
          }
        } else if (message.type === 'build_completed') {
          const { payload } = message
          if (payload.tool === tool && payload.jobId === jobId) {
            const success = payload.result.status === 'success'
            const color = success ? '1;32' : '1;31'
            term.writeln(
              `\x1b[${color}m=== Build ${success ? 'succeeded' : 'failed'} in ${
                payload.result.duration
              }ms ===\x1b[0m`
            )
            // Stop the timer
            if (buildStartTime) {
              setBuildDuration(Date.now() - buildStartTime)
            }
            setBuildStartTime(null)
          }
        } else if (message.type === 'build_bell') {
          const { payload } = message
          if (payload.tool === tool && payload.jobId === jobId) {
            setBellCount((prev) => prev + 1)
            if (onBellRung) {
              onBellRung()
            }
            // Stop the timer when bell rings
            if (buildStartTime) {
              const finalDuration = Date.now() - buildStartTime
              setBuildDuration(finalDuration)
              setBuildStartTime(null)
            }
            // Visual notification
            term.writeln('\x1b[1;33m🔔 BELL RUNG! Build milestone reached!\x1b[0m')
          }
        } else if (message.type === 'error') {
          term.writeln(`\x1b[1;31mError: ${message.payload.message}\x1b[0m`)
        }
      } catch (error) {
        console.error('Error processing WebSocket message:', error)
      }
    }

    ws.onerror = (error) => {
      console.error('WebSocket error:', error)
      term.writeln('\x1b[1;31mWebSocket connection error\x1b[0m')
    }

    ws.onclose = () => {
      console.log('WebSocket connection closed')
      term.writeln('\x1b[1;33mConnection closed\x1b[0m')
    }

    // Cleanup
    return () => {
      window.removeEventListener('resize', handleResize)
      ws.close()
      term.dispose()
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current)
      }
    }
  }, [jobId, tool, onBellRung])

  // Timer effect - updates elapsed time every 100ms when build is running
  useEffect(() => {
    if (buildStartTime !== null) {
      // Start interval to update elapsed time
      timerIntervalRef.current = setInterval(() => {
        setElapsedTime(Date.now() - buildStartTime)
      }, 100) // Update every 100ms for smooth display

      return () => {
        if (timerIntervalRef.current) {
          clearInterval(timerIntervalRef.current)
          timerIntervalRef.current = null
        }
      }
    }
  }, [buildStartTime])

  // Format time as MM:SS.d
  const formatTime = (ms: number): string => {
    const totalSeconds = Math.floor(ms / 1000)
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    const deciseconds = Math.floor((ms % 1000) / 100)
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${deciseconds}`
  }

  return (
    <div className="relative h-full w-full">
      <div ref={terminalRef} className="h-full w-full" />

      {/* Timer Display */}
      {(buildStartTime !== null || buildDuration !== null) && (
        <div className="absolute top-2 left-2 bg-indigo-600 text-white px-4 py-2 rounded-lg font-mono text-lg font-bold shadow-lg">
          ⏱️ {formatTime(buildDuration !== null ? buildDuration : elapsedTime)}
          {buildDuration !== null && ' (Final)'}
        </div>
      )}

      {/* Bell Counter */}
      {bellCount > 0 && (
        <div className="absolute top-2 right-2 bg-yellow-500 text-black px-3 py-1 rounded-full font-bold text-sm animate-pulse">
          🔔 x{bellCount}
        </div>
      )}

      {/* Keystroke Counter */}
      {keystrokeCount > 0 && (
        <div className="absolute bottom-2 right-2 bg-slate-700 text-gray-300 px-2 py-1 rounded text-xs">
          {keystrokeCount}/{KEYSTROKE_LIMIT} keys
        </div>
      )}
    </div>
  )
}
