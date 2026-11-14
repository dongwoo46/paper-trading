Skill: Quant Development Principles

## Data Integrity

- **Look-ahead bias is strictly forbidden**: data at time T must use only information available before T.
- **Survivorship bias**: using only currently listed stocks overstates historical performance — include delisted stocks.
- **Corporate actions**: use adjusted prices for dividends and stock splits.
- **Missing values**: no forward-fill (propagating the last value = look-ahead). Handle explicitly.

## Backtesting Rules

- **Train / Test split**: develop factors on the training period only. Use the test period for final validation once.
- **Walk-forward validation**: rolling window validation to prevent overfitting.
- **Always include transaction costs**: slippage + commission + tax.
- **Fill at open price on rebalancing day** (filling at close = look-ahead).

## Implementation Rules

- **Vectorized operations first**: no pandas/numpy loops (performance + bug prevention).
- **Reproducibility**: fix random seeds, document environment variables.
- **Match formula symbols to code variable names**: use the same notation as the paper or spec.
- **Unit test factor calculations**: verify against manually computed results.

## Risk Metrics

- **Sharpe Ratio**: (annualized return − risk-free rate) / annualized volatility.
- **MDD (Max Drawdown)**: maximum peak-to-trough decline.
- **VaR**: maximum loss at 95% / 99% confidence interval.
- **Calmar Ratio**: annualized return / MDD (higher is better).

## Portfolio Constraints

- Explicit per-position size limit (default 5–10%).
- Sector concentration limit (default ≤ 30% per sector).
- Explicit leverage cap.
- Explicitly state whether short positions are allowed.

## Prohibitions

- Using future data (look-ahead bias).
- Backtesting without transaction costs.
- Confirming a strategy from a single backtest period.
- Over-optimization (excessive parameter tuning).
- Using `float` / `double` for money or quantity calculations — use `Decimal`.
