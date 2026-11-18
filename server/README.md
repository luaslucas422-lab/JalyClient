# Jolly Shop server

A Cloudflare Worker + D1 database that keeps Jolly Coins and premium cosmetics.

- **Sign-in:** uses Mojang's own `joinServer`/`hasJoined` check, the same one Minecraft servers use. Passwords and tokens never reach this server.
- **Coins:** only added by Stripe's signed `checkout.session.completed` webhook. Each payment is counted once.
- **Packs:** €1 = 100, €5 = 550, €10 = 1200, €20 = 2600.

## Turning it on

1. Add these repo secrets: `CLOUDFLARE_API_TOKEN` (with the "Edit Cloudflare Workers" template + D1 edit) and `CLOUDFLARE_ACCOUNT_ID`.
2. Run the **Shop server** workflow. It creates the database, deploys, and writes the live URL to `endpoint.txt`. The game reads that file.
3. Payments (test mode first): add `STRIPE_SECRET_KEY` (`sk_test_...`) and `STRIPE_WEBHOOK_SECRET`. The webhook URL is `<worker url>/stripe/webhook`, with the event `checkout.session.completed`.
4. Real money needs a Stripe account owned and verified by an adult. Switch to `sk_live_...` only after that.
