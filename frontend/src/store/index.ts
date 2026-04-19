/**
 * Redux store exports
 */
export { store } from './store';
export type { RootState, AppDispatch } from './store';

/**
 * Redux hooks exports
 */
export { useAppDispatch, useAppSelector, useAppStore } from './hooks';

/**
 * Auth slice exports
 */
export { default as authReducer } from './slices/authSlice';
export type { User } from './slices/authSlice';
export {
  loginAsync,
  logoutAsync,
  fetchCurrentUserAsync,
  logout,
  setUser,
  setToken,
} from './slices/authSlice';

/**
 * Document slice exports
 */
export { default as documentReducer } from './slices/documentSlice';
export type { Document } from './slices/documentSlice';
export {
  fetchDocumentsAsync,
  uploadDocumentAsync,
  deleteDocumentAsync,
  fetchDocumentByIdAsync,
  updateDocumentTagsAsync,
  setDocuments,
  setCurrentDocument,
  setUploadStatus,
  setUploadProgress,
  resetUploadState,
  setFilters,
  clearFilters,
  updateDocumentInList,
} from './slices/documentSlice';

/**
 * QA slice exports
 */
export { default as qaReducer } from './slices/qaSlice';
export type { ChatMessage, QASession, SourceReference } from './slices/qaSlice';
export {
  askQuestionAsync,
  getChatHistoryAsync,
  deleteSessionAsync,
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
} from './slices/qaSlice';

/**
 * UI slice exports
 */
export { default as uiReducer } from './slices/uiSlice';
export type { Theme, Notification } from './slices/uiSlice';
export {
  toggleSidebar,
  setSidebarCollapsed,
  setTheme,
  setLoading,
  setGlobalLoading,
  addNotification,
  removeNotification,
  clearNotifications,
  openModal,
  closeModal,
  toggleModal,
  closeAllModals,
  setActiveTab,
  setSearchQuery,
  clearSearchQuery,
} from './slices/uiSlice';
