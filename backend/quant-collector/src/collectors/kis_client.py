from __future__ import annotations

import os
import time
from dataclasses import dataclass
from datetime import date, datetime, timedelta
from typing import Iterable

import requests
from dotenv import load_dotenv


@dataclass(frozen=True)
class KisConfig:
    mode: str
    app_key: str
    app_secret: str
    base_url: str
    token_url: str
    timeout_seconds: float
    throttle_seconds: float
    redis_host: str
    redis_port: int
    redis_db: int
    redis_enabled: bool
    db_enabled: bool


@dataclass(frozen=True)
class _CachedToken:
    token: str
    expires_at: datetime


class KisClient:
    """Minimal KIS REST client for domestic stock historical candles."""

    def __init__(self, config: KisConfig | None = None) -> None:
        self._config = config or load_kis_config_from_env()
        self._access_token: _CachedToken | None = None

    def fetch_period_price(
        self,
        symbol: str,
        start_date: date,
        end_date: date,
        period_div_code: str,
        adjusted: bool,
    ) -> list[dict[str, object]]:
        token = self._token()
        url = f"{self._config.base_url}/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice"
        headers = {
            "Content-Type": "application/json",
            "authorization": f"Bearer {token}",
            "appkey": self._config.app_key,
            "appsecret": self._config.app_secret,
            "tr_id": "FHKST03010100",
        }
        params = {
            "FID_COND_MRKT_DIV_CODE": "J",
            "FID_INPUT_ISCD": symbol,
            "FID_INPUT_DATE_1": start_date.strftime("%Y%m%d"),
            "FID_INPUT_DATE_2": end_date.strftime("%Y%m%d"),
            "FID_PERIOD_DIV_CODE": period_div_code,
            # KIS convention: 0 adjusted price, 1 original price.
            "FID_ORG_ADJ_PRC": "0" if adjusted else "1",
        }
        response = requests.get(
            url,
            headers=headers,
            params=params,
            timeout=self._config.timeout_seconds,
        )
        if self._config.throttle_seconds > 0:
            time.sleep(self._config.throttle_seconds)
        data = self._parse_json(response)
        rt_cd = str(data.get("rt_cd", ""))
        if rt_cd not in ("0", ""):
            msg = data.get("msg1") or data.get("msg_cd") or "KIS period price request failed"
            raise RuntimeError(str(msg))
        output = data.get("output2") or []
        if not isinstance(output, list):
            raise RuntimeError("KIS period price response output2 is not a list")
        return [item for item in output if isinstance(item, dict)]

    def _token(self) -> str:
        now = datetime.now()
        if self._access_token and self._access_token.expires_at > now:
            return self._access_token.token

        cached = self._load_cached_token(now)
        if cached is not None:
            self._access_token = cached
            return cached.token

        response = requests.post(
            self._config.token_url,
            headers={"Content-Type": "application/json"},
            json={
                "grant_type": "client_credentials",
                "appkey": self._config.app_key,
                "appsecret": self._config.app_secret,
            },
            timeout=self._config.timeout_seconds,
        )
        data = self._parse_json(response)
        token = data.get("access_token")
        if not token:
            msg = data.get("error_description") or data.get("msg1") or "KIS token response has no access_token"
            raise RuntimeError(str(msg))
        cached_token = _CachedToken(
            token=str(token),
            expires_at=_resolve_expires_at(data),
        )
        self._access_token = cached_token
        self._save_cached_token(cached_token)
        return cached_token.token

    def _load_cached_token(self, now: datetime) -> _CachedToken | None:
        redis_token = self._load_redis_token()
        if redis_token:
            # Redis TTL already enforces validity.
            return _CachedToken(token=redis_token, expires_at=now + timedelta(hours=1))

        db_token = self._load_db_token(now)
        if db_token is not None:
            self._save_redis_token(db_token)
            return db_token

        return None

    def _load_redis_token(self) -> str | None:
        if not self._config.redis_enabled:
            return None
        try:
            import redis

            client = redis.Redis(
                host=self._config.redis_host,
                port=self._config.redis_port,
                db=self._config.redis_db,
                decode_responses=True,
            )
            token = client.get(_redis_key(self._config.mode))
            return str(token) if token else None
        except Exception:
            return None

    def _save_redis_token(self, token: _CachedToken) -> None:
        if not self._config.redis_enabled:
            return
        ttl = int((token.expires_at - datetime.now()).total_seconds())
        if ttl <= 0:
            return
        try:
            import redis

            client = redis.Redis(
                host=self._config.redis_host,
                port=self._config.redis_port,
                db=self._config.redis_db,
                decode_responses=True,
            )
            client.set(_redis_key(self._config.mode), token.token, ex=ttl)
        except Exception:
            return

    def _load_db_token(self, now: datetime) -> _CachedToken | None:
        if not self._config.db_enabled:
            return None
        try:
            import psycopg

            with psycopg.connect(
                host=os.getenv("PG_HOST", "localhost"),
                port=int(os.getenv("PG_PORT", "5432")),
                dbname=os.getenv("PG_DATABASE", "paper"),
                user=os.getenv("PG_USER", "paper"),
                password=os.getenv("PG_PASSWORD", "paper"),
            ) as conn:
                with conn.cursor() as cur:
                    cur.execute(
                        "SELECT token, expires_at FROM kis_access_token_cache WHERE mode = %s",
                        [self._config.mode],
                    )
                    row = cur.fetchone()
            if row is None:
                return None
            token, expires_at = row
            if expires_at <= now:
                return None
            return _CachedToken(token=str(token), expires_at=expires_at)
        except Exception:
            return None

    def _save_db_token(self, token: _CachedToken) -> None:
        if not self._config.db_enabled:
            return
        try:
            import psycopg

            with psycopg.connect(
                host=os.getenv("PG_HOST", "localhost"),
                port=int(os.getenv("PG_PORT", "5432")),
                dbname=os.getenv("PG_DATABASE", "paper"),
                user=os.getenv("PG_USER", "paper"),
                password=os.getenv("PG_PASSWORD", "paper"),
            ) as conn:
                with conn.cursor() as cur:
                    cur.execute(
                        "INSERT INTO kis_access_token_cache (mode, token, expires_at) "
                        "VALUES (%s, %s, %s) "
                        "ON CONFLICT (mode) DO UPDATE SET "
                        "token = EXCLUDED.token, expires_at = EXCLUDED.expires_at, updated_at = CURRENT_TIMESTAMP",
                        [self._config.mode, token.token, token.expires_at],
                    )
                conn.commit()
        except Exception:
            return

    def _save_cached_token(self, token: _CachedToken) -> None:
        self._save_redis_token(token)
        self._save_db_token(token)

    def _parse_json(self, response: requests.Response) -> dict[str, object]:
        try:
            response.raise_for_status()
            data = response.json()
        except requests.RequestException as exc:
            raise RuntimeError(f"KIS HTTP request failed: {exc}") from exc
        except ValueError as exc:
            raise RuntimeError("KIS response is not valid JSON") from exc
        if not isinstance(data, dict):
            raise RuntimeError("KIS response root is not an object")
        return data


