import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../auth/authContext";
import Avatar from "../components/Avatar";
import { Card, Field, Notice, PageHead } from "../components/ui";
import { toSquareJpegBase64 } from "../imageTools";

export default function ProfilePage() {
  const { user, profile, avatarUrl, refreshProfile, logout } = useAuth();
  const fileRef = useRef(null);
  const [name, setName] = useState("");
  const [nameError, setNameError] = useState(null);
  const [nameMessage, setNameMessage] = useState(null);
  const [pictureError, setPictureError] = useState(null);
  const [pictureMessage, setPictureMessage] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setName(profile?.displayName ?? "");
  }, [profile]);

  async function saveName(e) {
    e.preventDefault();
    setNameError(null);
    setNameMessage(null);
    if (name.trim().length > 60) {
      setNameError("Use 60 characters or fewer.");
      return;
    }
    try {
      await api.put("/profile", { displayName: name });
      await refreshProfile();
      setNameMessage("Name saved.");
    } catch (err) {
      setNameError(err.fieldErrors?.displayName || err.message);
    }
  }

  async function pickPicture(e) {
    const file = e.target.files?.[0];
    e.target.value = ""; // lets the same file be chosen again later
    if (!file) return;
    setPictureError(null);
    setPictureMessage(null);
    if (!/^image\/(jpeg|png|webp)$/.test(file.type)) {
      setPictureError("Use a JPEG, PNG or WebP picture.");
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      setPictureError("That picture is very large. Choose one under 10 MB.");
      return;
    }
    setBusy(true);
    try {
      const data = await toSquareJpegBase64(file, 256);
      await api.put("/profile/avatar", { contentType: "image/jpeg", data });
      await refreshProfile();
      setPictureMessage("Picture saved.");
    } catch (err) {
      setPictureError(err.fieldErrors?.picture || err.message);
    } finally {
      setBusy(false);
    }
  }

  async function removePicture() {
    setPictureError(null);
    setPictureMessage(null);
    setBusy(true);
    try {
      await api.del("/profile/avatar");
      await refreshProfile();
      setPictureMessage("Picture removed.");
    } catch (err) {
      setPictureError(err.message);
    } finally {
      setBusy(false);
    }
  }

  const shownName = profile?.displayName || user.email;

  return (
    <>
      <PageHead title="Profile" sub="Your name, your picture, and how to sign out." />

      <Card>
        <div className="profile-head">
          <Avatar url={avatarUrl} name={shownName} size={96} />
          <div>
            <h3 style={{ margin: 0 }}>{shownName}</h3>
            <p className="muted" style={{ margin: "2px 0 12px" }}>
              {user.email}
            </p>
            <div className="row">
              <button type="button" className="btn small" disabled={busy} onClick={() => fileRef.current.click()}>
                {avatarUrl ? "Change picture" : "Upload a picture"}
              </button>
              {avatarUrl && (
                <button type="button" className="btn danger small" disabled={busy} onClick={removePicture}>
                  Remove picture
                </button>
              )}
              <input ref={fileRef} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={pickPicture} />
            </div>
            {busy && <p className="muted small">Saving your picture...</p>}
            {pictureError && <p className="field-error small">{pictureError}</p>}
            {pictureMessage && <p className="small" style={{ color: "var(--teal)", fontWeight: 600 }}>{pictureMessage}</p>}
            <p className="disclaimer">JPEG, PNG or WebP. Your picture is cropped to a square and shrunk before it is saved, and only you can see it.</p>
          </div>
        </div>
      </Card>

      <Card title="Display name">
        <form onSubmit={saveName} className="row" style={{ alignItems: "flex-start" }}>
          <div style={{ flex: 1, maxWidth: 320 }}>
            <Field label="Name shown in the app" hint="Optional. Leave blank to show your email." error={nameError}>
              <input value={name} onChange={(e) => setName(e.target.value)} maxLength={80} />
            </Field>
          </div>
          <button className="btn" type="submit" style={{ marginTop: 22 }}>
            Save name
          </button>
        </form>
        {nameMessage && <Notice>{nameMessage}</Notice>}
      </Card>

      <Card title="Sign out">
        <p className="muted">Signing out ends this session on this browser. Your data stays saved in your account.</p>
        <div className="row">
          <button type="button" className="btn" onClick={logout}>
            Sign out
          </button>
          <Link to="/settings" className="small">
            Delete my account (in Settings)
          </Link>
        </div>
      </Card>
    </>
  );
}
