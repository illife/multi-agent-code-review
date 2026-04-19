import { useCallback, useEffect, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import type { RootState } from '../store';
import {
  addMessage,
  setCurrentAnswer,
  appendToAnswer,
  setStreaming,
  setProcessing,
  setError,
  clearCurrentAnswer,
  createSession,
  type ChatMessage,
} from '../store/slices/qaSlice';
import { useWebSocket, type UseWebSocketOptions } from './useWebSocket';

/**
 * QA WebSocket message types
 */
export interface QAWsMessage {
  type: 'chunk' | 'complete' | 'error' | 'ping' | 'pong';
  sessionId?: string;
  content?: string;
  answer?: string;
  sources?: any[];
  error?: string;
}

/**
 * QA WebSocket hook options
 */
export interface UseWebSocketQAOptions {
  /**
   * Base API URL (defaults to VITE_API_BASE_URL)
   */
  apiBaseUrl?: string;

  /**
   * WebSocket path
   * @default '/api/ws/qa'
   */
  wsPath?: string;

  /**
   * Session ID for the conversation
   * If not provided, a new session will be created
   */
  sessionId?: string;

  /**
   * Called when a new chunk is received
   */
  onChunk?: (chunk: string) => void;

  /**
   * Called when the complete answer is received
   */
  onComplete?: (answer: string, sources?: any[]) => void;

  /**
   * Called when an error occurs
   */
  onError?: (error: string) => void;

  /**
   * Enable auto-reconnect
   * @default true
   */
  autoReconnect?: boolean;

  /**
   * Additional WebSocket options
   */
  wsOptions?: Partial<Omit<UseWebSocketOptions, 'url' | 'onMessage'>>;
}

/**
 * QA WebSocket hook return value
 */
export interface UseWebSocketQAReturn {
  /**
   * WebSocket connection state
   */
  isConnected: boolean;

  /**
   * Whether currently streaming an answer
   */
  isStreaming: boolean;

  /**
   * Whether currently processing a question
   */
  isProcessing: boolean;

  /**
   * Current session ID
   */
  sessionId: string | null;

  /**
   * Current answer being streamed
   */
  currentAnswer: string;

  /**
   * Last error message
   */
  error: string | null;

  /**
   * Ask a question (streams the answer)
   */
  askQuestion: (question: string, sessionId?: string) => void;

  /**
   * Send a raw message through WebSocket
   */
  sendRaw: (message: any) => void;

  /**
   * Manually connect
   */
  connect: () => void;

  /**
   * Manually disconnect
   */
  disconnect: () => void;

  /**
   * Clear the current answer
   */
  clearAnswer: () => void;
}

/**
 * QA WebSocket Hook
 *
 * Specialized WebSocket hook for Q&A streaming functionality.
 * Handles question submission, answer streaming, and session management.
 *
 * @example
 * ```tsx
 * const { isConnected, askQuestion, isStreaming } = useWebSocketQA({
 *   onChunk: (chunk) => console.log('Received:', chunk),
 *   onComplete: (answer) => console.log('Complete:', answer),
 * });
 *
 * askQuestion('What is the capital of France?');
 * ```
 */
export function useWebSocketQA(options: UseWebSocketQAOptions = {}): UseWebSocketQAReturn {
  const {
    apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    wsPath = '/api/ws/qa',
    sessionId: propSessionId,
    onChunk,
    onComplete,
    onError,
    autoReconnect = true,
    wsOptions = {},
  } = options;

  const dispatch = useDispatch();
  const currentSessionId = useSelector((state: RootState) => state.qa.currentSessionId);
  const isStreaming = useSelector((state: RootState) => state.qa.isStreaming);
  const isProcessing = useSelector((state: RootState) => state.qa.isProcessing);
  const currentAnswer = useSelector((state: RootState) => state.qa.currentAnswer);
  const qaError = useSelector((state: RootState) => state.qa.error);

  const sessionIdRef = useRef<string | null>(propSessionId || currentSessionId);
  const questionBufferRef = useRef<string[]>([]);

  // Update session ref when prop changes
  useEffect(() => {
    if (propSessionId) {
      sessionIdRef.current = propSessionId;
    }
  }, [propSessionId]);

  /**
   * Handle incoming WebSocket messages
   */
  const handleMessage = useCallback((data: any) => {
    const message: QAWsMessage = typeof data === 'string' ? JSON.parse(data) : data;

    switch (message.type) {
      case 'chunk':
        // Streaming chunk received
        dispatch(setStreaming(true));
        dispatch(setProcessing(false));
        dispatch(appendToAnswer(message.content || ''));
        onChunk?.(message.content || '');
        break;

      case 'complete':
        // Answer complete
        const answer = message.answer || currentAnswer;
        const sources = message.sources;

        // Add assistant message to session
        if (sessionIdRef.current && answer) {
          const assistantMessage: ChatMessage = {
            id: `msg-${Date.now()}-assistant`,
            role: 'assistant',
            content: answer,
            timestamp: new Date().toISOString(),
            sources,
          };
          dispatch(addMessage({ sessionId: sessionIdRef.current, message: assistantMessage }));
        }

        dispatch(setStreaming(false));
        dispatch(setProcessing(false));
        dispatch(setCurrentAnswer(''));
        onComplete?.(answer, sources);
        break;

      case 'error':
        // Error occurred
        const errorMsg = message.error || 'Unknown error';
        dispatch(setError(errorMsg));
        dispatch(setStreaming(false));
        dispatch(setProcessing(false));
        onError?.(errorMsg);
        break;

      case 'pong':
        // Heartbeat response - no action needed
        break;

      case 'ping':
        // Respond to server ping with pong
        wsRef.current?.send?.({ type: 'pong' });
        break;

      default:
        console.warn('Unknown message type:', message.type);
    }
  }, [dispatch, currentAnswer, onChunk, onComplete, onError]);

  // Build WebSocket URL
  const wsUrl = apiBaseUrl.replace('http://', 'ws://').replace('https://', 'wss://') + wsPath;

  // Use generic WebSocket hook
  const ws = useWebSocket({
    url: wsUrl,
    reconnect: autoReconnect,
    onMessage: handleMessage,
    ...wsOptions,
  });

  // Keep reference to ws methods
  const wsRef = useRef(ws);
  useEffect(() => {
    wsRef.current = ws;
  }, [ws]);

  /**
   * Ask a question
   */
  const askQuestion = useCallback((question: string, sessionId?: string) => {
    const effectiveSessionId = sessionId || sessionIdRef.current;

    if (!effectiveSessionId) {
      // Create new session
      const newSessionId = `session-${Date.now()}`;
      dispatch(createSession(newSessionId));
      sessionIdRef.current = newSessionId;
    }

    // Add user message to session
    const userMessage: ChatMessage = {
      id: `msg-${Date.now()}-user`,
      role: 'user',
      content: question,
      timestamp: new Date().toISOString(),
    };
    dispatch(addMessage({ sessionId: effectiveSessionId || sessionIdRef.current!, message: userMessage }));

    // Clear previous answer
    dispatch(clearCurrentAnswer());
    dispatch(setProcessing(true));

    // Send question through WebSocket
    if (ws.isConnected) {
      ws.send({
        type: 'ask',
        action: 'ask',
        question,
        sessionId: effectiveSessionId || sessionIdRef.current,
      });
    } else {
      // Queue question for when connection is established
      questionBufferRef.current.push(question);
      dispatch(setError('WebSocket not connected. Question queued.'));
    }
  }, [dispatch, ws]);

  /**
   * Send raw message through WebSocket
   */
  const sendRaw = useCallback((message: any) => {
    if (ws.isConnected) {
      ws.send(message);
    }
  }, [ws.isConnected, ws.send]);

  /**
   * Clear current answer
   */
  const clearAnswer = useCallback(() => {
    dispatch(clearCurrentAnswer());
  }, [dispatch]);

  /**
   * Send queued questions when connection is established
   */
  useEffect(() => {
    if (ws.isConnected && questionBufferRef.current.length > 0) {
      const question = questionBufferRef.current.shift();
      if (question) {
        askQuestion(question);
      }
    }
  }, [ws.isConnected, askQuestion]);

  return {
    isConnected: ws.isConnected,
    isStreaming,
    isProcessing,
    sessionId: sessionIdRef.current,
    currentAnswer,
    error: qaError,
    askQuestion,
    sendRaw,
    connect: ws.connect,
    disconnect: ws.disconnect,
    clearAnswer,
  };
}

export default useWebSocketQA;
