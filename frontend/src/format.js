export const money = (n) =>
  (Number(n) || 0).toLocaleString("en-US", { style: "currency", currency: "USD" });

export const pct = (n) => `${(Number(n) || 0).toFixed(1)}%`;

export const todayISO = () => new Date().toISOString().slice(0, 10);

export const num = (s) => {
  const v = parseFloat(s);
  return Number.isFinite(v) ? v : 0;
};
