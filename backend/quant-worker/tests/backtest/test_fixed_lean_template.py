from __future__ import annotations

import ast
import importlib.util
import sys
from datetime import datetime
from decimal import Decimal
from types import ModuleType, SimpleNamespace
from uuid import UUID

from src.backtest.domain import BacktestRunCreateRequest
from src.backtest.repository import InMemoryBacktestRunRepository
from src.backtest.service import BacktestRunService
from src.backtest.workspace import LeanWorkspaceBuilder
from tests.backtest.test_domain import run_request_payload


def test_fixed_template_submits_signal_close_sized_moo_without_scheduler(
    tmp_path,
    monkeypatch,
) -> None:
    workspace = _workspace_with_immediate_entry(tmp_path)
    algorithm_imports = _algorithm_imports_module()
    monkeypatch.setitem(sys.modules, "AlgorithmImports", algorithm_imports)
    for module_name in ("cost_profiles", "runtime", "strategy_loader"):
        monkeypatch.delitem(sys.modules, module_name, raising=False)
    monkeypatch.syspath_prepend(str(workspace.project_dir))
    module = _load_template_module(workspace.project_dir / "main.py")
    algorithm = module.FixedDslBacktestAlgorithm()

    algorithm.initialize()

    assert algorithm.add_equity_call == {
        "symbol": "005930",
        "resolution": algorithm_imports.Resolution.DAILY,
        "market": "krx",
        "fill_forward": False,
        "data_normalization_mode": algorithm_imports.DataNormalizationMode.RAW,
    }
    security = algorithm.asset
    assert isinstance(security.buying_power_model, module.ProjectBuyingPowerModel)
    assert isinstance(security.fee_model, module.ProjectFeeModel)
    assert isinstance(security.slippage_model, module.ProjectSlippageModel)
    assert isinstance(security.fill_model, module.ProjectFillModel)
    assert algorithm.scheduled_time_rule is None

    signal_bar = _Slice(
        "005930",
        _TradeBar(open_price="50", close_price="20"),
    )
    algorithm.on_data(signal_bar)

    assert algorithm.market_orders == []
    assert algorithm.market_on_open_orders == [("005930", 4)]
    assert algorithm.portfolio["005930"].quantity == 0
    assert algorithm.portfolio["005930"].invested is False
    security.price = "10"
    security.current_bar = _TradeBar(open_price="10", close_price="10")
    assert security.slippage_model.get_slippage_approximation(
        security,
        SimpleNamespace(),
    ) == 0.01
    sell_fee = security.fee_model.get_order_fee(
        SimpleNamespace(
            security=security,
            order=SimpleNamespace(
                absolute_quantity=2,
                direction=algorithm_imports.OrderDirection.SELL,
            ),
        )
    )
    assert sell_fee.value.currency == "KRW"
    assert str(sell_fee.value.amount) == "0.045954"


def test_fixed_template_is_reviewed_static_code_without_dynamic_execution() -> None:
    template_root = LeanWorkspaceBuilder.default_template_root()
    sources = [path.read_text("utf-8") for path in sorted(template_root.glob("*.py"))]
    main_source = (template_root / "main.py").read_text("utf-8")

    for source in sources:
        tree = ast.parse(source)
        called_names = {
            node.func.id
            for node in ast.walk(tree)
            if isinstance(node, ast.Call) and isinstance(node.func, ast.Name)
        }
        assert not ({"eval", "exec", "compile", "__import__"} & called_names)
        assert "importlib" not in source
        assert "generated_strategy" not in source
    assert "after_market_open" not in main_source
    assert ".market_order(" not in main_source
    assert ".market_on_open_order(" in main_source