def iter_date_chunks(start_date: date, end_date: date, chunk_days: int) -> Iterable[tuple[date, date]]:
    current = start_date
    while current <= end_date:
        chunk_end = min(current + timedelta(days=chunk_days - 1), end_date)
        yield current, chunk_end
        current = chunk_end + timedelta(days=1)


def load_kis_config_from_env() -> KisConfig:
    load_dotenv()
    mode = os.getenv("KIS_OHLCV_MODE", os.getenv("KIS_MODE", "live")).lower()
    is_paper = mode in ("paper", "kis_paper")
    normalized_mode = "paper" if is_paper else "live"

    app_key = os.getenv("KIS_PAPER_APP_KEY" if is_paper else "KIS_APP_KEY", "")
    app_secret = os.getenv("KIS_PAPER_APP_SECRET" if is_paper else "KIS_APP_SECRET", "")
    if not app_key or not app_secret:
        suffix = "KIS_PAPER_APP_KEY/KIS_PAPER_APP_SECRET" if is_paper else "KIS_APP_KEY/KIS_APP_SECRET"
        raise RuntimeError(f"KIS credentials are missing: set {suffix}")

    default_base = (
        "https://openapivts.koreainvestment.com:29443"
        if is_paper
        else "https://openapi.koreainvestment.com:9443"
    )
    base_url = os.getenv("KIS_PAPER_REST_BASE_URL" if is_paper else "KIS_REST_BASE_URL", default_base)
    token_url = os.getenv(
        "KIS_PAPER_TOKEN_URL" if is_paper else "KIS_TOKEN_URL",
        f"{base_url}/oauth2/tokenP",
    )
    return KisConfig(
        mode=normalized_mode,
        app_key=app_key,
        app_secret=app_secret,
        base_url=base_url.rstrip("/"),
        token_url=token_url,
        timeout_seconds=float(os.getenv("KIS_TIMEOUT_SECONDS", "10")),
        throttle_seconds=float(os.getenv("KIS_THROTTLE_SECONDS", "0.05")),
        redis_host=os.getenv("REDIS_HOST", "localhost"),
        redis_port=int(os.getenv("REDIS_PORT", "6379")),
        redis_db=int(os.getenv("REDIS_DB", "0")),
        redis_enabled=os.getenv("KIS_TOKEN_REDIS_ENABLED", "true").lower() in ("1", "true", "yes", "on"),
        db_enabled=os.getenv("KIS_TOKEN_DB_ENABLED", "true").lower() in ("1", "true", "yes", "on"),
    )


def _redis_key(mode: str) -> str:
    return f"kis:token:{mode}"


def _resolve_expires_at(data: dict[str, object]) -> datetime:
    expires_in = data.get("expires_in")
    if expires_in is not None:
        try:
            seconds = int(str(expires_in))
            if seconds > 0:
                return datetime.now() + timedelta(seconds=seconds)
        except ValueError:
            pass

    absolute = data.get("access_token_token_expired")
    if absolute:
        raw = str(absolute).strip()
        for fmt in ("%Y-%m-%d %H:%M:%S", "%Y%m%d%H%M%S"):
            try:
                return datetime.strptime(raw, fmt)
            except ValueError:
                continue

    return datetime.now() + timedelta(hours=24)
