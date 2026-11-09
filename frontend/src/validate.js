import { todayISO } from "./format";

// Checks done in the browser BEFORE sending, so people get an instant message next to the right input.
// The server checks everything again (and its messages use the same field names), so nothing here is trusted.
const blank = (v) => v === undefined || v === null || String(v).trim() === "";
const isNum = (v) => !blank(v) && Number.isFinite(Number(v));

export function validateExpense({ description, category, amount }) {
  const e = {};
  if (blank(description)) e.description = 'Enter a description, for example "Groceries at Aldi".';
  if (blank(category)) e.category = "Choose a category.";
  if (!isNum(amount)) e.amount = "Enter the amount you spent.";
  else if (Number(amount) <= 0) e.amount = "The amount must be greater than 0.";
  return e;
}

export function validateDebt({ name, balance, apr, minPayment }) {
  const e = {};
  if (blank(name)) e.name = 'Give this debt a name, for example "Visa card".';
  if (!isNum(balance) || Number(balance) <= 0) e.balance = "Enter the balance you still owe.";
  if (!isNum(apr)) e.apr = "Enter the interest rate (APR). Use 0 if there is no interest.";
  else if (Number(apr) < 0 || Number(apr) > 100) e.apr = "The APR must be between 0 and 100.";
  if (!isNum(minPayment)) e.minPayment = "Enter the minimum payment. Use 0 if you do not know it.";
  else if (Number(minPayment) < 0) e.minPayment = "The minimum payment cannot be negative.";
  return e;
}

export function validateGoal({ name, target, saved, date, monthly }) {
  const e = {};
  if (blank(name)) e.name = 'Give the goal a name, for example "Emergency fund".';
  if (!isNum(target) || Number(target) <= 0) e.targetAmount = "Enter the amount you want to reach.";
  if (!blank(saved) && (!isNum(saved) || Number(saved) < 0)) e.savedAmount = "Saved so far cannot be negative.";
  else if (isNum(target) && Number(saved) > Number(target)) e.savedAmount = "You already saved more than the target. Raise the target or lower this amount.";
  if (blank(date)) e.targetDate = "Pick the date you want to reach this goal.";
  else if (date <= todayISO()) e.targetDate = "Pick a target date in the future.";
  if (!blank(monthly) && (!isNum(monthly) || Number(monthly) < 0)) e.monthlyContribution = "The monthly amount cannot be negative.";
  return e;
}

export function validatePaycheck(s) {
  const e = {};
  if (s.payType === "salary") {
    if (!isNum(s.salaryAmount) || Number(s.salaryAmount) <= 0) e.salaryAmount = "Enter your salary for one pay period.";
  } else {
    if (!isNum(s.hourlyRate) || Number(s.hourlyRate) <= 0) e.hourlyRate = "Enter your hourly rate.";
    if (!isNum(s.regularHours) || Number(s.regularHours) <= 0) e.regularHours = "Enter the hours you work in one pay period.";
    if (!blank(s.overtimeHours) && (!isNum(s.overtimeHours) || Number(s.overtimeHours) < 0)) e.overtimeHours = "Overtime hours cannot be negative.";
    if (Number(s.overtimeHours) > 0 && !(Number(s.overtimeMultiplier) >= 1)) e.overtimeMultiplier = "The overtime multiplier must be at least 1.";
  }
  const percent = (key, v, max) => {
    if (!blank(v) && (!isNum(v) || Number(v) < 0 || Number(v) > max)) e[key] = `Enter a percent from 0 to ${max}.`;
  };
  percent("contribution401kPercent", s.k401, 100);
  percent("employerMatchRate", s.matchRate, 200);
  percent("employerMatchLimit", s.matchLimit, 100);
  if (Object.values(s.deductions || {}).some((v) => !blank(v) && (!isNum(v) || Number(v) < 0))) e.deductions = "Deduction amounts must be 0 or more.";
  return e;
}
