import React from "react";
import { RefreshCw, Activity } from "lucide-react";

// --- Base UI Components ---

export const Card = ({ children, className = "" }: { children: React.ReactNode; className?: string }) => (
  <article className={`card fade-in ${className}`}>
    {children}
  </article>
);

export const SectionCard = ({ title, icon: Icon, children, headerAction, className = "" }: { 
  title: string; 
  icon?: any; 
  children: React.ReactNode; 
  headerAction?: React.ReactNode; 
  className?: string;
}) => (
  <section className={`section-card fade-in ${className}`}>
    <div className="section-card-header">
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        {Icon && <Icon size={18} className="brand-icon" />}
        <h3>{title}</h3>
      </div>
      {headerAction && <div className="header-action-group">{headerAction}</div>}
    </div>
    <div className="scroll-container">
      {children}
    </div>
  </section>
);

export const GlassPanel = ({ children, className = "", style = {} }: { children: React.ReactNode; className?: string; style?: React.CSSProperties }) => (
  <div className={`glass-panel fade-in ${className}`} style={style}>
    {children}
  </div>
);

export const Chip = ({ children, statusColor, className = "", style = {} }: { 
  children: React.ReactNode; 
  statusColor?: string; 
  className?: string; 
  style?: React.CSSProperties;
}) => (
  <div className={`chip ${className}`} style={style}>
    {statusColor && (
      <span 
        className="chip-status" 
        style={{ 
          background: statusColor, 
          boxShadow: `0 0 8px ${statusColor}` 
        }} 
      />
    )}
    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', width: '100%' }}>
      {children}
    </div>
  </div>
);

// --- Layout & Feedback Components ---

export const StatusBar = ({ message, loading, icon: Icon = Activity }: { message: string; loading: boolean; icon?: any }) => (
  <div className="status-bar">
    {loading ? <RefreshCw size={16} className="spin" /> : <Icon size={16} color="var(--brand-primary)" />}
    <span>{message}</span>
  </div>
);