def test_fixed_template_rejects_unaffordable_buy_moo_before_fill_mutation(
    tmp_path,
    monkeypatch,
) -> None:
    workspace = _workspace_with_immediate_entry(tmp_path)
    algorithm_imports = _algorithm_imports_module()
    monkeypatch.setitem(sys.modules, "AlgorithmImports", algorithm_imports)
    for module_name in ("cost_profiles", "runtime", "strategy_loader"):
        monkeypatch.delitem(sys.modules, module_name, raising=False)
    monkeypatch.syspath_prepend(str(workspace.project_dir))
    module = _load_template_module(workspace.project_dir / "main.py")
    algorithm = module.FixedDslBacktestAlgorithm()
    algorithm.initialize()
    security = algorithm.asset
    security.price = "20"
    security.current_bar = _TradeBar(open_price="25", close_price="20")

    fill = security.fill_model.market_on_open_fill(
        security,
        SimpleNamespace(
            absolute_quantity=4,
            direction=algorithm_imports.OrderDirection.BUY,
            id=1,
            quantity=4,
        ),
    )

    assert fill.status == algorithm_imports.OrderStatus.INVALID
    assert fill.fill_quantity == 0
    assert fill.message == "moo_buying_power_rejected"
    assert algorithm.portfolio.cash == 100.0
    assert algorithm.portfolio["005930"].quantity == 0


def test_fixed_template_prices_moo_costs_from_open_when_close_differs(
    tmp_path,
    monkeypatch,
) -> None:
    workspace = _workspace_with_immediate_entry(tmp_path)
    algorithm_imports = _algorithm_imports_module()
    monkeypatch.setitem(sys.modules, "AlgorithmImports", algorithm_imports)
    for module_name in ("cost_profiles", "runtime", "strategy_loader"):
        monkeypatch.delitem(sys.modules, module_name, raising=False)
    monkeypatch.syspath_prepend(str(workspace.project_dir))
    module = _load_template_module(workspace.project_dir / "main.py")
    algorithm = module.FixedDslBacktestAlgorithm()
    algorithm.initialize()
    security = algorithm.asset
    security.price = "12"
    security.current_bar = _TradeBar(open_price="10", close_price="12")
    order = SimpleNamespace(
        absolute_quantity=2,
        direction=algorithm_imports.OrderDirection.BUY,
        id=1,
        quantity=2,
    )

    fill = security.fill_model.market_on_open_fill(security, order)
    fee = security.fee_model.get_order_fee(
        SimpleNamespace(security=security, order=order)
    )
    sell_order = SimpleNamespace(
        absolute_quantity=2,
        direction=algorithm_imports.OrderDirection.SELL,
        id=2,
        quantity=-2,
    )
    sell_fill = security.fill_model.market_on_open_fill(security, sell_order)
    sell_fee = security.fee_model.get_order_fee(
        SimpleNamespace(security=security, order=sell_order)
    )

    assert Decimal(str(fill.fill_price)) == Decimal("10.010")
    assert Decimal(str(fee.value.amount)) == Decimal("0.010010")
    assert Decimal(str(sell_fill.fill_price)) == Decimal("9.990")
    assert Decimal(str(sell_fee.value.amount)) == Decimal("0.045954")


def test_fixed_template_defers_buying_power_to_actual_open_fill_check(
    tmp_path,
    monkeypatch,
) -> None:
    workspace = _workspace_with_immediate_entry(tmp_path)
    algorithm_imports = _algorithm_imports_module()
    monkeypatch.setitem(sys.modules, "AlgorithmImports", algorithm_imports)
    for module_name in ("cost_profiles", "runtime", "strategy_loader"):
        monkeypatch.delitem(sys.modules, module_name, raising=False)
    monkeypatch.syspath_prepend(str(workspace.project_dir))
    module = _load_template_module(workspace.project_dir / "main.py")
    algorithm = module.FixedDslBacktestAlgorithm()
    algorithm.initialize()
    security = algorithm.asset
    security.price = "40"
    security.current_bar = _TradeBar(open_price="10", close_price="40")
    order = SimpleNamespace(
        absolute_quantity=4,
        direction=algorithm_imports.OrderDirection.BUY,
        id=1,
        quantity=4,
        type=algorithm_imports.OrderType.MARKET_ON_OPEN,
    )
    parameters = SimpleNamespace(security=security, order=order)

    default_check = algorithm_imports.BuyingPowerModel()
    assert default_check.has_sufficient_buying_power_for_order(
        parameters
    ).is_sufficient is False
    project_check = security.buying_power_model.has_sufficient_buying_power_for_order(
        parameters
    )
    if project_check.is_sufficient:
        fill = security.fill_model.market_on_open_fill(security, order)

    assert project_check.is_sufficient is True
    assert fill.status == algorithm_imports.OrderStatus.FILLED
    assert Decimal(str(fill.fill_price)) == Decimal("10.010")


