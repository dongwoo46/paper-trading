import React from "react";
import { RefreshCw, Activity } from "lucide-react";
import type { LucideProps } from "lucide-react";

type IconComponent = React.ComponentType<LucideProps>;

// --- Base UI Components ---

export const Card = ({ children, className = "" }: { children: React.ReactNode; className?: string }) => (
  <article
    className={`bg-bg-card border border-white/12 rounded-[20px] p-6 transition-all duration-300 relative overflow-hidden flex flex-col hover:-translate-y-1 hover:border-blue-400/50 hover:shadow-2xl animate-fade-in before:absolute before:top-0 before:left-0 before:right-0 before:h-px before:bg-gradient-to-r before:from-transparent before:via-white/10 before:to-transparent ${className}`}
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
  <section className={`bg-bg-card border border-white/12 rounded-[20px] flex flex-col overflow-hidden shadow-md animate-fade-in ${className}`}>
    <div className="px-6 py-5 border-b border-white/12 flex items-center justify-between bg-white/[0.01] flex-wrap gap-4">
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
    className={`bg-[rgba(13,17,23,0.7)] backdrop-blur-[16px] border border-white/12 animate-fade-in ${className}`}
    style={{
      backgroundImage: "linear-gradient(180deg, rgba(255,255,255,0.03) 0%, rgba(255,255,255,0) 100%)",
      ...style,
    }}
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
    className={`bg-bg-input border border-white/12 px-4 py-2 rounded-xl text-[13.5px] flex items-center gap-2 transition-all hover:bg-white/5 hover:border-text-muted ${className}`}
    style={style}
  >
    {statusColor && (
      <span
        className="w-2 h-2 rounded-full shrink-0"
        style={{ background: statusColor, boxShadow: `0 0 8px ${statusColor}` }}
      />
    )}
    <div className="flex items-center gap-2 w-full">{children}</div>
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
  <div className="fixed bottom-8 right-8 bg-bg-card border border-blue-400/50 px-6 py-3.5 rounded-[16px] shadow-2xl text-sm font-semibold flex items-center gap-3 z-[100] animate-slide-in-right">
    {loading ? (
      <RefreshCw size={16} className="animate-spin-slow" />
    ) : (
      <Icon size={16} className="text-brand-primary" />
    )}
    <span>{message}</span>
  </div>
);
