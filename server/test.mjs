// Local test of the shop server (wrangler dev --local). Run: node test.mjs http://127.0.0.1:8787
import crypto from "node:crypto";
import { execSync } from "node:child_process";
const base = process.argv[2];
let fails = 0;
const check = (name, ok) => { console.log((ok ? "PASS " : "FAIL ") + name); if (!ok) fails++; };
const j = async (path, opts = {}) => { const r = await fetch(base + path, opts); return { status: r.status, body: await r.json().catch(() => ({})) }; };
const sql = (q) => execSync(`wrangler d1 execute jalyclient-shop --local --command "${q}"`, { stdio: "pipe" });

check("health", (await j("/health")).body.ok === true);
check("catalog has items", (await j("/catalog")).body.items.length >= 10);
check("auth start gives a server id", (await j("/auth/start", { method: "POST" })).body.serverId.length === 32);
check("bad login refused", (await j("/auth/finish", { method: "POST", body: JSON.stringify({ name: "x", serverId: "nope" }) })).status === 401);
sql("INSERT INTO players (uuid,name,coins,token,created) VALUES ('11111111-2222-3333-4444-555555555555','Tester',0,'tok123',0)");
const auth = { authorization: "Bearer tok123" };
check("me works", (await j("/me", { headers: auth })).body.coins === 0);
check("can't buy with 0 coins", (await j("/buy", { method: "POST", headers: auth, body: JSON.stringify({ item: "trail:galaxy" }) })).status === 402);
// signed Stripe webhook adds coins exactly once
const ev = JSON.stringify({ type: "checkout.session.completed", data: { object: { id: "cs_test_1", payment_status: "paid", amount_total: 500, metadata: { uuid: "11111111-2222-3333-4444-555555555555", pack: "p550" } } } });
const t = Math.floor(Date.now() / 1000);
const sig = crypto.createHmac("sha256", "whsec_test").update(`${t}.${ev}`).digest("hex");
const hook = () => j("/stripe/webhook", { method: "POST", headers: { "stripe-signature": `t=${t},v1=${sig}` }, body: ev });
check("webhook accepted", (await hook()).status === 200);
await hook();
check("coins added once (550)", (await j("/me", { headers: auth })).body.coins === 550);
check("forged webhook refused", (await j("/stripe/webhook", { method: "POST", headers: { "stripe-signature": `t=${t},v1=deadbeef` }, body: ev.replace("p550", "p2600") })).status === 400);
const bought = await j("/buy", { method: "POST", headers: auth, body: JSON.stringify({ item: "hat:crystal_crown" }) });
check("buy crystal crown", bought.status === 200 && bought.body.owned.includes("hat:crystal_crown") && bought.body.coins === 50);
check("unknown item refused", (await j("/buy", { method: "POST", headers: auth, body: JSON.stringify({ item: "hat:free_money" }) })).status === 400);
check("equip + public cosmetics", (await j("/equip", { method: "POST", headers: auth, body: JSON.stringify({ hat: "crystal_crown" }) })).status === 200
  && (await j("/cosmetics?uuids=11111111-2222-3333-4444-555555555555")).body["11111111-2222-3333-4444-555555555555"].hat === "crystal_crown");
// friends
sql("INSERT INTO players (uuid,name,coins,token,created) VALUES ('aaaaaaaa-2222-3333-4444-555555555555','Buddy',0,'tok456',0)");
const b = { authorization: "Bearer tok456" };
const post = (path, h, body) => j(path, { method: "POST", headers: h, body: JSON.stringify(body) });
check("can't friend yourself", (await post("/friends/add", auth, { name: "tester" })).status === 400);
check("unknown name refused", (await post("/friends/add", auth, { name: "Nobody123" })).status === 404);
check("request sent", (await post("/friends/add", auth, { name: "buddy" })).body.result === "sent");
const inc = (await j("/friends", { headers: b })).body;
check("buddy sees the request", inc.incoming.length === 1 && inc.incoming[0].name === "Tester");
check("accept", (await post("/friends/accept", b, { uuid: "11111111-2222-3333-4444-555555555555" })).status === 200);
await post("/presence", b, { status: "dnd", server: "play.hypixel.net" });
let mine = (await j("/friends", { headers: auth })).body;
check("now friends, sees dnd + server", mine.friends.length === 1 && mine.friends[0].status === "dnd" && mine.friends[0].server === "play.hypixel.net");
await post("/presence", b, { status: "invisible", server: "play.hypixel.net" });
mine = (await j("/friends", { headers: auth })).body;
check("appear offline hides server", mine.friends[0].status === "offline" && mine.friends[0].server === "");
check("remove friend", (await post("/friends/remove", b, { uuid: "11111111-2222-3333-4444-555555555555" })).status === 200 && (await j("/friends", { headers: auth })).body.friends.length === 0);
check("friends need sign-in", (await j("/friends")).status === 401);
console.log(fails ? `FAILED ${fails}` : "ALL SERVER TESTS PASSED");
process.exit(fails ? 1 : 0);
