import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '../store';
import { api } from '../../services/api';

/**
 * Chat message interface
 */
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  sources?: SourceReference[];
}

/**
 * Source reference interface
 */
export interface SourceReference {
  documentId: string;
  documentName: string;
  chunkId: string;
  content: string;
  score: number;
}

/**
 * QA session interface
 */
export interface QASession {
  sessionId: string;
  title: string;
  messages: ChatMessage[];
  createdAt: string;
  updatedAt: string;
}

/**
 * QA state interface
 */
interface QAState {
  sessions: Record<string, QASession>;
  currentSessionId: string | null;
  currentAnswer: string;
  isStreaming: boolean;
  isProcessing: boolean;
  error: string | null;
  searchResults: any[];
}

/**
 * Initial QA state
 */
const initialState: QAState = {
  sessions: {},
  currentSessionId: null,
  currentAnswer: '',
  isStreaming: false,
  isProcessing: false,
  error: null,
  searchResults: [],
};

/**
 * Ask question async thunk (non-streaming)
 */
export const askQuestionAsync = createAsyncThunk<
  { answer: string; sources: SourceReference[]; sessionId: string },
  { question: string; sessionId?: string },
  { state: RootState; rejectValue: string }
>(
  'qa/askQuestionAsync',
  async ({ question, sessionId }, { rejectWithValue }) => {
    try {
      const response = await api.post('/qa/ask', { question, sessionId });
      const data = response.data;
      return {
        answer: data.data.answer,
        sources: data.data.sources || [],
        sessionId: data.data.sessionId,
      };
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to ask question');
    }
  }
);

/**
 * Get chat history async thunk
 */
export const getChatHistoryAsync = createAsyncThunk<
  QASession,
  string,
  { state: RootState; rejectValue: string }
>(
  'qa/getChatHistoryAsync',
  async (sessionId, { rejectWithValue }) => {
    try {
      const response = await api.get(`/qa/history/${sessionId}`);
      const data = response.data;
      return data.data;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch chat history');
    }
  }
);

/**
 * Delete session async thunk
 */
export const deleteSessionAsync = createAsyncThunk<
  string,
  string,
  { state: RootState; rejectValue: string }
>(
  'qa/deleteSessionAsync',
  async (sessionId, { rejectWithValue }) => {
    try {
      await api.delete(`/qa/session/${sessionId}`);
      return sessionId;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to delete session');
    }
  }
);

/**
 * QA slice
 */
