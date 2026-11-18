// JalyClient shop server (Cloudflare Worker + D1).
// Proves who you are with Mojang's "hasJoined" check (no passwords), stores Jolly Coins and purchases,
// and sells coin packs with Stripe Checkout. Coins are only added from Stripe's signed webhook.
import catalog from "./shop.json";

const json = (data, status = 200) =>
  new Response(JSON.stringify(data), { status, headers: { "content-type": "application/json", "access-control-allow-origin": "*" } });
const err = (msg, status = 400) => json({ error: msg }, status);
const now = () => Math.floor(Date.now() / 1000);
const hex = (bytes) => [...new Uint8Array(bytes)].map((b) => b.toString(16).padStart(2, "0")).join("");
const randomHex = (n) => hex(crypto.getRandomValues(new Uint8Array(n)));
const dashed = (u) => u.length === 32 ? `${u.slice(0, 8)}-${u.slice(8, 12)}-${u.slice(12, 16)}-${u.slice(16, 20)}-${u.slice(20)}` : u;

async function player(env, req) {
  const auth = req.headers.get("authorization") || "";
  const token = auth.startsWith("Bearer ") ? auth.slice(7) : "";
  if (!token) return null;
  return await env.DB.prepare("SELECT * FROM players WHERE token = ?").bind(token).first();
}

async function state(env, p) {
  const owned = await env.DB.prepare("SELECT item FROM owned WHERE uuid = ?").bind(p.uuid).all();
  const fresh = await env.DB.prepare("SELECT coins FROM players WHERE uuid = ?").bind(p.uuid).first();
  return { uuid: p.uuid, name: p.name, coins: fresh ? fresh.coins : p.coins, owned: owned.results.map((r) => r.item), testMode: (env.STRIPE_SECRET_KEY || "").startsWith("sk_test") };
}

async function verifyStripe(payload, header, secret) {
  const parts = Object.fromEntries((header || "").split(",").map((kv) => kv.split("=")));
  if (!parts.t || !parts.v1) return false;
  if (Math.abs(now() - Number(parts.t)) > 600) return false;
  const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const sig = hex(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(`${parts.t}.${payload}`)));
  return sig.length === parts.v1.length && crypto.subtle.timingSafeEqual
    ? crypto.subtle.timingSafeEqual(new TextEncoder().encode(sig), new TextEncoder().encode(parts.v1))
    : sig === parts.v1;
}

const page = (title, body) => new Response(`<!doctype html><meta name=viewport content="width=device-width"><title>${title}</title>
<body style="margin:0;height:100vh;display:grid;place-items:center;background:linear-gradient(#060b1f,#1e3366);color:#f6f4f7;font-family:system-ui;text-align:center">
<div><div style="font-size:64px">&#127873;</div><h1 style="color:#fde68a">${title}</h1><p>${body}</p></div></body>`, { headers: { "content-type": "text/html" } });

