// Crops a picture to a centered square, shrinks it to size x size, and re-saves it as a JPEG.
// Re-drawing the picture also removes anything hidden inside the original file. Returns the base64 text (no "data:" prefix).
export function toSquareJpegBase64(file, size = 256) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      const side = Math.min(img.width, img.height);
      const canvas = document.createElement("canvas");
      canvas.width = size;
      canvas.height = size;
      canvas.getContext("2d").drawImage(img, (img.width - side) / 2, (img.height - side) / 2, side, side, 0, 0, size, size);
      URL.revokeObjectURL(url);
      resolve(canvas.toDataURL("image/jpeg", 0.85).split(",")[1]);
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("That file could not be read as a picture. Try a JPEG, PNG or WebP."));
    };
    img.src = url;
  });
}
