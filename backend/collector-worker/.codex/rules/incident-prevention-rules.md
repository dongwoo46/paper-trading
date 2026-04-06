# Incident Prevention Rules

- Before running DB-dependent scripts, verify required env vars are set:
  - `PG_HOST`
  - `PG_DATABASE`
  - `PG_USER`
  - `PG_PASSWORD`
- For local runs, use a fixed preflight command to print missing variables first.
- For date window logic, use `datetime.timedelta` with `date` objects and avoid mixed `pd.Timedelta`/`date` conversions.
- For yfinance normalization, always flatten MultiIndex columns before column rename/select logic.
