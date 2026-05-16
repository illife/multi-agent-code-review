import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = { hasError: false, error: null };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return (
        this.props.fallback || (
          <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 p-8 text-center text-slate-900">
            <div className="w-full max-w-md rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
              <h1 className="text-xl font-semibold">Something went wrong</h1>
              <p className="mt-3 text-sm text-slate-600">
                {this.state.error?.message || 'The page failed to render.'}
              </p>
              <button
                type="button"
                onClick={() => this.setState({ hasError: false, error: null })}
                className="mt-5 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
              >
                Try again
              </button>
            </div>
          </div>
        )
      );
    }

    return this.props.children;
  }
}
