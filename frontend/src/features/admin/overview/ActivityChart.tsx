import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  XAxis,
} from "recharts";
import type { AdminActivityDay } from "../../../api/types";
import { formatNumber } from "../../../lib/format";
import { activityColors } from "./chartColors";

const TICK = { fontSize: 12, fill: activityColors.tick };

function shortDate(iso: string | undefined): string {
  if (!iso) return "";
  const [, month, day] = iso.split("-");
  return `${day}.${month}`;
}

export default function ActivityChart({ days }: { days: AdminActivityDay[] }) {
  const searches = days.reduce((sum, d) => sum + (d.searches ?? 0), 0);
  const beacons = days.reduce((sum, d) => sum + (d.beacons ?? 0), 0);
  return (
    <div
      role="img"
      aria-label={`Поиски и новые маячки за ${days.length} дней: всего поисков ${formatNumber(searches)}, маячков ${formatNumber(beacons)}`}
      className="h-[220px] w-full"
    >
      <ResponsiveContainer
        width="100%"
        height="100%"
        initialDimension={{ width: 800, height: 220 }}
      >
        <BarChart
          data={days}
          accessibilityLayer={false}
          margin={{ top: 8, right: 4, left: 4, bottom: 0 }}
        >
          <CartesianGrid vertical={false} horizontal={false} />
          <XAxis
            dataKey="date"
            tickFormatter={shortDate}
            tick={TICK}
            tickLine={false}
            axisLine={{ stroke: activityColors.grid }}
            tickMargin={8}
            interval="preserveStartEnd"
          />
          <Bar
            dataKey="searches"
            fill={activityColors.searches}
            radius={[3, 3, 0, 0]}
            isAnimationActive={false}
          />
          <Bar
            dataKey="beacons"
            fill={activityColors.beacons}
            radius={[3, 3, 0, 0]}
            isAnimationActive={false}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
