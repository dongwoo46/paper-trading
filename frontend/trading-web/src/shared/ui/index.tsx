import React from "react";
import { RefreshCw, Activity } from "lucide-react";
import type { LucideProps } from "lucide-react";
import { cn } from "../lib/utils";
import { Alert } from "./shadcn/alert";
import { Badge } from "./shadcn/badge";
import {
  Card as ShadcnCard,
  CardContent,
  CardHeader,
  CardTitle,
} from "./shadcn/card";

type IconComponent = React.ComponentType<LucideProps>;

// --- Base UI Components ---

export const Card = ({ children, className = "" }: { children: React.ReactNode; className?: string }) => (
  <ShadcnCard
    className={cn(
      "relative animate-fade-in p-6 transition-all duration-300 hover:-translate-y-1 hover:border-primary/50 hover:shadow-lg",
      className,
    )}
  >
    {children}
  </ShadcnCard>
);

export const SectionCard = ({
  title,
  icon: Icon,
  children,
  headerAction,
  className = "",
}: {
  title: string;
  icon?: IconComponent;
  children: React.ReactNode;
  headerAction?: React.ReactNode;
  className?: string;
}) => (
  <ShadcnCard className={cn("animate-fade-in gap-0 shadow-sm", className)}>
    <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-4 border-b px-6 py-5">
      <div className="flex items-center gap-2.5">
        {Icon && <Icon size={18} className="text-primary" />}
        <CardTitle>{title}</CardTitle>
      </div>
      {headerAction && <div>{headerAction}</div>}
    </CardHeader>
    <CardContent className="w-full overflow-x-auto px-0 pb-2">
      {children}
    </CardContent>
  </ShadcnCard>
);

export const GlassPanel = ({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) => (
  <ShadcnCard
    className={cn("animate-fade-in gap-0 bg-muted p-0", className)}
  >
    {children}
  </ShadcnCard>
);

export const Chip = ({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) => (
  <Badge
    variant="secondary"
    className={cn("h-auto px-4 py-2 text-sm transition-all hover:bg-muted", className)}
  >
    <span className="flex w-full items-center gap-2 text-foreground">{children}</span>
  </Badge>
);

// --- Layout & Feedback Components ---

export const StatusBar = ({
  message,
  loading,
  icon: Icon = Activity,
}: {
  message: string;
  loading: boolean;
  icon?: IconComponent;
}) => (
  <Alert className="fixed right-8 bottom-8 z-100 flex w-auto items-center gap-3 px-6 py-3.5 font-semibold shadow-lg animate-slide-in-right">
    {loading ? (
      <RefreshCw size={16} className="animate-spin-slow text-muted-foreground" />
    ) : (
      <Icon size={16} className="text-primary" />
    )}
    <span>{message}</span>
  </Alert>
);
