import React from "react";
import { RefreshCw, Activity } from "lucide-react";
import type { LucideProps } from "lucide-react";

type IconComponent = React.ComponentType<LucideProps>;

// --- Base UI Components ---

export const Card = ({ children, className = "" }: { children: React.ReactNode; className?: string }) => (
  <article
    className={`bg-bg-card border border-border-primary rounded-[16px] p-6 transition-all duration-300 relative flex flex-col hover:-translate-y-1 hover:border-brand-primary/50 hover:shadow-lg animate-fade-in ${className}`}
  >
    {children}
  </article>
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
  <section className={`bg-bg-card border border-border-primary rounded-[16px] flex flex-col overflow-hidden shadow-sm animate-fade-in ${className}`}>
    <div className="px-6 py-5 border-b border-border-primary flex items-center justify-between flex-wrap gap-4 bg-bg-card">
      <div className="flex items-center gap-2.5">
        {Icon && <Icon size={18} className="text-brand-primary" />}
        <h3 className="text-[17px] font-semibold text-text-primary">{title}</h3>
      </div>
      {headerAction && <div>{headerAction}</div>}
    </div>
    <div className="w-full overflow-x-auto pb-2">
      {children}
    </div>
  </section>
);

export const GlassPanel = ({
  children,
  className = "",
  style = {},
}: {
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}) => (
  <div
    className={`bg-bg-input border border-border-primary rounded-[16px] animate-fade-in ${className}`}
    style={style}
  >
    {children}
  </div>
);

export const Chip = ({
  children,
  statusColor,
  className = "",
  style = {},
}: {
  children: React.ReactNode;
  statusColor?: string;
  className?: string;
  style?: React.CSSProperties;
}) => (
  <div
    className={`bg-bg-input px-4 py-2 rounded-xl text-[13.5px] flex items-center gap-2 transition-all hover:bg-border-primary/50 ${className}`}
    style={style}
  >
    {statusColor && (
      <span
        className="w-2 h-2 rounded-full shrink-0"
        style={{ background: statusColor, boxShadow: `0 0 4px ${statusColor}` }}
      />
    )}
    <div className="flex items-center gap-2 w-full text-text-primary">{children}</div>
  </div>
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
  <div className="fixed bottom-8 right-8 bg-bg-card border border-border-primary px-6 py-3.5 rounded-[16px] shadow-lg text-sm font-semibold flex items-center gap-3 z-[100] animate-slide-in-right">
    {loading ? (
      <RefreshCw size={16} className="animate-spin-slow text-text-muted" />
    ) : (
      <Icon size={16} className="text-brand-primary" />
    )}
    <span className="text-text-primary">{message}</span>
  </div>
);

