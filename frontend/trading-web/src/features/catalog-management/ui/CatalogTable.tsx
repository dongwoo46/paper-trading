import type { SymbolCatalogItem } from "../../../entities/symbol/model/types";

interface Props {
  rows: SymbolCatalogItem[];
  onToggle: (id: string, enabled: boolean) => void;
}

export function CatalogTable({ rows, onToggle }: Props) {
  if (rows.length === 0) {
    return <div className="empty-state">표시할 데이터가 없습니다.</div>;
  }

  return (
    <div className="table-container" style={{ maxHeight: "600px" }}>
      <table>
        <thead>
          <tr>
            <th>심볼</th>
            <th>종목명</th>
            <th>마켓</th>
            <th style={{ textAlign: "center" }}>구독</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const id = row.symbol ?? row.ticker;
            return (
              <tr key={id}>
                <td style={{ fontWeight: 700, color: "var(--brand-primary)" }}>{id}</td>
                <td>{row.name}</td>
                <td>{row.market}</td>
                <td style={{ textAlign: "center" }}>
                  <button
                    className={`btn ${row.enabled ? "btn-danger" : "btn-primary"}`}
                    style={{ padding: "6px 12px", fontSize: "12px" }}
                    onClick={() => onToggle(id!, row.enabled)}
                  >
                    {row.enabled ? "해지" : "추가"}
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