def test_fixed_template_rejection_rearms_only_after_ready_false_to_true(
    tmp_path,
    monkeypatch,
) -> None:
    workspace = _workspace_with_immediate_entry(tmp_path)
    algorithm_imports = _algorithm_imports_module()
    monkeypatch.setitem(sys.modules, "AlgorithmImports", algorithm_imports)
    for module_name in ("cost_profiles", "runtime", "strategy_loader"):
        monkeypatch.delitem(sys.modules, module_name, raising=False)
    monkeypatch.syspath_prepend(str(workspace.project_dir))
    module = _load_template_module(workspace.project_dir / "main.py")
    algorithm = module.FixedDslBacktestAlgorithm()
    algorithm.initialize()

    algorithm.on_data(_Slice("005930", _TradeBar(open_price="20", close_price="20")))
    algorithm.on_order_event(
        SimpleNamespace(
            message=module.MOO_BUYING_POWER_REJECTED,
            order_id=1,
            status=algorithm_imports.OrderStatus.INVALID,
        )
    )
    algorithm.on_data(_Slice("005930", _TradeBar(open_price="20", close_price="20")))
    algorithm.on_data(_Slice("005930", _TradeBar(open_price="4", close_price="4")))
    algorithm.on_data(_Slice("005930", _TradeBar(open_price="20", close_price="20")))

    assert algorithm.market_on_open_orders == [("005930", 4), ("005930", 4)]
    assert algorithm.logs == []


def test_fixed_template_exit_submits_full_position_sell_moo(
    tmp_path,
    monkeypatch,
) -> None:
    workspace = _workspace_with_immediate_entry(tmp_path)
    algorithm_imports = _algorithm_imports_module()
    monkeypatch.setitem(sys.modules, "AlgorithmImports", algorithm_imports)
    for module_name in ("cost_profiles", "runtime", "strategy_loader"):
        monkeypatch.delitem(sys.modules, module_name, raising=False)
    monkeypatch.syspath_prepend(str(workspace.project_dir))
    module = _load_template_module(workspace.project_dir / "main.py")
    algorithm = module.FixedDslBacktestAlgorithm()
    algorithm.initialize()
    holding = algorithm.portfolio["005930"]
    holding.invested = True
    holding.quantity = 7

    algorithm.on_data(_Slice("005930", _TradeBar(open_price="4", close_price="4")))

    assert algorithm.market_on_open_orders == [("005930", -7)]


def test_fixed_template_emits_one_terminal_audit_for_unfilled_moo(
    tmp_path,
    monkeypatch,
) -> None:
    workspace = _workspace_with_immediate_entry(tmp_path)
    algorithm_imports = _algorithm_imports_module()
    monkeypatch.setitem(sys.modules, "AlgorithmImports", algorithm_imports)
    for module_name in ("cost_profiles", "runtime", "strategy_loader"):
        monkeypatch.delitem(sys.modules, module_name, raising=False)
    monkeypatch.syspath_prepend(str(workspace.project_dir))
    module = _load_template_module(workspace.project_dir / "main.py")
    algorithm = module.FixedDslBacktestAlgorithm()
    algorithm.initialize()

    algorithm.on_data(_Slice("005930", _TradeBar(open_price="20", close_price="20")))
    algorithm.on_end_of_algorithm()

    assert algorithm.logs == [module.MOO_UNFILLED_AT_END]


