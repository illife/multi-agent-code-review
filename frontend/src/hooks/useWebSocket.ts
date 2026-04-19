import { useEffect, useRef, useCallback, useState } from 'react';

/**
 * WebSocket message handler type
 */
export type MessageHandler = (data: any) => void;

/**
 * WebSocket hook options
 */
export interface UseWebSocketOptions {
  /**
   * WebSocket URL
   */
  url: string;

  /**
   * Enable auto-reconnect
   * @default true
   */
  reconnect?: boolean;

  /**
   * Maximum retry attempts
   * @default 5
   */
  maxRetries?: number;

  /**
   * Retry interval in milliseconds
   * @default 3000
   */
  retryInterval?: number;

  /**
   * Enable exponential backoff for retries
   * @default true
   */
  exponentialBackoff?: boolean;

  /**
   * Heartbeat interval in milliseconds (0 = disabled)
   * @default 30000
   */
  heartbeatInterval?: number;

  /**
   * Connection timeout in milliseconds
   * @default 10000
   */
  connectionTimeout?: number;

  /**
   * Message handler for received messages
   */
  onMessage?: MessageHandler;

  /**
   * Called when connection is established
   */
  onOpen?: (event: WebSocketEventMap['open']) => void;

  /**
   * Called when connection is closed
   */
  onClose?: (event: WebSocketEventMap['close']) => void;

  /**
   * Called when an error occurs
   */
  onError?: (event: WebSocketEventMap['error']) => void;

  /**
   * Called when reconnecting
   */
  onReconnecting?: (attempt: number) => void;
}

/**
 * WebSocket hook return value
 */
export interface UseWebSocketReturn {
  /**
   * WebSocket connection state
   */
  isConnected: boolean;

  /**
   * Whether currently reconnecting
   */
  isReconnecting: boolean;

  /**
   * Current retry attempt count
   */
  retryCount: number;

  /**
   * Last error message
   */
  error: string | null;

  /**
   * Send message through WebSocket
   */
  send: (data: string | object) => void;

  /**
   * Manually connect
   */
  connect: () => void;

  /**
   * Manually disconnect
   */
  disconnect: () => void;

  /**
   * Get the underlying WebSocket instance
   */
  getWebSocket: () => WebSocket | null;
}

/**
 * Generic WebSocket hook
 *
 * Provides a robust WebSocket connection with auto-reconnect,
 * heartbeat/ping-pong, and message handling.
 *
 * @example
 * ```tsx
 * const { isConnected, send } = useWebSocket({
 *   url: 'ws://localhost:8080/ws',
 *   onMessage: (data) => console.log('Received:', data),
 *   onOpen: () => console.log('Connected'),
 * });
 * ```
 */
