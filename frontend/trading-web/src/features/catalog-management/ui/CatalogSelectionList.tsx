import { Trash2 } from "lucide-react";
import { Chip } from "../../../shared/ui";
import type { SymbolCatalogItem } from "../../../entities/symbol/model/types";
import { Button } from "@/shared/ui/shadcn/button";

interface Props {
  items: SymbolCatalogItem[];
  onRemove: (id: string) => void;
}

export function CatalogSelectionList({ items, onRemove }: Props) {
  return (
    <div className="flex flex-wrap gap-2.5 p-6">
      {items.length === 0 && <p className="w-full px-6 py-10 text-center text-muted-foreground">선택된 항목이 없습니다.</p>}
      {items.map((row) => {
        const id = row.symbol ?? row.ticker;
        return (
          <Chip key={`sel-${id}`} className="w-full justify-between">
            <div className="flex items-center gap-2">
              <span className="size-2 rounded-full bg-market-positive" />
              <strong>{id}</strong>
              <span className="text-xs text-muted-foreground">{row.name}</span>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="icon-xs"
              onClick={() => onRemove(id!)}
              className="text-destructive"
              aria-label={`${id} 구독 해지`}
            >
              <Trash2 size={14} />
            </Button>
          </Chip>
        );
      })}
    </div>
  );
}
