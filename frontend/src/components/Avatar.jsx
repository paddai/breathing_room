// The person's picture, or a colored circle with their first letter when they have not uploaded one.
export default function Avatar({ url, name, size = 40 }) {
  const initial = (name || "?").trim().charAt(0).toUpperCase() || "?";
  if (url) return <img className="avatar" src={url} alt="Your profile" style={{ width: size, height: size }} />;
  return (
    <div className="avatar avatar-fallback" style={{ width: size, height: size, fontSize: Math.round(size * 0.45) }} aria-hidden="true">
      {initial}
    </div>
  );
}
