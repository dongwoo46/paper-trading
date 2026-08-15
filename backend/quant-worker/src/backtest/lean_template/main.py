from __future__ import annotations

from datetime import date
from decimal import Decimal

from AlgorithmImports import (
    BuyingPowerModel,
    CashAmount,
    DataNormalizationMode,
    EquityFillModel,
    FeeModel,
    HasSufficientBuyingPowerForOrderResult,
    OrderDirection,
    OrderFee,
    OrderStatus,
    OrderType,
    QCAlgorithm,
    Resolution,
    TradeBar,
)
from strategy_loader import load_cost_profile, load_run_config, load_strategy

from runtime import (
    BASIS_POINTS,
    HUNDRED,
    MOO_BUYING_POWER_REJECTED,
    MOO_EXECUTION_POLICY_ID,
    MOO_GAP_BUFFER_BPS,
    MOO_UNFILLED_AT_END,
    DailyBar,
    StrategySignalEvaluator,
    calculate_fill,
    calculate_moo_buy_quantity,
)


class ProjectBuyingPowerModel(BuyingPowerModel):
    """Defer MOO affordability to the actual-open fill check below."""

    def has_sufficient_buying_power_for_order(self, parameters):
        if parameters.order.type == OrderType.MARKET_ON_OPEN:
            return HasSufficientBuyingPowerForOrderResult(True)
        return super().has_sufficient_buying_power_for_order(parameters)


class ProjectFeeModel(FeeModel):
    def __init__(self, profile) -> None:
        self.profile = profile

    def get_order_fee(self, parameters):
        base_price = _market_open_price(parameters.security)
        quantity = int(abs(parameters.order.absolute_quantity))
        side = (
            "SELL"
            if parameters.order.direction == OrderDirection.SELL
            else "BUY"
        )
        fill = calculate_fill(self.profile, side, quantity, base_price)
        fee = fill.commission + fill.sell_tax
        return OrderFee(
            CashAmount(
                float(fee),
                parameters.security.quote_currency.symbol,
            )
        )


class ProjectSlippageModel:
    def __init__(self, profile) -> None:
        self.profile = profile

    def get_slippage_approximation(self, asset, order):
        price = _market_open_price(asset)
        rate = Decimal(self.profile.slippage_bps_per_fill) / BASIS_POINTS
        return float(price * rate)


def _market_open_price(asset) -> Decimal:
    trade_bar = asset.cache.get_data(TradeBar)
    if trade_bar is None:
        return Decimal(str(asset.price))
    return Decimal(str(trade_bar.open))


class ProjectFillModel(EquityFillModel):
    """Reject unaffordable buy MOO fills before LEAN mutates cash or holdings."""

    def __init__(self, algorithm, profile) -> None:
        super().__init__()
        self.algorithm = algorithm
        self.profile = profile

    def market_on_open_fill(self, asset, order):
        fill = super().market_on_open_fill(asset, order)
        if (
            order.direction != OrderDirection.BUY
            or fill.status != OrderStatus.FILLED
            or fill.fill_quantity == 0
        ):
            return fill
        fill_price = Decimal(str(fill.fill_price))
        quantity = Decimal(str(abs(fill.fill_quantity)))
        commission_rate = (
            Decimal(self.profile.commission_bps_per_fill) / BASIS_POINTS
        )
        required_cash = quantity * fill_price * (Decimal(1) + commission_rate)
        available_cash = Decimal(str(self.algorithm.portfolio.cash))
        if required_cash > available_cash:
            fill.status = OrderStatus.INVALID
            fill.fill_quantity = 0
            fill.message = MOO_BUYING_POWER_REJECTED
        return fill


