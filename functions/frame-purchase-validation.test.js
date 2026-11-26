const test = require("node:test");
const assert = require("node:assert/strict");
const { validateShopFramePurchase, SHOP_FRAME_PRICES } = require("./frame-purchase-validation");

const NOW = 1_800_000_000_000;
const vipExpiry = { toMillis: () => NOW + 15 * 24 * 60 * 60 * 1000 };

test("all 16 VIP frames require their exact active level and expire at vipExpiresAt", () => {
  for (const [frameId, price] of Object.entries(SHOP_FRAME_PRICES)) {
    const match = frameId.match(/^(?:vip_dragon_|vip_gold_banner_)([1-8])$/);
    if (!match) continue;
    const level = Number(match[1]);
    const result = validateShopFramePurchase(frameId, {
      vipLevel: level,
      vipExpiresAt: vipExpiry
    }, NOW);
    assert.equal(result.price, price, frameId);
    assert.equal(result.expiresAtMillis, vipExpiry.toMillis(), frameId);
  }
});

test("all 16 VIP frames reject mismatched VIP levels", () => {
  for (const frameId of Object.keys(SHOP_FRAME_PRICES)) {
    const match = frameId.match(/^(?:vip_dragon_|vip_gold_banner_)([1-8])$/);
    if (!match) continue;
    const wrongLevel = Number(match[1]) === 8 ? 7 : Number(match[1]) + 1;
    assert.throws(() => validateShopFramePurchase(frameId, {
      vipLevel: wrongLevel,
      vipExpiresAt: vipExpiry
    }, NOW), /exclusive to its matching VIP level/, frameId);
  }
});

test("VIP frames reject missing and expired VIP subscriptions", () => {
  assert.throws(() => validateShopFramePurchase("vip_dragon_1", {
    vipLevel: 1,
    vipExpiresAt: { toMillis: () => NOW }
  }, NOW), /active VIP subscription/);
  assert.throws(() => validateShopFramePurchase("vip_gold_banner_8", {
    vipLevel: 8
  }, NOW), /active VIP subscription/);
});

test("legacy five frames retain their prices and seven-day expiry", () => {
  const expected = {
    neon_cyan: 15000,
    rose_gold: 25000,
    royal_gold: 40000,
    galaxy: 60000,
    aurora: 80000
  };
  for (const [frameId, price] of Object.entries(expected)) {
    const result = validateShopFramePurchase(frameId, {}, NOW);
    assert.equal(result.price, price);
    assert.equal(result.expiresAtMillis, NOW + 7 * 24 * 60 * 60 * 1000);
  }
});

test("unknown and malformed VIP frame IDs are rejected", () => {
  for (const frameId of ["vip_dragon_0", "vip_dragon_9", "vip_gold_banner_10", "neon_blue"]) {
    assert.throws(() => validateShopFramePurchase(frameId, {}, NOW), /Invalid frame item/);
  }
});
