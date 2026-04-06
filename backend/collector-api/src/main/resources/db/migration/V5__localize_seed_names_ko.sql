UPDATE fred_series_catalog
SET title = CASE series_id
    WHEN 'CPIAUCSL' THEN '미국 소비자물가지수(CPI, 전체 도시소비자)'
    WHEN 'PCEPI' THEN '미국 개인소비지출 물가지수(PCE)'
    WHEN 'FEDFUNDS' THEN '연방기금금리(월)'
    WHEN 'DFF' THEN '연방기금금리(일)'
    WHEN 'GS10' THEN '미국 10년 만기 국채수익률'
    WHEN 'GS2' THEN '미국 2년 만기 국채수익률'
    WHEN 'UNRATE' THEN '미국 실업률'
    WHEN 'PAYEMS' THEN '미국 비농업 고용자수'
    WHEN 'GDPC1' THEN '미국 실질 국내총생산(GDP)'
    WHEN 'INDPRO' THEN '미국 산업생산지수'
    WHEN 'VIXCLS' THEN 'VIX 변동성 지수'
    WHEN 'M2SL' THEN '미국 M2 통화량'
    WHEN 'T10Y2Y' THEN '미국 장단기 금리차(10년-2년)'
    WHEN 'BAA10Y' THEN '회사채-국채 스프레드(Baa-10년물)'
    WHEN 'DGS1MO' THEN '미국 1개월 국채수익률'
    WHEN 'DGS3MO' THEN '미국 3개월 국채수익률'
    WHEN 'DEXKOUS' THEN '원/달러 환율'
    WHEN 'DEXUSEU' THEN '달러/유로 환율'
    WHEN 'HOUST' THEN '미국 주택착공건수'
    WHEN 'UMCSENT' THEN '미국 미시간대 소비자심리지수'
    WHEN 'NFCI' THEN '시카고 연은 금융여건지수(NFCI)'
    ELSE title
END
WHERE series_id IN (
    'CPIAUCSL', 'PCEPI', 'FEDFUNDS', 'DFF', 'GS10', 'GS2', 'UNRATE', 'PAYEMS', 'GDPC1', 'INDPRO',
    'VIXCLS', 'M2SL', 'T10Y2Y', 'BAA10Y', 'DGS1MO', 'DGS3MO', 'DEXKOUS', 'DEXUSEU', 'HOUST', 'UMCSENT', 'NFCI'
);

UPDATE pykrx_symbol_catalog
SET name = CASE symbol
    WHEN '005930' THEN '삼성전자'
    WHEN '000660' THEN 'SK하이닉스'
    WHEN '035420' THEN 'NAVER'
    WHEN '035720' THEN '카카오'
    WHEN '207940' THEN '삼성바이오로직스'
    WHEN '051910' THEN 'LG화학'
    WHEN '373220' THEN 'LG에너지솔루션'
    WHEN '247540' THEN '에코프로비엠'
    WHEN '086520' THEN '에코프로'
    WHEN '293490' THEN '카카오게임즈'
    WHEN '122630' THEN 'KODEX 레버리지'
    WHEN '252670' THEN 'KODEX 200선물인버스2X'
    ELSE name
END
WHERE symbol IN (
    '005930', '000660', '035420', '035720', '207940', '051910', '373220', '247540', '086520', '293490', '122630', '252670'
);

UPDATE yfinance_symbol_catalog
SET name = CASE ticker
    WHEN 'SPY' THEN 'SPDR S&P 500 ETF'
    WHEN 'QQQ' THEN '인베스코 QQQ ETF'
    WHEN 'DIA' THEN 'SPDR 다우존스 산업평균 ETF'
    WHEN 'IWM' THEN '아이셰어즈 러셀2000 ETF'
    WHEN 'AAPL' THEN '애플'
    WHEN 'MSFT' THEN '마이크로소프트'
    WHEN 'NVDA' THEN '엔비디아'
    WHEN 'AMZN' THEN '아마존'
    WHEN 'GOOGL' THEN '알파벳 A'
    WHEN 'META' THEN '메타 플랫폼스'
    WHEN 'TSLA' THEN '테슬라'
    WHEN '^GSPC' THEN 'S&P 500 지수'
    WHEN '^IXIC' THEN '나스닥 종합지수'
    WHEN '^VIX' THEN 'VIX 변동성 지수'
    ELSE name
END
WHERE ticker IN (
    'SPY', 'QQQ', 'DIA', 'IWM', 'AAPL', 'MSFT', 'NVDA', 'AMZN', 'GOOGL', 'META', 'TSLA', '^GSPC', '^IXIC', '^VIX'
);

