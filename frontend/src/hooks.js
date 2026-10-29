import { useEffect, useState } from "react";

// Returns `value` only after it has stopped changing for `ms` milliseconds (so we do not call the API on every keystroke).
export function useDebounced(value, ms) {
  const [settled, setSettled] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setSettled(value), ms);
    return () => clearTimeout(timer);
  }, [value, ms]);
  return settled;
}
