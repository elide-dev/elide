#!/bin/bash
# Monitor test progress by watching log file growth
# If stuck for 10 seconds, kill and report where it got stuck

LOG_FILE="test-output.log"
STALL_THRESHOLD=10  # seconds without log growth = stuck

echo "🔍 Starting monitored test run..."
echo "📝 Logging to: $LOG_FILE"
echo ""

# Start the test in background, logging to file
node test-claude-auto-approve.js > "$LOG_FILE" 2>&1 &
TEST_PID=$!

echo "🚀 Test started (PID: $TEST_PID)"
echo "👀 Monitoring log file for progress..."
echo ""

last_size=0
stall_count=0

while kill -0 $TEST_PID 2>/dev/null; do
  current_size=$(wc -c < "$LOG_FILE" 2>/dev/null || echo 0)

  if [ "$current_size" -gt "$last_size" ]; then
    # Log is growing - reset stall counter
    stall_count=0
    echo "📈 Log growing: $current_size bytes (+$((current_size - last_size)))"
    last_size=$current_size
  else
    # Log hasn't grown
    stall_count=$((stall_count + 1))
    echo "⏳ Stalled for ${stall_count}s (threshold: ${STALL_THRESHOLD}s)"

    if [ $stall_count -ge $STALL_THRESHOLD ]; then
      echo ""
      echo "❌ STUCK! Log hasn't grown for ${STALL_THRESHOLD} seconds"
      echo ""

      # Kill the test
      kill $TEST_PID 2>/dev/null
      sleep 2
      kill -9 $TEST_PID 2>/dev/null

      # Show last 50 lines of log
      echo "📋 Last 50 lines of output:"
      echo "════════════════════════════════════════"
      tail -50 "$LOG_FILE" | sed 's/\x1b\[[0-9;]*m//g'  # Strip ANSI colors
      echo "════════════════════════════════════════"
      echo ""

      # Try to identify the stuck point
      echo "🔍 Analyzing stuck point..."
      if tail -100 "$LOG_FILE" | grep -q "API Error: 500\|Internal server error\|api_error"; then
        echo "   ⚠️  API Error detected - minder should auto-retry"
        echo "   Check if retry worked or if error persists"
      elif tail -100 "$LOG_FILE" | grep -q "Do you want to proceed?"; then
        echo "   Appears stuck on permission prompt"
      elif tail -100 "$LOG_FILE" | grep -q "Waiting…"; then
        echo "   Appears stuck waiting for command"
      elif tail -100 "$LOG_FILE" | grep -q "command not found"; then
        echo "   Command not found - likely missing tool"
      else
        echo "   Unknown stuck state - check log manually"
      fi

      exit 1
    fi
  fi

  sleep 1
done

# Check if test completed successfully
if grep -q "🔔" "$LOG_FILE"; then
  echo ""
  echo "🎉 SUCCESS! Bell detected - test completed!"
  echo ""
  grep "🔔" "$LOG_FILE"
  exit 0
else
  echo ""
  echo "⚠️  Test exited but no bell found"
  exit 1
fi
