export const money = (n) =>
  (Number(n) || 0).toLocaleString("en-US", { style: "currency", currency: "USD" });

export const pct = (n) => `${(Number(n) || 0).toFixed(1)}%`;

export const todayISO = () => new Date().toISOString().slice(0, 10);

export const num = (s) => {
  const v = parseFloat(s);
  return Number.isFinite(v) ? v : 0;
};

// "3 yr 4 mo", "11 mo", or a plain message when the debts are not cleared within the 50-year limit
export const duration = (months, paidOff = true) => {
  if (!paidOff) return "Not paid off in 50 years";
  if (!months) return "No debt";
  const y = Math.floor(months / 12);
  const m = months % 12;
  if (y === 0) return `${m} mo`;
  return m === 0 ? `${y} yr` : `${y} yr ${m} mo`;
};
