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
                        
                        function connectWebSocket() {
                            // Enhanced WebSocket connection with fallback support
                            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                            const host = window.location.host;
                            
                            // Try direct WebSocket connection first
                            const wsUrl = `${protocol}//${host}/websocket/logs`;
                            
                            addLogEntry(`Attempting WebSocket connection to: ${wsUrl}`, 'info');
                            
                            try {
                                socket = new WebSocket(wsUrl);
                                
                                // Set a connection timeout
                                const connectionTimeout = setTimeout(() => {
                                    if (socket.readyState === WebSocket.CONNECTING) {
                                        socket.close();
                                        addLogEntry('WebSocket connection timeout - trying fallback', 'warn');
                                        connectWithFallback();
                                    }
                                }, 10000); // 10 second timeout
                                
                                socket.onopen = function(event) {
                                    clearTimeout(connectionTimeout);
                                    updateConnectionStatus(true);
                                    addLogEntry('WebSocket connected successfully', 'info');
                                };
                                
                                socket.onmessage = function(event) {
                                    if (!isPaused) {
                                        processLogMessage(event.data);
                                    } else {
                                        logQueue.push(event.data);
                                    }
                                };
                                
                                socket.onclose = function(event) {
                                    clearTimeout(connectionTimeout);
                                    updateConnectionStatus(false);
                                    addLogEntry(`WebSocket connection closed. Code: ${event.code}, Reason: ${event.reason}`, 'warn');
                                    
                                    // Retry connection after delay
                                    if (event.code !== 1000) { // Not a normal closure
                                        setTimeout(() => {
                                            addLogEntry('Attempting to reconnect...', 'info');
                                            connectWebSocket();
                                        }, 5000);
                                    }
                                };
                                
                                socket.onerror = function(error) {
                                    clearTimeout(connectionTimeout);
                                    updateConnectionStatus(false);
                                    addLogEntry('WebSocket error occurred - check browser console for details', 'error');
                                    console.error('WebSocket error:', error);
                                };
                                
                            } catch (error) {
                                addLogEntry('Failed to create WebSocket connection: ' + error.message, 'error');
                                connectWithFallback();
                            }
                        }
                        
                        // Fallback connection method for environments with WebSocket issues
                        function connectWithFallback() {
                            addLogEntry('Attempting fallback connection method...', 'info');
                            
                            // Try SockJS endpoint as fallback
                            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                            const host = window.location.host;
                            const fallbackUrl = `${protocol}//${host}/ws/logs`;
                            
                            addLogEntry(`Trying SockJS fallback: ${fallbackUrl}`, 'info');
                            
                            try {
                                socket = new WebSocket(fallbackUrl);
                                
                                socket.onopen = function(event) {
                                    updateConnectionStatus(true);
                                    addLogEntry('WebSocket connected via SockJS fallback', 'info');
                                };
                                
                                socket.onmessage = function(event) {
                                    if (!isPaused) {
                                        processLogMessage(event.data);
                                    } else {
                                        logQueue.push(event.data);
                                    }
                                };
                                
                                socket.onclose = function(event) {
                                    updateConnectionStatus(false);
                                    addLogEntry(`Fallback WebSocket connection closed. Code: ${event.code}, Reason: ${event.reason}`, 'warn');
                                };
                                
                                socket.onerror = function(error) {
                                    updateConnectionStatus(false);
                                    addLogEntry('Fallback WebSocket error occurred', 'error');
                                    addLogEntry('WebSocket unavailable. Please check dev tunnel configuration.', 'error');
                                    addLogEntry('For dev tunnels, WebSocket connections may require additional configuration.', 'warn');
                                };
                                
                            } catch (error) {
                                addLogEntry('Failed to create fallback WebSocket connection: ' + error.message, 'error');
                                addLogEntry('WebSocket unavailable. Please check Azure VM configuration.', 'error');
                                addLogEntry('Ensure port 8080 is open and WebSocket proxy is configured.', 'warn');
                            }
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