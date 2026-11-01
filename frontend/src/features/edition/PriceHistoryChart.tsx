import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { PricePeriod, PricePoint } from "../../api/types";
import { formatNumber, formatPrice } from "../../lib/format";
import { chartColors } from "./chartColors";
import {
  formatDate,
  historyLabel,
  monthName,
  monthTicks,
} from "./priceHistoryFormat";

interface PriceHistoryChartProps {
  points: PricePoint[];
  period: PricePeriod;
  className: string;
}

const TICK = { fontSize: 12, fill: chartColors.tick };

export default function PriceHistoryChart({
  points,
  period,
  className,
}: PriceHistoryChartProps) {
  const lastIndex = points.length - 1;
  return (
    <div
      role="img"
      aria-label={historyLabel(points, period)}
      className={className}
    >
      <ResponsiveContainer
        width="100%"
        height="100%"
        initialDimension={{ width: 800, height: 240 }}
      >
        <LineChart
          data={points}
          accessibilityLayer={false}
          margin={{ top: 8, right: 12, left: 0, bottom: 0 }}
        >
          <CartesianGrid vertical={false} stroke={chartColors.grid} />
          <XAxis
            dataKey="date"
            ticks={monthTicks(points)}
            tickFormatter={monthName}
            tick={TICK}
            axisLine={false}
            tickLine={false}
            tickMargin={8}
          />
          <YAxis
            tickFormatter={formatNumber}
            width={48}
            tick={TICK}
            axisLine={false}
            tickLine={false}
            domain={[
              (min: number) => Math.floor(min / 200) * 200,
              (max: number) => Math.ceil(max / 200) * 200,
            ]}
            tickCount={5}
            interval={0}
          />
          <Tooltip
            formatter={(value) => formatPrice(Number(value))}
            labelFormatter={(label) => formatDate(String(label))}
          />
          <Line
            type="linear"
            dataKey="price"
            stroke={chartColors.line}
            strokeWidth={3}
            isAnimationActive={false}
            dot={(props) => {
              const { key, cx, cy, index } = props as {
                key?: string;
                cx?: number;
                cy?: number;
                index?: number;
              };
              if (index !== lastIndex || cx == null || cy == null) {
                return <g key={key} />;
              }
              return (
                <circle
                  key={key}
                  cx={cx}
                  cy={cy}
                  r={6}
                  fill={chartColors.line}
                />
              );
            }}
            activeDot={{ r: 5, fill: chartColors.line }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