export function useWebSocket(options: UseWebSocketOptions): UseWebSocketReturn {
  const {
    url,
    reconnect = true,
    maxRetries = 5,
    retryInterval = 3000,
    exponentialBackoff = true,
    heartbeatInterval = 30000,
    connectionTimeout = 10000,
    onMessage,
    onOpen,
    onClose,
    onError,
    onReconnecting,
  } = options;

  const wsRef = useRef<WebSocket | null>(null);
  const retryTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const heartbeatTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const connectionTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const [isConnected, setIsConnected] = useState(false);
  const [isReconnecting, setIsReconnecting] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const [error, setError] = useState<string | null>(null);

  /**
   * Calculate retry delay with exponential backoff
   */
  const getRetryDelay = useCallback((attempt: number): number => {
    if (exponentialBackoff) {
      return Math.min(retryInterval * Math.pow(2, attempt), 30000); // Cap at 30s
    }
    return retryInterval;
  }, [retryInterval, exponentialBackoff]);

  /**
   * Clear all timeouts
   */
  const clearTimeouts = useCallback(() => {
    if (retryTimeoutRef.current) {
      clearTimeout(retryTimeoutRef.current);
      retryTimeoutRef.current = null;
    }
    if (heartbeatTimeoutRef.current) {
      clearInterval(heartbeatTimeoutRef.current);
      heartbeatTimeoutRef.current = null;
    }
    if (connectionTimeoutRef.current) {
      clearTimeout(connectionTimeoutRef.current);
      connectionTimeoutRef.current = null;
    }
  }, []);

  /**
   * Start heartbeat interval
   */
  const startHeartbeat = useCallback(() => {
    if (!heartbeatInterval || heartbeatInterval <= 0) return;

    heartbeatTimeoutRef.current = setInterval(() => {
      const ws = wsRef.current;
      if (ws && ws.readyState === WebSocket.OPEN) {
        try {
          ws.send(JSON.stringify({ type: 'ping' }));
        } catch (err) {
          console.error('Heartbeat failed:', err);
        }
      }
    }, heartbeatInterval);
  }, [heartbeatInterval]);

  /**
   * Connect to WebSocket
   */
  const connect = useCallback(() => {
    // Clear existing connection
    if (wsRef.current) {
      wsRef.current.close();
    }
    clearTimeouts();

    // Create new WebSocket
    const ws = new WebSocket(url);
    wsRef.current = ws;

    // Connection timeout
    connectionTimeoutRef.current = setTimeout(() => {
      if (ws.readyState !== WebSocket.OPEN) {
        ws.close();
        setError('Connection timeout');
      }
    }, connectionTimeout);

    ws.onopen = (event) => {
      clearTimeouts();
      setIsConnected(true);
      setIsReconnecting(false);
      setRetryCount(0);
      setError(null);
      startHeartbeat();
      onOpen?.(event);
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        onMessage?.(data);
      } catch {
        // Not JSON, pass raw data
        onMessage?.(event.data);
      }
    };

    ws.onclose = (event) => {
      clearTimeouts();
      setIsConnected(false);

      if (!event.wasClean && reconnect && retryCount < maxRetries) {
        // Attempt to reconnect
        const nextRetry = retryCount + 1;
        setRetryCount(nextRetry);
        setIsReconnecting(true);
        onReconnecting?.(nextRetry);

        const delay = getRetryDelay(nextRetry);
        retryTimeoutRef.current = setTimeout(() => {
          connect();
        }, delay);
      } else if (retryCount >= maxRetries) {
        setIsReconnecting(false);
        setError('Max reconnection attempts reached');
      }

      onClose?.(event);
    };

    ws.onerror = (event) => {
      setError('WebSocket error occurred');
      onError?.(event);
    };
  }, [
    url,
    reconnect,
    maxRetries,
    retryCount,
    connectionTimeout,
    heartbeatInterval,
    getRetryDelay,
    clearTimeouts,
    startHeartbeat,
    onOpen,
    onMessage,
    onClose,
    onError,
    onReconnecting,
  ]);

  /**
   * Disconnect WebSocket
   */
  const disconnect = useCallback(() => {
    clearTimeouts();
    if (wsRef.current) {
      wsRef.current.close(1000, 'User disconnected');
      wsRef.current = null;
    }
    setIsConnected(false);
    setIsReconnecting(false);
  }, [clearTimeouts]);

  /**
   * Send message through WebSocket
   */
  const send = useCallback((data: string | object) => {
    const ws = wsRef.current;
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      console.warn('Cannot send message: WebSocket is not connected');
      return;
    }

    try {
      const message = typeof data === 'string' ? data : JSON.stringify(data);
      ws.send(message);
    } catch (err) {
      console.error('Failed to send message:', err);
      setError('Failed to send message');
    }
  }, []);

  /**
   * Get WebSocket instance
   */
  const getWebSocket = useCallback(() => wsRef.current, []);

  /**
   * Auto-connect on mount
   */
  useEffect(() => {
    connect();
    return () => {
      disconnect();
    };
  }, [url]); // Only reconnect when URL changes

  return {
    isConnected,
    isReconnecting,
    retryCount,
    error,
    send,
    connect,
    disconnect,
    getWebSocket,
  };
}

export default useWebSocket;
