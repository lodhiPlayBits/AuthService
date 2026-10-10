/**
 * Jittered exponential backoff for client reconnect loops.
 *
 * Pure exponential backoff has a herd problem: when the server (or the
 * network path to it) drops every client at once — restart, deploy, blip —
 * they all retry at the same instants (1s, 2s, 4s …) and hammer the
 * recovering server in lockstep. Our nginx /ws/ route even rate-limits
 * handshakes (10 r/s), so a synchronized stampede would mostly collect 429s
 * and push everyone's backoff upward together.
 *
 * "Equal jitter": wait at least half the exponential delay, plus a random
 * amount from the other half. That keeps a predictable floor (no instant
 * retry) while spreading a fleet of clients across the window.
 */

export const RECONNECT_BASE_DELAY_MS = 1000;
export const RECONNECT_MAX_DELAY_MS = 30000;

/**
 * @param {number} attempt 0-based count of consecutive failed attempts.
 * @returns {number} milliseconds to wait before the next attempt.
 */
export const getReconnectDelay = (attempt) => {
  const exponential = Math.min(
    RECONNECT_MAX_DELAY_MS,
    RECONNECT_BASE_DELAY_MS * 2 ** attempt
  );
  return Math.round(exponential / 2 + Math.random() * (exponential / 2));
};
