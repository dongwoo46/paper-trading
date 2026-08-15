import { Component, type ErrorInfo, type ReactNode } from "react";
import { AlertTriangle, RotateCcw } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "./shadcn/alert";
import { Button } from "./shadcn/button";

type RouteErrorPageProps = {
  onRetry?: () => void;
};

export function RouteErrorPage({ onRetry }: RouteErrorPageProps) {
  const retry = onRetry ?? (() => window.location.reload());

  return (
    <div className="mx-auto flex min-h-80 w-full max-w-2xl items-center px-4 py-12">
      <Alert variant="destructive" className="gap-x-3 p-5 sm:p-6">
        <AlertTriangle aria-hidden="true" className="mt-0.5" />
        <AlertTitle className="text-base">페이지를 불러오지 못했습니다</AlertTitle>
        <AlertDescription className="space-y-4">
          <p>일시적인 오류가 발생했습니다. 다시 시도해 주세요.</p>
          <Button type="button" variant="outline" onClick={retry}>
            <RotateCcw aria-hidden="true" />
            다시 시도
          </Button>
        </AlertDescription>
      </Alert>
    </div>
  );
}

type RouteErrorBoundaryProps = {
  children: ReactNode;
  onRetry?: () => void;
  resetKey?: string;
};

type RouteErrorBoundaryState = {
  hasError: boolean;
};

export class RouteErrorBoundary extends Component<
  RouteErrorBoundaryProps,
  RouteErrorBoundaryState
> {
  state: RouteErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): RouteErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("Route render failed", error, info);
  }

  componentDidUpdate(previousProps: RouteErrorBoundaryProps) {
    if (
      this.state.hasError &&
      previousProps.resetKey !== this.props.resetKey
    ) {
      this.setState({ hasError: false });
    }
  }

  private retry = () => {
    if (this.props.onRetry) {
      this.props.onRetry();
      this.setState({ hasError: false });
      return;
    }
    window.location.reload();
  };

  render() {
    return this.state.hasError
      ? <RouteErrorPage onRetry={this.retry} />
      : this.props.children;
  }
}
