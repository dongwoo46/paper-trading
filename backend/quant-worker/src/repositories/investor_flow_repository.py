from __future__ import annotations

import logging
from datetime import date

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Column definitions
# ---------------------------------------------------------------------------

_INVESTOR_FLOW_COLS = [
    "trade_date", "symbol", "market",
    "foreign_buy_volume", "foreign_sell_volume", "foreign_net_volume",
    "foreign_buy_amount", "foreign_sell_amount", "foreign_net_amount",
    "institution_buy_volume", "institution_sell_volume", "institution_net_volume",
    "institution_buy_amount", "institution_sell_amount", "institution_net_amount",
    "finance_invest_buy_volume", "finance_invest_sell_volume", "finance_invest_net_volume",
    "finance_invest_buy_amount", "finance_invest_sell_amount", "finance_invest_net_amount",
    "insurance_buy_volume", "insurance_sell_volume", "insurance_net_volume",
    "insurance_buy_amount", "insurance_sell_amount", "insurance_net_amount",
    "trust_buy_volume", "trust_sell_volume", "trust_net_volume",
    "trust_buy_amount", "trust_sell_amount", "trust_net_amount",
    "private_equity_buy_volume", "private_equity_sell_volume", "private_equity_net_volume",
    "private_equity_buy_amount", "private_equity_sell_amount", "private_equity_net_amount",
    "bank_buy_volume", "bank_sell_volume", "bank_net_volume",
    "bank_buy_amount", "bank_sell_amount", "bank_net_amount",
    "other_finance_buy_volume", "other_finance_sell_volume", "other_finance_net_volume",
    "other_finance_buy_amount", "other_finance_sell_amount", "other_finance_net_amount",
    "pension_buy_volume", "pension_sell_volume", "pension_net_volume",
    "pension_buy_amount", "pension_sell_amount", "pension_net_amount",
    "individual_buy_volume", "individual_sell_volume", "individual_net_volume",
    "individual_buy_amount", "individual_sell_amount", "individual_net_amount",
]

_SHORT_SELLING_COLS = [
    "trade_date", "symbol", "market",
    "short_sell_volume", "short_sell_amount", "short_sell_ratio",
    "short_balance_volume", "short_balance_amount",
]

_PROGRAM_TRADING_COLS = [
    "trade_date", "symbol", "market",
    "arb_buy_volume", "arb_sell_volume", "arb_net_volume",
    "arb_buy_amount", "arb_sell_amount", "arb_net_amount",
    "non_arb_buy_volume", "non_arb_sell_volume", "non_arb_net_volume",
    "non_arb_buy_amount", "non_arb_sell_amount", "non_arb_net_amount",
]

_FOREIGN_HOLDING_COLS = [
    "trade_date", "symbol", "market",
    "holding_limit", "holding_volume", "exhaustion_ratio",
]


def _placeholders(cols: list[str]) -> str:
    return ", ".join(["%s"] * len(cols))


def _col_list(cols: list[str]) -> str:
    return ", ".join(cols)


def _update_set(cols: list[str], skip: set[str] | None = None) -> str:
    """Build the SET clause for ON CONFLICT DO UPDATE, skipping PK-equivalent columns."""
    exclude = skip or {"trade_date", "symbol", "market"}
    updates = [f"{c} = EXCLUDED.{c}" for c in cols if c not in exclude]
    return ", ".join(updates)


