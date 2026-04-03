function getFilenameFromContentDisposition(disp: string | null): string | null {
  if (!disp) return null;

  // RFC 5987: filename*=UTF-8''...
  const mStar = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(disp);
  if (mStar?.[1]) {
    try {
      return decodeURIComponent(mStar[1]);
    } catch {
      return mStar[1];
    }
  }

  // Basic: filename="..."
  const m = /filename\s*=\s*"?([^";]+)"?/i.exec(disp);
  return m?.[1] ?? null;
}

async function readErrorMessage(res: Response): Promise<string> {
  const ct = res.headers.get("Content-Type") || "";
  try {
    if (ct.includes("application/json")) {
      const body = (await res.json()) as { message?: string; error?: string };
      return body?.message || body?.error || `请求失败(${res.status})`;
    }
    const text = await res.text();
    return text ? text.slice(0, 200) : `请求失败(${res.status})`;
  } catch {
    return `请求失败(${res.status})`;
  }
}

function looksLikeFileResponse(res: Response): boolean {
  const disp = res.headers.get("Content-Disposition") || "";
  if (/attachment/i.test(disp)) return true;

  const ct = (res.headers.get("Content-Type") || "").toLowerCase();
  if (!ct) return false;
  if (ct.includes("application/json")) return false;
  if (ct.includes("text/html")) return false;
  // Typical file types we return in this project
  if (ct.includes("application/octet-stream")) return true;
  if (ct.includes("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) return true;
  return false;
}

export async function downloadByFetch(
  input: RequestInfo | URL,
  init: RequestInit | undefined,
  fallbackName: string
): Promise<void> {
  const res = await fetch(input, init);
  if (!res.ok) {
    throw new Error(await readErrorMessage(res));
  }

  // Some servers return 200 with an HTML/JSON error body (e.g., redirect to login page).
  // In that case, treat it as an error instead of downloading a broken file.
  if (!looksLikeFileResponse(res)) {
    throw new Error(await readErrorMessage(res));
  }

  const blob = await res.blob();
  const name = getFilenameFromContentDisposition(res.headers.get("Content-Disposition")) || fallbackName;

  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.style.display = "none";
  document.body.appendChild(a);
  a.click();
  a.remove();

  // Avoid revoking too early (can cancel downloads in some browsers)
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}

