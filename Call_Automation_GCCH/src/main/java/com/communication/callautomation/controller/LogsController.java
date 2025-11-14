package com.communication.callautomation.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller
@Tag(name = "01. Live Console Logs", description = "Real-time console logs viewer")
public class LogsController {

    @GetMapping(value = "/api/logs", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Live Console Logs", description = "View real-time console logs")
    @ResponseBody
    public String getLiveLogsPage() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Live Console Logs</title>
                    <style>
                        body {
                            font-family: 'Courier New', monospace;
                            background-color: #1e1e1e;
                            color: #d4d4d4;
                            margin: 0;
                            padding: 20px;
                        }
                        .header {
                            background-color: #2d2d30;
                            padding: 15px;
                            border-radius: 5px;
                            margin-bottom: 20px;
                            border-left: 4px solid #007acc;
                        }
                        .logs-container {
                            background-color: #252526;
                            border: 1px solid #3e3e42;
                            border-radius: 5px;
                            height: 600px;
                            overflow-y: auto;
                            padding: 15px;
                            font-size: 14px;
                            line-height: 1.4;
                        }
                        .log-entry {
                            margin-bottom: 5px;
                            white-space: pre-wrap;
                            word-break: break-all;
                        }
                        .log-info { color: #4fc1ff; }
                        .log-warn { color: #ffcc02; }
                        .log-error { color: #f48771; }
                        .log-debug { color: #b5cea8; }
                        .connection-status {
                            padding: 10px;
                            margin-bottom: 15px;
                            border-radius: 5px;
                            text-align: center;
                            font-weight: bold;
                        }
                        .connected {
                            background-color: #1a5a1a;
                            color: #4caf50;
                            border: 1px solid #4caf50;
                        }
                        .disconnected {
                            background-color: #5a1a1a;
                            color: #f44336;
                            border: 1px solid #f44336;
                        }
                        .controls {
                            margin-bottom: 15px;
                        }
                        .btn {
                            background-color: #007acc;
                            color: white;
                            border: none;
                            padding: 8px 16px;
                            border-radius: 3px;
                            cursor: pointer;
                            margin-right: 10px;
                            font-size: 14px;
                        }
                        .btn:hover {
                            background-color: #005a9e;
                        }
                        .btn:disabled {
                            background-color: #666;
                            cursor: not-allowed;
                        }
                        .auto-scroll {
                            margin-left: 10px;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>🔴 Live Console Logs</h1>
                        <p>Real-time streaming of application logs via WebSocket</p>
                    </div>
                    
                    <div id="connectionStatus" class="connection-status disconnected">
                        Connecting to WebSocket...
                    </div>
                    
                    <div class="controls">
                        <button id="clearBtn" class="btn" onclick="clearLogs()">Clear Logs</button>
                        <button id="reconnectBtn" class="btn" onclick="reconnect()">Reconnect</button>
                        <button id="pauseBtn" class="btn" onclick="togglePause()">Pause</button>
                        <label class="auto-scroll">
                            <input type="checkbox" id="autoScroll" checked> Auto-scroll
                        </label>
                    </div>
                    
                    <div id="logsContainer" class="logs-container">
                        <div class="log-entry log-info">Initializing WebSocket connection...</div>
                    </div>

                    <script>
                        let socket = null;
                        let isPaused = false;
                        let logQueue = [];
                        const maxLogEntries = 1000;
                        
                        let reconnectAttempts = 0;
                        let maxReconnectAttempts = 10;
                        let reconnectInterval = 3000; // Start with 3 seconds
                        let heartbeatInterval = null;
                        
                        function connectWebSocket() {
                            // Try SockJS-enabled endpoint first for better compatibility
                            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                            const host = window.location.host;
                            const wsUrl = `${protocol}//${host}/ws/logs`;
                            
                            addLogEntry(`Attempting WebSocket connection to: ${wsUrl} (attempt ${reconnectAttempts + 1})`, 'info');
                            
                            try {
                                socket = new WebSocket(wsUrl);
                                
                                // Set a connection timeout
                                const connectionTimeout = setTimeout(() => {
                                    if (socket && socket.readyState === WebSocket.CONNECTING) {
                                        socket.close();
                                        addLogEntry('WebSocket connection timeout - trying direct endpoint', 'warn');
                                        connectDirectWebSocket();
                                    }
                                }, 8000); // 8 second timeout
                                
                                socket.onopen = function(event) {
                                    clearTimeout(connectionTimeout);
                                    reconnectAttempts = 0; // Reset on successful connection
                                    reconnectInterval = 3000; // Reset interval
                                    updateConnectionStatus(true);
                                    addLogEntry('✅ WebSocket connected successfully via SockJS', 'info');
                                    
                                    // Start heartbeat
                                    startHeartbeat();
                                };
                                
                                socket.onmessage = function(event) {
                                    const message = event.data;
                                    if (message === 'PONG') {
                                        addLogEntry('Heartbeat: PONG received', 'debug');
                                        return;
                                    }
                                    
                                    if (!isPaused) {
                                        processLogMessage(message);
                                    } else {
                                        logQueue.push(message);
                                    }
                                };
                                
                                socket.onclose = function(event) {
                                    clearTimeout(connectionTimeout);
                                    stopHeartbeat();
                                    updateConnectionStatus(false);
                                    
                                    let closeReason = getCloseReason(event.code);
                                    addLogEntry(`WebSocket connection closed. Code: ${event.code} (${closeReason})`, 'warn');
                                    
                                    // Handle different close codes
                                    if (event.code === 1006) {
                                        addLogEntry('Error 1006: Abnormal closure detected - likely network/proxy issue', 'error');
                                    }
                                    
                                    // Retry connection with exponential backoff
                                    if (event.code !== 1000 && reconnectAttempts < maxReconnectAttempts) {
                                        reconnectAttempts++;
                                        const delay = Math.min(reconnectInterval * Math.pow(1.5, reconnectAttempts - 1), 30000);
                                        
                                        setTimeout(() => {
                                            addLogEntry(`Attempting to reconnect in ${delay/1000}s...`, 'info');
                                            connectWebSocket();
                                        }, delay);
                                    } else if (reconnectAttempts >= maxReconnectAttempts) {
                                        addLogEntry('Max reconnection attempts reached. Switching to Server-Sent Events...', 'warn');
                                        connectWithFallback();
                                    }
                                };
                                
                                socket.onerror = function(error) {
                                    clearTimeout(connectionTimeout);
                                    updateConnectionStatus(false);
                                    addLogEntry('WebSocket error occurred - attempting direct connection', 'error');
                                    console.error('WebSocket error:', error);
                                    
                                    // Try direct WebSocket endpoint
                                    setTimeout(() => connectDirectWebSocket(), 1000);
                                };
                                
                            } catch (error) {
                                addLogEntry('Failed to create WebSocket connection: ' + error.message, 'error');
                                setTimeout(() => connectDirectWebSocket(), 1000);
                            }
                        }
                        
                        function connectDirectWebSocket() {
                            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                            const host = window.location.host;
                            const wsUrl = `${protocol}//${host}/websocket/logs`;
                            
                            addLogEntry(`Trying direct WebSocket connection to: ${wsUrl}`, 'info');
                            
                            try {
                                socket = new WebSocket(wsUrl);
                                
                                socket.onopen = function(event) {
                                    reconnectAttempts = 0;
                                    updateConnectionStatus(true);
                                    addLogEntry('✅ Direct WebSocket connected successfully', 'info');
                                    startHeartbeat();
                                };
                                
                                socket.onmessage = function(event) {
                                    const message = event.data;
                                    if (message === 'PONG') {
                                        return; // Ignore pong messages
                                    }
                                    
                                    if (!isPaused) {
                                        processLogMessage(message);
                                    } else {
                                        logQueue.push(message);
                                    }
                                };
                                
                                socket.onclose = function(event) {
                                    stopHeartbeat();
                                    updateConnectionStatus(false);
                                    addLogEntry(`Direct WebSocket closed. Code: ${event.code}`, 'warn');
                                    
                                    if (event.code !== 1000) {
                                        addLogEntry('Direct WebSocket failed. Switching to Server-Sent Events...', 'warn');
                                        connectWithFallback();
                                    }
                                };
                                
                                socket.onerror = function(error) {
                                    updateConnectionStatus(false);
                                    addLogEntry('Direct WebSocket error - switching to fallback', 'error');
                                    connectWithFallback();
                                };
                                
                            } catch (error) {
                                addLogEntry('Failed to create direct WebSocket: ' + error.message, 'error');
                                connectWithFallback();
                            }
                        }
                        
                        function startHeartbeat() {
                            if (heartbeatInterval) {
                                clearInterval(heartbeatInterval);
                            }
                            
                            heartbeatInterval = setInterval(() => {
                                if (socket && socket.readyState === WebSocket.OPEN) {
                                    socket.send('PING');
                                    addLogEntry('Heartbeat: PING sent', 'debug');
                                }
                            }, 30000); // Send ping every 30 seconds
                        }
                        
                        function stopHeartbeat() {
                            if (heartbeatInterval) {
                                clearInterval(heartbeatInterval);
                                heartbeatInterval = null;
                            }
                        }
                        
                        function getCloseReason(code) {
                            const reasons = {
                                1000: 'Normal Closure',
                                1001: 'Going Away',
                                1002: 'Protocol Error',
                                1003: 'Unsupported Data',
                                1005: 'No Status Received',
                                1006: 'Abnormal Closure',
                                1007: 'Invalid frame payload data',
                                1008: 'Policy Violation',
                                1009: 'Message too big',
                                1010: 'Mandatory extension',
                                1011: 'Internal Server Error',
                                1015: 'TLS handshake'
                            };
                            return reasons[code] || 'Unknown';
                        }
                        
                        // Fallback connection method for environments with WebSocket issues
                        function connectWithFallback() {
                            addLogEntry('Attempting fallback connection method...', 'info');
                            // For now, just show a message. In production, you might use Server-Sent Events
                            addLogEntry('WebSocket unavailable. Please check Azure VM configuration.', 'error');
                            addLogEntry('Ensure port 8080 is open and WebSocket proxy is configured.', 'warn');
                        }
                        
                        function updateConnectionStatus(connected) {
                            const statusElement = document.getElementById('connectionStatus');
                            if (connected) {
                                statusElement.className = 'connection-status connected';
                                statusElement.textContent = '🟢 Connected to WebSocket';
                            } else {
                                statusElement.className = 'connection-status disconnected';
                                statusElement.textContent = '🔴 Disconnected from WebSocket';
                            }
                        }
                        
                        function processLogMessage(message) {
                            const logLevel = getLogLevel(message);
                            addLogEntry(message, logLevel);
                        }
                        
                        function getLogLevel(message) {
                            if (message.includes('[ERROR]')) return 'error';
                            if (message.includes('[WARN]')) return 'warn';
                            if (message.includes('[DEBUG]')) return 'debug';
                            return 'info';
                        }
                        
                        function addLogEntry(message, level = 'info') {
                            const container = document.getElementById('logsContainer');
                            const logEntry = document.createElement('div');
                            logEntry.className = `log-entry log-${level}`;
                            logEntry.textContent = message;
                            
                            container.appendChild(logEntry);
                            
                            const entries = container.getElementsByClassName('log-entry');
                            if (entries.length > maxLogEntries) {
                                container.removeChild(entries[0]);
                            }
                            
                            if (document.getElementById('autoScroll').checked) {
                                container.scrollTop = container.scrollHeight;
                            }
                        }
                        
                        function clearLogs() {
                            document.getElementById('logsContainer').innerHTML = '';
                            logQueue = [];
                        }
                        
                        function reconnect() {
                            if (socket) {
                                socket.close();
                            }
                            connectWebSocket();
                        }
                        
                        function togglePause() {
                            isPaused = !isPaused;
                            const btn = document.getElementById('pauseBtn');
                            btn.textContent = isPaused ? 'Resume' : 'Pause';
                            
                            if (!isPaused && logQueue.length > 0) {
                                logQueue.forEach(message => processLogMessage(message));
                                logQueue = [];
                            }
                        }
                        
                        window.onload = function() {
                            connectWebSocket();
                        };
                        
                        document.addEventListener('visibilitychange', function() {
                            if (document.visibilityState === 'visible' && (!socket || socket.readyState === WebSocket.CLOSED)) {
                                connectWebSocket();
                            }
                        });
                    </script>
                </body>
                </html>
                """;
    }
}