class InvestorFlowRepository:
    """Repository for investor flow data tables.

    Accepts an existing psycopg connection for each operation — connection
    lifecycle (open/close/commit) is managed by the caller.
    """

    # ------------------------------------------------------------------
    # upsert methods
    # ------------------------------------------------------------------

    def upsert_investor_flow(self, records: list[dict], conn) -> int:
        """Upsert investor_flow records. Returns number of records processed."""
        if not records:
            return 0
        rows = [tuple(r.get(c) for c in _INVESTOR_FLOW_COLS) for r in records]
        with conn.cursor() as cursor:
            cursor.executemany(self._investor_flow_upsert_query(), rows)
        return len(records)

    def upsert_short_selling(self, records: list[dict], conn) -> int:
        """Upsert short_selling records. Returns number of records processed."""
        if not records:
            return 0
        rows = [tuple(r.get(c) for c in _SHORT_SELLING_COLS) for r in records]
        with conn.cursor() as cursor:
            cursor.executemany(self._short_selling_upsert_query(), rows)
        return len(records)

    def upsert_program_trading(self, records: list[dict], conn) -> int:
        """Upsert program_trading records. Returns number of records processed."""
        if not records:
            return 0
        rows = [tuple(r.get(c) for c in _PROGRAM_TRADING_COLS) for r in records]
        with conn.cursor() as cursor:
            cursor.executemany(self._program_trading_upsert_query(), rows)
        return len(records)

    def upsert_foreign_holding(self, records: list[dict], conn) -> int:
        """Upsert foreign_holding records. Returns number of records processed."""
        if not records:
            return 0
        rows = [tuple(r.get(c) for c in _FOREIGN_HOLDING_COLS) for r in records]
        with conn.cursor() as cursor:
            cursor.executemany(self._foreign_holding_upsert_query(), rows)
        return len(records)

    # ------------------------------------------------------------------
    # find methods
    # ------------------------------------------------------------------

    def find_investor_flow(
        self,
        symbol: str,
        from_date: date,
        to_date: date,
        limit: int,
        conn,
        market: str | None = None,
    ) -> list[dict]:
        """Find investor_flow records for a symbol within a date range."""
        query, params = self._build_find_query(
            _INVESTOR_FLOW_COLS, "investor_flow", symbol, from_date, to_date, limit, market
        )
        with conn.cursor() as cursor:
            cursor.execute(query, params)
            rows = cursor.fetchall()
        return [dict(zip(_INVESTOR_FLOW_COLS, row)) for row in rows]

    def find_short_selling(
        self,
        symbol: str,
        from_date: date,
        to_date: date,
        limit: int,
        conn,
        market: str | None = None,
    ) -> list[dict]:
        """Find short_selling records for a symbol within a date range."""
        query, params = self._build_find_query(
            _SHORT_SELLING_COLS, "short_selling", symbol, from_date, to_date, limit, market
        )
        with conn.cursor() as cursor:
            cursor.execute(query, params)
            rows = cursor.fetchall()
        return [dict(zip(_SHORT_SELLING_COLS, row)) for row in rows]

    def find_program_trading(
        self,
        symbol: str,
        from_date: date,
        to_date: date,
        limit: int,
        conn,
        market: str | None = None,
    ) -> list[dict]:
        """Find program_trading records for a symbol within a date range."""
        query, params = self._build_find_query(
            _PROGRAM_TRADING_COLS, "program_trading", symbol, from_date, to_date, limit, market
        )
        with conn.cursor() as cursor:
            cursor.execute(query, params)
            rows = cursor.fetchall()
        return [dict(zip(_PROGRAM_TRADING_COLS, row)) for row in rows]

    def find_foreign_holding(
        self,
        symbol: str,
        from_date: date,
        to_date: date,
        limit: int,
        conn,
        market: str | None = None,
    ) -> list[dict]:
        """Find foreign_holding records for a symbol within a date range."""
        query, params = self._build_find_query(
            _FOREIGN_HOLDING_COLS, "foreign_holding", symbol, from_date, to_date, limit, market
        )
        with conn.cursor() as cursor:
            cursor.execute(query, params)
            rows = cursor.fetchall()
        return [dict(zip(_FOREIGN_HOLDING_COLS, row)) for row in rows]

    # ------------------------------------------------------------------
    # Query builders (exposed for testing)
    # ------------------------------------------------------------------

    def _build_find_query(
        self,
        cols: list[str],
        table: str,
        symbol: str,
        from_date: date,
        to_date: date,
        limit: int,
        market: str | None,
    ) -> tuple[str, list]:
        """Build a parameterized SELECT query with optional market filter."""
        base = (
            f"SELECT {_col_list(cols)} "
            f"FROM {table} "
            f"WHERE symbol = %s AND trade_date BETWEEN %s AND %s"
        )
        params: list = [symbol, from_date, to_date]
        if market is not None:
            base += " AND market = %s"
            params.append(market)
        base += " ORDER BY trade_date ASC LIMIT %s"
        params.append(limit)
        return base, params

    def _investor_flow_upsert_query(self) -> str:
        cols = _INVESTOR_FLOW_COLS
        return (
            f"INSERT INTO investor_flow ({_col_list(cols)}) "
            f"VALUES ({_placeholders(cols)}) "
            f"ON CONFLICT (trade_date, symbol, market) DO UPDATE SET "
            f"{_update_set(cols)}"
        )

    def _short_selling_upsert_query(self) -> str:
        cols = _SHORT_SELLING_COLS
        return (
            f"INSERT INTO short_selling ({_col_list(cols)}) "
            f"VALUES ({_placeholders(cols)}) "
            f"ON CONFLICT (trade_date, symbol, market) DO UPDATE SET "
            f"{_update_set(cols)}"
        )

    def _program_trading_upsert_query(self) -> str:
        cols = _PROGRAM_TRADING_COLS
        return (
            f"INSERT INTO program_trading ({_col_list(cols)}) "
            f"VALUES ({_placeholders(cols)}) "
            f"ON CONFLICT (trade_date, symbol, market) DO UPDATE SET "
            f"{_update_set(cols)}"
        )

    def _foreign_holding_upsert_query(self) -> str:
        cols = _FOREIGN_HOLDING_COLS
        return (
            f"INSERT INTO foreign_holding ({_col_list(cols)}) "
            f"VALUES ({_placeholders(cols)}) "
            f"ON CONFLICT (trade_date, symbol, market) DO UPDATE SET "
            f"{_update_set(cols)}"
        )

    def _investor_flow_find_query(self) -> str:
        return (
            f"SELECT {_col_list(_INVESTOR_FLOW_COLS)} "
            f"FROM investor_flow "
            f"WHERE symbol = %s AND trade_date BETWEEN %s AND %s "
            f"ORDER BY trade_date ASC "
            f"LIMIT %s"
        )

    def _short_selling_find_query(self) -> str:
        return (
            f"SELECT {_col_list(_SHORT_SELLING_COLS)} "
            f"FROM short_selling "
            f"WHERE symbol = %s AND trade_date BETWEEN %s AND %s "
            f"ORDER BY trade_date ASC "
            f"LIMIT %s"
        )

    def _program_trading_find_query(self) -> str:
        return (
            f"SELECT {_col_list(_PROGRAM_TRADING_COLS)} "
            f"FROM program_trading "
            f"WHERE symbol = %s AND trade_date BETWEEN %s AND %s "
            f"ORDER BY trade_date ASC "
            f"LIMIT %s"
        )

    def _foreign_holding_find_query(self) -> str:
        return (
            f"SELECT {_col_list(_FOREIGN_HOLDING_COLS)} "
            f"FROM foreign_holding "
            f"WHERE symbol = %s AND trade_date BETWEEN %s AND %s "
            f"ORDER BY trade_date ASC "
            f"LIMIT %s"
        )
