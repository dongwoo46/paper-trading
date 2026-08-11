import type { SymbolCatalogItem } from "../../../entities/symbol/model/types";
import { Button } from "@/shared/ui/shadcn/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/ui/shadcn/table";

interface Props {
  rows: SymbolCatalogItem[];
  onToggle: (id: string, enabled: boolean) => void;
}

export function CatalogTable({ rows, onToggle }: Props) {
  if (rows.length === 0) {
    return <div className="px-6 py-10 text-center text-muted-foreground">표시할 데이터가 없습니다.</div>;
  }

  return (
    <div className="max-h-150 min-h-75 flex-1 overflow-auto rounded-xl border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>심볼</TableHead>
            <TableHead>종목명</TableHead>
            <TableHead>마켓</TableHead>
            <TableHead className="text-center">구독</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.map((row) => {
            const id = row.symbol ?? row.ticker;
            return (
              <TableRow key={id}>
                <TableCell className="font-bold text-primary">{id}</TableCell>
                <TableCell className="text-muted-foreground">{row.name}</TableCell>
                <TableCell className="text-muted-foreground">{row.market}</TableCell>
                <TableCell className="text-center">
                  <Button
                    size="sm"
                    variant={row.enabled ? "destructive" : "default"}
                    onClick={() => onToggle(id!, row.enabled)}
                  >
                    {row.enabled ? "해지" : "추가"}
                  </Button>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
