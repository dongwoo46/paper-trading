import { Trash2 } from "lucide-react";
import { Chip } from "../../../shared/ui";
import type { SymbolCatalogItem } from "../../../entities/symbol/model/types";

interface Props {
  items: SymbolCatalogItem[];
  onRemove: (id: string) => void;
}

export function CatalogSelectionList({ items, onRemove }: Props) {
  return (
    <div className="chips-container">
      {items.length === 0 && <p className="empty-state">선택된 항목이 없습니다.</p>}
      {items.map((row) => {
        const id = row.symbol ?? row.ticker;
        return (
          <Chip key={`sel-${id}`} style={{ width: "100%", justifyContent: "space-between" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <span className="chip-status"></span>
              <strong>{id}</strong>
              <span style={{ color: "var(--text-secondary)", fontSize: "12px" }}>{row.name}</span>
            </div>
            <button
              onClick={() => onRemove(id!)}
              style={{ color: "var(--status-error)", border: "none", background: "transparent", cursor: "pointer", display: "flex" }}
            >
              <Trash2 size={14} />
            </button>
          </Chip>
        );
      })}
    </div>
  );
}