class FixedDslBacktestAlgorithm(QCAlgorithm):
    """Reviewed DSL interpreter; run JSON is data and never executable source."""

    def initialize(self) -> None:
        config = load_run_config()
        self.strategy_definition = load_strategy()
        self.cost_profile = load_cost_profile()
        self.signal_evaluator = StrategySignalEvaluator(self.strategy_definition)
        execution_policy = config["execution_policy"]
        if execution_policy != {
            "gap_buffer_bps": str(MOO_GAP_BUFFER_BPS),
            "policy_id": MOO_EXECUTION_POLICY_ID,
        }:
            raise ValueError("unsupported MOO execution policy snapshot")
        self.gap_buffer_bps = Decimal(execution_policy["gap_buffer_bps"])
        self.entry_armed = True
        self.open_moo_order_ids = set()
        self.rules_evaluated = 0

        start_date = date.fromisoformat(config["start_date"])
        end_date = date.fromisoformat(config["end_date"])
        self.set_start_date(start_date.year, start_date.month, start_date.day)
        self.set_end_date(end_date.year, end_date.month, end_date.day)
        self.set_account_currency(config["currency"])
        self.set_cash(float(Decimal(config["initial_cash"])))
        self.asset = self.add_equity(
            config["symbol"],
            Resolution.DAILY,
            market=config["lean_market"],
            fill_forward=False,
            data_normalization_mode=DataNormalizationMode.RAW,
        )
        self.asset.set_buying_power_model(ProjectBuyingPowerModel())
        self.asset.set_fee_model(ProjectFeeModel(self.cost_profile))
        self.asset.set_slippage_model(ProjectSlippageModel(self.cost_profile))
        self.asset.set_fill_model(ProjectFillModel(self, self.cost_profile))

    def on_data(self, data) -> None:
        if self.asset.symbol not in data.bars:
            return
        trade_bar = data.bars[self.asset.symbol]
        bar = DailyBar(
            date=trade_bar.end_time.date().isoformat(),
            open=Decimal(str(trade_bar.open)),
            high=Decimal(str(trade_bar.high)),
            low=Decimal(str(trade_bar.low)),
            close=Decimal(str(trade_bar.close)),
            volume=Decimal(str(trade_bar.volume)),
        )
        decision = self.signal_evaluator.update(bar)
        if not decision.ready:
            return
        self.rules_evaluated += 1
        self.set_runtime_statistic("DSL Rules Evaluated", str(self.rules_evaluated))

        invested = self.portfolio[self.asset.symbol].invested
        if not decision.entry:
            self.entry_armed = True
        if decision.exit:
            if invested and not self.open_moo_order_ids:
                quantity = int(self.portfolio[self.asset.symbol].quantity)
                if quantity > 0:
                    self._submit_moo(-quantity)
            return
        if (
            decision.entry
            and not invested
            and self.entry_armed
            and not self.open_moo_order_ids
        ):
            position_percent = Decimal(
                self.strategy_definition["risk"]["position_size_percent"]
            )
            portfolio_value = Decimal(str(self.portfolio.total_portfolio_value))
            quantity = calculate_moo_buy_quantity(
                self.cost_profile,
                signal_close_price=bar.close,
                target_value=portfolio_value * position_percent / HUNDRED,
                available_cash=Decimal(str(self.portfolio.cash)),
                gap_buffer_bps=self.gap_buffer_bps,
            )
            if quantity > 0:
                self._submit_moo(quantity)

    def _submit_moo(self, quantity: int) -> None:
        ticket = self.market_on_open_order(self.asset.symbol, quantity)
        self.open_moo_order_ids.add(ticket.order_id)

    def on_order_event(self, order_event) -> None:
        if order_event.status == OrderStatus.INVALID and (
            MOO_BUYING_POWER_REJECTED in str(order_event.message)
        ):
            self.entry_armed = False
        if order_event.status in {
            OrderStatus.CANCELED,
            OrderStatus.FILLED,
            OrderStatus.INVALID,
        }:
            self.open_moo_order_ids.discard(order_event.order_id)

    def on_end_of_algorithm(self) -> None:
        if self.open_moo_order_ids:
            self.log(MOO_UNFILLED_AT_END)
