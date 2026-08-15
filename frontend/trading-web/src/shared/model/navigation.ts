import {
  BarChart2,
  BookOpen,
  CandlestickChart,
  ChartLine,
  ClipboardList,
  Globe,
  History,
  Home,
  ReceiptText,
  Wallet,
  Zap,
  type LucideIcon,
} from "lucide-react";

export type NavigationItem = {
  to: string;
  label: string;
  description: string;
  icon: LucideIcon;
  end?: boolean;
};

export type NavigationGroup = {
  id: "overview" | "trading" | "market-data" | "analysis";
  label: string;
  items: NavigationItem[];
};

export const navigationGroups: NavigationGroup[] = [
  {
    id: "overview",
    label: "개요",
    items: [
      {
        to: "/",
        label: "홈",
        description: "트레이딩 워크스테이션의 주요 작업을 시작합니다.",
        icon: Home,
        end: true,
      },
      {
        to: "/account",
        label: "계좌·포지션",
        description: "계좌 잔고와 보유 포지션을 확인합니다.",
        icon: Wallet,
      },
    ],
  },
  {
    id: "trading",
    label: "트레이딩",
    items: [
      {
        to: "/orders",
        label: "주문 관리",
        description: "주문을 생성하고 체결 내역을 관리합니다.",
        icon: ClipboardList,
      },
      {
        to: "/portfolio",
        label: "포트폴리오 차트",
        description: "기간별 포트폴리오 성과를 분석합니다.",
        icon: ChartLine,
      },
      {
        to: "/trading-journals",
        label: "거래 일지",
        description: "거래 판단과 결과를 기록하고 복기합니다.",
        icon: BookOpen,
      },
      {
        to: "/tax-summary",
        label: "세금 요약",
        description: "연도별 거래 손익과 세금 요약을 확인합니다.",
        icon: ReceiptText,
      },
    ],
  },
  {
    id: "market-data",
    label: "시장 데이터",
    items: [
      {
        to: "/realtime",
        label: "실시간 데이터",
        description: "거래소별 실시간 수집 상태를 관리합니다.",
        icon: Zap,
      },
      {
        to: "/historical",
        label: "과거 시세 수집",
        description: "분석에 필요한 과거 시세를 수집합니다.",
        icon: History,
      },
      {
        to: "/macro",
        label: "거시경제 지표",
        description: "시장 판단에 필요한 거시 지표를 관리합니다.",
        icon: Globe,
      },
      {
        to: "/market-unified",
        label: "통합 시세 차트",
        description: "여러 시장의 시세를 한 차트에서 비교합니다.",
        icon: CandlestickChart,
      },
    ],
  },
  {
    id: "analysis",
    label: "분석",
    items: [
      {
        to: "/chart-analysis",
        label: "차트 분석",
        description: "수집된 종목을 기술 지표와 함께 분석합니다.",
        icon: BarChart2,
      },
    ],
  },
];

export const navigationItems = navigationGroups.flatMap((group) => group.items);
