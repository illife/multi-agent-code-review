import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { RootState } from '../store';

/**
 * Theme type
 */
export type Theme = 'light' | 'dark' | 'auto';

/**
 * Notification type
 */
export interface Notification {
  id: string;
  type: 'success' | 'info' | 'warning' | 'error';
  title: string;
  message?: string;
  duration?: number; // Auto-dismiss duration in ms, 0 = no auto-dismiss
  createdAt: number;
}

/**
 * Modal state
 */
export interface ModalState {
  [key: string]: boolean;
}

/**
 * UI state interface
 */
interface UIState {
  sidebarCollapsed: boolean;
  theme: Theme;
  loading: boolean;
  globalLoading: boolean;
  notifications: Notification[];
  modals: ModalState;
  activeTab: string;
  searchQuery: string;
}

/**
 * Initial UI state
 */
const initialState: UIState = {
  sidebarCollapsed: false,
  theme: (localStorage.getItem('theme') as Theme) || 'light',
  loading: false,
  globalLoading: false,
  notifications: [],
  modals: {},
  activeTab: 'dashboard',
  searchQuery: '',
};

/**
 * UI slice
 */
const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    /**
     * Toggle sidebar
     */
    toggleSidebar: (state) => {
      state.sidebarCollapsed = !state.sidebarCollapsed;
    },

    /**
     * Set sidebar collapsed state
     */
    setSidebarCollapsed: (state, action: PayloadAction<boolean>) => {
      state.sidebarCollapsed = action.payload;
    },

    /**
     * Set theme
     */
    setTheme: (state, action: PayloadAction<Theme>) => {
      state.theme = action.payload;
      localStorage.setItem('theme', action.payload);
    },

    /**
     * Set loading state
     */
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },

    /**
     * Set global loading state
     */
    setGlobalLoading: (state, action: PayloadAction<boolean>) => {
      state.globalLoading = action.payload;
    },

    /**
     * Add notification
     */
    addNotification: (state, action: PayloadAction<Omit<Notification, 'id' | 'createdAt'>>) => {
      const notification: Notification = {
        ...action.payload,
        id: `notification-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        createdAt: Date.now(),
      };
      state.notifications.push(notification);
    },

    /**
     * Remove notification
     */
    removeNotification: (state, action: PayloadAction<string>) => {
      state.notifications = state.notifications.filter((n) => n.id !== action.payload);
    },

    /**
     * Clear all notifications
     */
    clearNotifications: (state) => {
      state.notifications = [];
    },

    /**
     * Open modal
     */
    openModal: (state, action: PayloadAction<string>) => {
      state.modals[action.payload] = true;
    },

    /**
     * Close modal
     */
    closeModal: (state, action: PayloadAction<string>) => {
      state.modals[action.payload] = false;
    },

    /**
     * Toggle modal
     */
    toggleModal: (state, action: PayloadAction<string>) => {
      const modalName = action.payload;
      state.modals[modalName] = !state.modals[modalName];
    },

    /**
     * Close all modals
     */
    closeAllModals: (state) => {
      state.modals = {};
    },

    /**
     * Set active tab
     */
    setActiveTab: (state, action: PayloadAction<string>) => {
      state.activeTab = action.payload;
    },

    /**
     * Set search query
     */
    setSearchQuery: (state, action: PayloadAction<string>) => {
      state.searchQuery = action.payload;
    },

    /**
     * Clear search query
     */
    clearSearchQuery: (state) => {
      state.searchQuery = '';
    },
  },
});

export const {
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
} = uiSlice.actions;

/**
 * Selectors
 */
export const selectSidebarCollapsed = (state: RootState) => state.ui.sidebarCollapsed;
export const selectTheme = (state: RootState) => state.ui.theme;
export const selectLoading = (state: RootState) => state.ui.loading;
export const selectGlobalLoading = (state: RootState) => state.ui.globalLoading;
export const selectNotifications = (state: RootState) => state.ui.notifications;
export const selectModals = (state: RootState) => state.ui.modals;
export const selectActiveTab = (state: RootState) => state.ui.activeTab;
export const selectSearchQuery = (state: RootState) => state.ui.searchQuery;

/**
 * Helper selector to check if a modal is open
 */
export const selectIsModalOpen = (modalName: string) => (state: RootState) =>
  Boolean(state.ui.modals[modalName]);

export default uiSlice.reducer;