def _workspace_with_immediate_entry(tmp_path):
    payload = run_request_payload()
    payload["initialCash"] = "100"
    payload["strategy"]["factors"] = [  # type: ignore[index]
        {
            "id": "close_price",
            "category": "price",
            "indicator": "price",
            "params": {},
            "source": {"field": "close"},
        }
    ]
    payload["strategy"]["entry"] = {  # type: ignore[index]
        "operator": "and",
        "conditions": [
            {
                "type": "greater_than",
                "left": {"field": "close"},
                "right": {"value": "10"},
            }
        ],
    }
    payload["strategy"]["exit"] = {  # type: ignore[index]
        "operator": "and",
        "conditions": [
            {
                "type": "less_than",
                "left": {"field": "close"},
                "right": {"value": "5"},
            }
        ],
    }
    run = BacktestRunService(
        InMemoryBacktestRunRepository(),
        id_factory=lambda: UUID("00000000-0000-0000-0000-000000000021"),
        clock=lambda: datetime(2024, 1, 1, 9, 0),  # noqa: DTZ001 - KST-naive
    ).create_run(BacktestRunCreateRequest.model_validate(payload))
    return LeanWorkspaceBuilder(tmp_path / "runs").build(run)


def _load_template_module(path):
    spec = importlib.util.spec_from_file_location("fixed_template_adapter", path)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _algorithm_imports_module() -> ModuleType:
    module = ModuleType("AlgorithmImports")
    module.CashAmount = _CashAmount
    module.DataNormalizationMode = SimpleNamespace(RAW="RAW")
    module.BuyingPowerModel = _BuyingPowerModel
    module.EquityFillModel = _EquityFillModel
    module.FeeModel = _FeeModel
    module.HasSufficientBuyingPowerForOrderResult = (
        _HasSufficientBuyingPowerForOrderResult
    )
    module.OrderDirection = SimpleNamespace(BUY="BUY", SELL="SELL")
    module.OrderFee = _OrderFee
    module.OrderStatus = SimpleNamespace(
        CANCELED="CANCELED",
        FILLED="FILLED",
        INVALID="INVALID",
    )
    module.OrderType = SimpleNamespace(MARKET_ON_OPEN="MARKET_ON_OPEN")
    module.QCAlgorithm = _QCAlgorithm
    module.Resolution = SimpleNamespace(DAILY="DAILY")
    module.TradeBar = _TradeBar
    return module


class _CashAmount:
    def __init__(self, amount, currency) -> None:
        self.amount = amount
        self.currency = currency


class _OrderFee:
    def __init__(self, value) -> None:
        self.value = value


class _FeeModel:
    pass


class _HasSufficientBuyingPowerForOrderResult:
    def __init__(self, is_sufficient) -> None:
        self.is_sufficient = is_sufficient


class _BuyingPowerModel:
    def has_sufficient_buying_power_for_order(self, parameters):
        required_cash = Decimal(str(parameters.security.price)) * Decimal(
            str(parameters.order.absolute_quantity)
        )
        return _HasSufficientBuyingPowerForOrderResult(
            required_cash <= Decimal(str(parameters.security.algorithm.portfolio.cash))
        )


class _EquityFillModel:
    def market_on_open_fill(self, asset, order):
        open_price = Decimal(str(asset.cache.get_data(_TradeBar).open))
        slippage = Decimal(
            str(asset.slippage_model.get_slippage_approximation(asset, order))
        )
        if order.direction == "BUY":
            fill_price = open_price + slippage
        else:
            fill_price = open_price - slippage
        return SimpleNamespace(
            fill_price=fill_price,
            fill_quantity=order.quantity,
            message="",
            status="FILLED",
        )