const qaSlice = createSlice({
  name: 'qa',
  initialState,
  reducers: {
    /**
     * Set current session ID
     */
    setCurrentSessionId: (state, action: PayloadAction<string | null>) => {
      state.currentSessionId = action.payload;
    },

    /**
     * Add message to session
     */
    addMessage: (state, action: PayloadAction<{ sessionId: string; message: ChatMessage }>) => {
      const { sessionId, message } = action.payload;
      if (!state.sessions[sessionId]) {
        state.sessions[sessionId] = {
          sessionId,
          title: message.content.slice(0, 50),
          messages: [],
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        };
      }
      state.sessions[sessionId].messages.push(message);
      state.sessions[sessionId].updatedAt = new Date().toISOString();
    },

    /**
     * Set streaming state
     */
    setStreaming: (state, action: PayloadAction<boolean>) => {
      state.isStreaming = action.payload;
    },

    /**
     * Set current answer (for streaming)
     */
    setCurrentAnswer: (state, action: PayloadAction<string>) => {
      state.currentAnswer = action.payload;
    },

    /**
     * Append to current answer (for streaming)
     */
    appendToAnswer: (state, action: PayloadAction<string>) => {
      state.currentAnswer += action.payload;
    },

    /**
     * Clear current answer
     */
    clearCurrentAnswer: (state) => {
      state.currentAnswer = '';
    },

    /**
     * Set processing state
     */
    setProcessing: (state, action: PayloadAction<boolean>) => {
      state.isProcessing = action.payload;
    },

    /**
     * Set error
     */
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },

    /**
     * Clear error
     */
    clearError: (state) => {
      state.error = null;
    },

    /**
     * Create new session
     */
    createSession: (state, action: PayloadAction<string>) => {
      const sessionId = action.payload;
      state.sessions[sessionId] = {
        sessionId,
        title: 'New Conversation',
        messages: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      state.currentSessionId = sessionId;
    },

    /**
     * Update session title
     */
    updateSessionTitle: (state, action: PayloadAction<{ sessionId: string; title: string }>) => {
      const { sessionId, title } = action.payload;
      if (state.sessions[sessionId]) {
        state.sessions[sessionId].title = title;
      }
    },

    /**
     * Delete session (local)
     */
    removeSession: (state, action: PayloadAction<string>) => {
      const sessionId = action.payload;
      delete state.sessions[sessionId];
      if (state.currentSessionId === sessionId) {
        state.currentSessionId = null;
      }
    },

    /**
     * Clear all sessions
     */
    clearAllSessions: (state) => {
      state.sessions = {};
      state.currentSessionId = null;
    },
  },
  extraReducers: (builder) => {
    // Ask question
    builder
      .addCase(askQuestionAsync.pending, (state) => {
        state.isProcessing = true;
        state.error = null;
      })
      .addCase(askQuestionAsync.fulfilled, (state, action) => {
        state.isProcessing = false;
        const { answer, sources, sessionId } = action.payload;

        // Ensure session exists
        if (!state.sessions[sessionId]) {
          state.sessions[sessionId] = {
            sessionId,
            title: 'New Conversation',
            messages: [],
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          };
        }

        // Add assistant message with sources
        state.sessions[sessionId].messages.push({
          id: `${Date.now()}-assistant`,
          role: 'assistant',
          content: answer,
          timestamp: new Date().toISOString(),
          sources,
        });

        state.currentSessionId = sessionId;
        state.currentAnswer = '';
      })
      .addCase(askQuestionAsync.rejected, (state, action) => {
        state.isProcessing = false;
        state.error = action.payload || 'Failed to ask question';
      });

    // Get chat history
    builder
      .addCase(getChatHistoryAsync.fulfilled, (state, action) => {
        state.sessions[action.payload.sessionId] = action.payload;
      })
      .addCase(getChatHistoryAsync.rejected, (state, action) => {
        state.error = action.payload || 'Failed to fetch chat history';
      });

    // Delete session
    builder
      .addCase(deleteSessionAsync.fulfilled, (state, action) => {
        delete state.sessions[action.payload];
        if (state.currentSessionId === action.payload) {
          state.currentSessionId = null;
        }
      })
      .addCase(deleteSessionAsync.rejected, (state, action) => {
        state.error = action.payload || 'Failed to delete session';
      });
  },
});

export const {
  setCurrentSessionId,
  addMessage,
  setStreaming,
  setCurrentAnswer,
  appendToAnswer,
  clearCurrentAnswer,
  setProcessing,
  setError,
  clearError,
  createSession,
  updateSessionTitle,
  removeSession,
  clearAllSessions,
} = qaSlice.actions;

/**
 * Selectors
 */
export const selectSessions = (state: RootState) => state.qa.sessions;
export const selectCurrentSessionId = (state: RootState) => state.qa.currentSessionId;
export const selectCurrentSession = (state: RootState) =>
  state.qa.currentSessionId ? state.qa.sessions[state.qa.currentSessionId] : null;
export const selectCurrentAnswer = (state: RootState) => state.qa.currentAnswer;
export const selectIsStreaming = (state: RootState) => state.qa.isStreaming;
export const selectIsProcessing = (state: RootState) => state.qa.isProcessing;
export const selectQAError = (state: RootState) => state.qa.error;
export const selectSearchResults = (state: RootState) => state.qa.searchResults;

export default qaSlice.reducer;
