const SHOP_FRAME_PRICES = Object.freeze({
  neon_cyan: 15000,
  rose_gold: 25000,
  royal_gold: 40000,
  galaxy: 60000,
  aurora: 80000,
  new_year_2027: 500000,
  vip_dragon_1: 90000,
  vip_dragon_2: 110000,
  vip_dragon_3: 130000,
  vip_dragon_4: 150000,
  vip_dragon_5: 175000,
  vip_dragon_6: 200000,
  vip_dragon_7: 230000,
  vip_dragon_8: 275000,
  vip_gold_banner_1: 90000,
  vip_gold_banner_2: 110000,
  vip_gold_banner_3: 130000,
  vip_gold_banner_4: 150000,
  vip_gold_banner_5: 175000,
  vip_gold_banner_6: 200000,
  vip_gold_banner_7: 230000,
  vip_gold_banner_8: 275000
});

const VIP_FRAME_PATTERN = /^(?:vip_dragon_|vip_gold_banner_)([1-8])$/;
const LEGACY_FRAME_IDS = new Set(["neon_cyan", "rose_gold", "royal_gold", "galaxy", "aurora"]);
const SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000;
const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000;

function validateShopFramePurchase(frameId, profile, nowMillis) {
  const price = typeof frameId === "string" ? SHOP_FRAME_PRICES[frameId] : undefined;
  if (!Number.isSafeInteger(price) || price <= 0) {
    const error = new Error("Invalid frame item.");
    error.code = "invalid-argument";
    throw error;
  }

  const vipMatch = frameId.match(VIP_FRAME_PATTERN);
  if (vipMatch) {
    const requiredLevel = Number(vipMatch[1]);
    if (!Number.isInteger(profile.vipLevel) || profile.vipLevel !== requiredLevel) {
      throw new Error("This frame is exclusive to its matching VIP level.");
    }
    const vipExpiry = profile.vipExpiresAt;
    if (!vipExpiry || typeof vipExpiry.toMillis !== "function" ||
        vipExpiry.toMillis() <= nowMillis) {
      throw new Error("An active VIP subscription is required for this frame.");
    }
    return { price, expiresAtMillis: vipExpiry.toMillis() };
  }

  if (frameId === "new_year_2027") {
    return { price, expiresAtMillis: nowMillis + THIRTY_DAYS_MS };
  }
  if (!LEGACY_FRAME_IDS.has(frameId)) {
    const error = new Error("Invalid frame item.");
    error.code = "invalid-argument";
    throw error;
  }
  return { price, expiresAtMillis: nowMillis + SEVEN_DAYS_MS };
}

module.exports = { validateShopFramePurchase, SHOP_FRAME_PRICES };
