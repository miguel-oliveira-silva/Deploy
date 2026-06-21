#!/bin/bash

# Test script for retry_command function
# This script tests the retry logic implementation

source ./bootstrap.sh.tpl

echo "=== Test 1: Command that succeeds immediately ==="
retry_command 3 1 "TEST" echo "Success on first try"
echo "Exit code: $?"
echo ""

echo "=== Test 2: Command that fails (simulating with false) ==="
retry_command 3 1 "TEST" false
echo "Exit code: $?"
echo ""

echo "=== Test 3: Command with multiple arguments ==="
retry_command 2 1 "TEST" echo "Multiple" "arguments" "test"
echo "Exit code: $?"
echo ""

echo "=== Displaying generated log ==="
cat "$LOG_FILE"
