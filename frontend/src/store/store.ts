import { configureStore, combineReducers } from '@reduxjs/toolkit';
import { setupListeners } from '@reduxjs/toolkit/query';
import authReducer from './slices/authSlice';
import documentReducer from './slices/documentSlice';
import qaReducer from './slices/qaSlice';
import uiReducer from './slices/uiSlice';
import notificationReducer from './slices/notificationSlice';

/**
 * Root reducer combining all slices
 */
const rootReducer = combineReducers({
  auth: authReducer,
  documents: documentReducer,
  qa: qaReducer,
  ui: uiReducer,
  notifications: notificationReducer,
});

/**
 * Redux store configuration with RTK Query support
 */
export const store = configureStore({
  reducer: rootReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        // Ignore these action types from the serializable check
        ignoredActions: ['persist/PERSIST', 'persist/REHYDRATE'],
      },
    }).concat(),
  devTools: process.env.NODE_ENV !== 'production',
});

// Enable refetchOnFocus/refetchOnReconnect behaviors for RTK Query
setupListeners(store.dispatch);

/**
 * TypeScript types for the store
 */
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

export default store;