export default {
  async fetch(req, env) {
    const url = new URL(req.url);
    const path = url.pathname;
    if (req.method === "OPTIONS") return new Response(null, { headers: { "access-control-allow-origin": "*", "access-control-allow-headers": "authorization,content-type", "access-control-allow-methods": "GET,POST" } });

    if (path === "/" || path === "/health") return json({ ok: true, name: "JalyClient shop", testMode: (env.STRIPE_SECRET_KEY || "").startsWith("sk_test") });
    if (path === "/catalog") return json(catalog);

    // 1) client asks for a one-time server id, 2) joins it through Mojang, 3) we check hasJoined.
    if (path === "/auth/start" && req.method === "POST") {
      const serverId = randomHex(16);
      await env.DB.prepare("INSERT INTO nonces (server_id, created) VALUES (?, ?)").bind(serverId, now()).run();
      await env.DB.prepare("DELETE FROM nonces WHERE created < ?").bind(now() - 600).run();
      return json({ serverId });
    }
    if (path === "/auth/finish" && req.method === "POST") {
      const { name, serverId } = await req.json();
      const nonce = await env.DB.prepare("DELETE FROM nonces WHERE server_id = ? RETURNING server_id").bind(serverId || "").first();
      if (!nonce) return err("unknown or used login code", 401);
      const r = await fetch(`https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${encodeURIComponent(name)}&serverId=${encodeURIComponent(serverId)}`);
      if (r.status !== 200) return err("Mojang could not confirm this account", 401);
      const prof = await r.json();
      const uuid = dashed(prof.id);
      const token = randomHex(32);
      await env.DB.prepare("INSERT INTO players (uuid, name, coins, token, created) VALUES (?, ?, 0, ?, ?) ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, token = excluded.token")
        .bind(uuid, prof.name, token, now()).run();
      const p = await env.DB.prepare("SELECT * FROM players WHERE uuid = ?").bind(uuid).first();
      return json({ token, ...(await state(env, p)) });
    }

    if (path === "/me") {
      const p = await player(env, req);
      return p ? json(await state(env, p)) : err("not signed in", 401);
    }

    if (path === "/buy" && req.method === "POST") {
      const p = await player(env, req);
      if (!p) return err("not signed in", 401);
      const { item } = await req.json();
      const it = catalog.items.find((i) => i.id === item);
      if (!it) return err("unknown item");
      const have = await env.DB.prepare("SELECT 1 FROM owned WHERE uuid = ? AND item = ?").bind(p.uuid, item).first();
      if (have) return json(await state(env, p));
      const res = await env.DB.prepare("UPDATE players SET coins = coins - ? WHERE uuid = ? AND coins >= ?").bind(it.price, p.uuid, it.price).run();
      if (!res.meta.changes) return err("not enough Jolly Coins", 402);
      await env.DB.prepare("INSERT OR IGNORE INTO owned (uuid, item, created) VALUES (?, ?, ?)").bind(p.uuid, item, now()).run();
      return json(await state(env, p));
    }

    if (path === "/checkout" && req.method === "POST") {
      const p = await player(env, req);
      if (!p) return err("not signed in", 401);
      if (!env.STRIPE_SECRET_KEY) return err("payments are not set up yet", 503);
      const { pack } = await req.json();
      const pk = catalog.packs.find((x) => x.id === pack);
      if (!pk) return err("unknown pack");
      const form = new URLSearchParams({
        mode: "payment",
        success_url: `${url.origin}/paid`,
        cancel_url: `${url.origin}/cancelled`,
        client_reference_id: p.uuid,
        "metadata[uuid]": p.uuid,
        "metadata[pack]": pk.id,
        "line_items[0][quantity]": "1",
        "line_items[0][price_data][currency]": "eur",
        "line_items[0][price_data][unit_amount]": String(pk.cents),
        "line_items[0][price_data][product_data][name]": `${pk.name} - JalyClient`,
        "line_items[0][price_data][product_data][description]": `For ${p.name}. Cosmetic currency for JalyClient, no gameplay advantage.`,
      });
      const r = await fetch("https://api.stripe.com/v1/checkout/sessions", {
        method: "POST",
        headers: { authorization: `Bearer ${env.STRIPE_SECRET_KEY}`, "content-type": "application/x-www-form-urlencoded" },
        body: form,
      });
      const s = await r.json();
      if (!r.ok) return err(s.error ? s.error.message : "Stripe error", 502);
      return json({ url: s.url });
    }

    if (path === "/stripe/webhook" && req.method === "POST") {
      const payload = await req.text();
      if (!(await verifyStripe(payload, req.headers.get("stripe-signature"), env.STRIPE_WEBHOOK_SECRET || ""))) return err("bad signature", 400);
      const ev = JSON.parse(payload);
      if (ev.type === "checkout.session.completed" && ev.data.object.payment_status === "paid") {
        const s = ev.data.object;
        const pk = catalog.packs.find((x) => x.id === (s.metadata || {}).pack);
        const uuid = (s.metadata || {}).uuid;
        if (pk && uuid && s.amount_total === pk.cents) {
          const ins = await env.DB.prepare("INSERT OR IGNORE INTO payments (session_id, uuid, coins, cents, created) VALUES (?, ?, ?, ?, ?)")
            .bind(s.id, uuid, pk.coins, pk.cents, now()).run();
          if (ins.meta.changes) await env.DB.prepare("UPDATE players SET coins = coins + ? WHERE uuid = ?").bind(pk.coins, uuid).run();
        }
      }
      return json({ received: true });
    }

    // What others are wearing (so JalyClient players can see each other's cosmetics).
    if (path === "/equip" && req.method === "POST") {
      const p = await player(env, req);
      if (!p) return err("not signed in", 401);
      const data = await req.text();
      if (data.length > 2000) return err("too big");
      await env.DB.prepare("INSERT INTO equipped (uuid, data, updated) VALUES (?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET data = excluded.data, updated = excluded.updated").bind(p.uuid, data, now()).run();
      return json({ ok: true });
    }
    if (path === "/cosmetics") {
      const ids = (url.searchParams.get("uuids") || "").split(",").filter((x) => /^[0-9a-f-]{32,36}$/i.test(x)).slice(0, 100);
      if (!ids.length) return json({});
      const rows = await env.DB.prepare(`SELECT uuid, data FROM equipped WHERE uuid IN (${ids.map(() => "?").join(",")})`).bind(...ids).all();
      return json(Object.fromEntries(rows.results.map((r) => [r.uuid, JSON.parse(r.data)])));
    }

    if (path === "/paid") return page("Thank you!", "Your Jolly Coins are on their way. Go back to Minecraft - the shopkeeper is waiting (grumpily).");
    if (path === "/cancelled") return page("No worries", "Nothing was charged. You can close this tab.");
    return err("not found", 404);
  },
};
