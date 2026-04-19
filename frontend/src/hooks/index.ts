/**
 * WebSocket hooks for real-time communication
 *
 * This module exports React hooks for WebSocket communication:
 * - useWebSocket: Generic WebSocket hook with auto-reconnect and heartbeat
 * - useWebSocketQA: Specialized hook for QA streaming with session management
 *
 * @example
 * ```tsx
 * import { useWebSocket, useWebSocketQA } from '@/hooks'
 *
 * // Generic WebSocket
 * const ws = useWebSocket({
 *   url: 'ws://localhost:8080/api/ws',
 *   onMessage: (data) => console.log(data),
 * })
 *
 * // QA Streaming WebSocket
 * const qa = useWebSocketQA({
 *   onMessage: (chunk) => console.log(chunk),
 *   onComplete: (answer) => console.log('Done:', answer),
 *   onError: (error) => console.error(error),
 * })
 * ```
 */

export { useWebSocket } from './useWebSocket'
export type {
  MessageHandler,
  UseWebSocketOptions,
  UseWebSocketReturn,
} from './useWebSocket'

export { useWebSocketQA } from './useWebSocketQA'
export type {
  QAWsMessage,
  UseWebSocketQAOptions,
  UseWebSocketQAReturn,
} from './useWebSocketQA'
