import { ArrowUpRight } from "lucide-react";
import { NavLink } from "react-router-dom";
import { navigationGroups } from "../../../shared/model/navigation";
import { cn } from "../../../shared/lib/utils";
import { PageHeader } from "../../../shared/ui/PageHeader";
import { buttonVariants } from "../../../shared/ui/shadcn/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "../../../shared/ui/shadcn/card";

export function HomePage() {
  return (
    <section className="flex flex-col gap-8 animate-fade-in">
      <PageHeader
        eyebrow="워크스테이션"
        title="오늘의 작업"
        description="주문과 포지션을 관리하고, 시장 데이터를 수집해 분석으로 이어지는 실제 업무를 시작하세요."
      />

      <div className="space-y-4">
        <div>
          <h2 className="text-lg font-semibold tracking-tight text-foreground">업무 영역</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            필요한 기능을 업무 흐름에 따라 모았습니다.
          </p>
        </div>

        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {navigationGroups.map((group) => {
            const items = group.items.filter((item) => item.to !== "/");
            return (
              <Card key={group.id} className="gap-0 py-0 shadow-sm">
                <CardHeader className="border-b px-5 py-4">
                  <CardTitle className="text-sm">{group.label}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-1 p-2">
                  {items.map((item) => {
                    const Icon = item.icon;
                    return (
                      <NavLink
                        key={item.to}
                        to={item.to}
                        className={cn(
                          buttonVariants({ variant: "ghost" }),
                          "group h-auto w-full justify-start gap-3 whitespace-normal px-3 py-3 text-left",
                        )}
                      >
                        <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-secondary text-secondary-foreground">
                          <Icon aria-hidden="true" size={16} />
                        </span>
                        <span className="min-w-0 flex-1">
                          <span className="block text-sm font-medium text-foreground">
                            {item.label}
                          </span>
                          <span className="mt-0.5 block text-xs leading-5 text-muted-foreground">
                            {item.description}
                          </span>
                        </span>
                        <ArrowUpRight
                          aria-hidden="true"
                          className="text-muted-foreground transition-colors group-hover:text-foreground"
                          size={15}
                        />
                      </NavLink>
                    );
                  })}
                </CardContent>
              </Card>
            );
          })}
        </div>
      </div>
    </section>
  );
}
