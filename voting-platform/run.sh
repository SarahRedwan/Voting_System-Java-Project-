#!/bin/bash
# SecureVote 2026 Startup Script for Linux/macOS

echo ""
echo "================================"
echo "SecureVote 2026 Platform"
echo "================================"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed or not in PATH"
    echo "Please install Maven from: https://maven.apache.org/download.cgi"
    exit 1
fi

# Build project
echo "[1/3] Building project..."
mvn clean install -q
if [ $? -ne 0 ]; then
    echo "ERROR: Build failed"
    exit 1
fi
echo "Build successful!"

echo ""
echo "[2/3] Starting VotingSocketServer..."
echo "Press Ctrl+C to stop server"
echo ""
java -cp target/classes org.example.client.core.VotingSocketServer &
SERVER_PID=$!

echo ""
echo "[3/3] Waiting for server to start (3 seconds)..."
sleep 3

echo ""
echo "Starting JavaFX Application..."
echo ""
mvn javafx:run

# Kill server on exit
kill $SERVER_PID 2>/dev/null
