import { useDispatch, useSelector, useStore } from 'react-redux';
import type { AppDispatch, RootState } from './store';

/**
 * Use throughout the app instead of plain `useDispatch` and `useSelector`
 */
export const useAppDispatch = () => useDispatch<AppDispatch>();
export const useAppSelector = <T>(selector: (state: RootState) => T) => useSelector(selector);
export const useAppStore = () => useStore<RootState>();
