import React, { Component } from 'react';
import { AlertTriangle, RefreshCw, Home } from 'lucide-react';
import { Button } from '@/components/ui/Button';

export class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('[HireAI ErrorBoundary caught an error]:', error, errorInfo);
  }

  handleReload = () => {
    window.location.reload();
  };

  handleGoHome = () => {
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-background text-foreground flex items-center justify-center p-6">
          <div className="max-w-md w-full glass p-8 rounded-3xl border border-border shadow-2xl text-center space-y-6">
            <div className="w-16 h-16 mx-auto rounded-2xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-500">
              <AlertTriangle className="w-8 h-8" />
            </div>

            <div className="space-y-2">
              <h2 className="text-xl font-bold font-heading text-foreground">Something went wrong</h2>
              <p className="text-xs text-muted-foreground leading-relaxed">
                An unexpected interface error occurred. We have captured the details.
              </p>
              {this.state.error?.message && (
                <div className="mt-3 p-3 rounded-xl bg-surface-2 text-left font-mono text-[11px] text-rose-400 border border-border/60 overflow-x-auto">
                  {this.state.error.message}
                </div>
              )}
            </div>

            <div className="flex items-center justify-center gap-3 pt-2">
              <Button
                variant="outline"
                size="sm"
                onClick={this.handleReload}
                className="gap-1.5 text-xs font-semibold"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Reload Page</span>
              </Button>
              <Button
                variant="gradient"
                size="sm"
                onClick={this.handleGoHome}
                className="gap-1.5 text-xs font-semibold"
              >
                <Home className="w-3.5 h-3.5" />
                <span>Go to Home</span>
              </Button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
