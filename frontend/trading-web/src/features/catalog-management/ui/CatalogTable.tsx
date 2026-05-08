import type { SymbolCatalogItem } from "../../../entities/symbol/model/types";

interface Props {
  rows: SymbolCatalogItem[];
  onToggle: (id: string, enabled: boolean) => void;
}

const TH = "bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap";
const TD = "px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-text-secondary";
const BTN_BASE = "inline-flex items-center justify-center gap-2.5 px-3 py-1.5 rounded-xl font-semibold text-xs cursor-pointer transition-all border whitespace-nowrap";
const BTN_PRIMARY = `${BTN_BASE} bg-gradient-to-br from-blue-500 to-emerald-500 text-white shadow border-transparent hover:brightness-110`;
const BTN_DANGER = `${BTN_BASE} bg-red-500/8 text-red-500 border-red-500/20 hover:bg-red-500/15`;

export function CatalogTable({ rows, onToggle }: Props) {
  if (rows.length === 0) {
    return <div className="py-10 px-6 text-center text-text-muted italic">표시할 데이터가 없습니다.</div>;
  }

  return (
    <div className="overflow-x-auto rounded-[16px] bg-black/20 border border-white/12 flex-1 min-h-[300px]" style={{ maxHeight: "600px" }}>
      <table className="w-full border-collapse">
        <thead>
          <tr>
            <th className={TH}>심볼</th>
            <th className={TH}>종목명</th>
            <th className={TH}>마켓</th>
            <th className={`${TH} text-center`}>구독</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const id = row.symbol ?? row.ticker;
            return (
              <tr key={id} className="hover:bg-white/[0.02]">
                <td className={TD} style={{ fontWeight: 700, color: "var(--brand-primary)" }}>{id}</td>
                <td className={TD}>{row.name}</td>
                <td className={TD}>{row.market}</td>
                <td className={`${TD} text-center`}>
                  <button
                    className={row.enabled ? BTN_DANGER : BTN_PRIMARY}
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