class _Holding:
    def __init__(self) -> None:
        self.invested = False
        self.quantity = 0


class _Portfolio:
    def __init__(self) -> None:
        self.cash = 0
        self.total_portfolio_value = 0
        self._holdings = {}

    def __getitem__(self, symbol):
        return self._holdings.setdefault(symbol, _Holding())


class _Security:
    def __init__(self, symbol, algorithm) -> None:
        self.symbol = symbol
        self.algorithm = algorithm
        self.price = "0"
        self.current_bar = _TradeBar(open_price="0", close_price="0")
        self.cache = SimpleNamespace(get_data=lambda _type: self.current_bar)
        self.quote_currency = SimpleNamespace(symbol="KRW")
        self.fee_model = None
        self.fill_model = None
        self.slippage_model = None
        self.buying_power_model = _BuyingPowerModel()

    def set_buying_power_model(self, model) -> None:
        self.buying_power_model = model

    def set_fee_model(self, model) -> None:
        self.fee_model = model

    def set_fill_model(self, model) -> None:
        self.fill_model = model

    def set_slippage_model(self, model) -> None:
        self.slippage_model = model


class _QCAlgorithm:
    def __init__(self) -> None:
        self.portfolio = _Portfolio()
        self.securities = {}
        self.market_orders = []
        self.market_on_open_orders = []
        self.logs = []
        self.add_equity_call = None
        self.scheduled_callback = None
        self.scheduled_time_rule = None
        self.date_rules = SimpleNamespace(
            every_day=lambda symbol: ("every_day", symbol)
        )
        self.time_rules = SimpleNamespace(
            after_market_open=lambda symbol, minutes: (
                "after_market_open",
                symbol,
                minutes,
            )
        )
        self.schedule = SimpleNamespace(on=self._schedule)

    def set_start_date(self, *parts) -> None:
        self.start_date = parts

    def set_end_date(self, *parts) -> None:
        self.end_date = parts

    def set_account_currency(self, currency) -> None:
        self.account_currency = currency

    def set_cash(self, cash) -> None:
        self.portfolio.cash = cash
        self.portfolio.total_portfolio_value = cash

    def add_equity(
        self,
        symbol,
        resolution,
        *,
        market,
        fill_forward,
        data_normalization_mode,
    ):
        self.add_equity_call = {
            "symbol": symbol,
            "resolution": resolution,
            "market": market,
            "fill_forward": fill_forward,
            "data_normalization_mode": data_normalization_mode,
        }
        security = _Security(symbol, self)
        self.securities[symbol] = security
        self.portfolio[symbol]
        return security

    def _schedule(self, date_rule, time_rule, callback) -> None:
        self.scheduled_date_rule = date_rule
        self.scheduled_time_rule = time_rule
        self.scheduled_callback = callback

    def set_runtime_statistic(self, key, value) -> None:
        self.runtime_statistic = (key, value)

    def market_order(self, symbol, quantity) -> None:
        self.market_orders.append((symbol, quantity))
        holding = self.portfolio[symbol]
        holding.quantity += quantity
        holding.invested = holding.quantity != 0

    def market_on_open_order(self, symbol, quantity):
        self.market_on_open_orders.append((symbol, quantity))
        return SimpleNamespace(order_id=len(self.market_on_open_orders))

    def log(self, message) -> None:
        self.logs.append(message)


class _TradeBar:
    def __init__(self, *, open_price, close_price) -> None:
        self.end_time = datetime(2024, 1, 1, 16, 0)  # noqa: DTZ001 - fake bar
        self.open = open_price
        self.high = close_price
        self.low = open_price
        self.close = close_price
        self.volume = "1000"


class _Slice:
    def __init__(self, symbol, bar) -> None:
        self.bars = {symbol: bar}
