package io.github.mateokadiu.moss.file.corrections;

/**
 * Why we're adjusting a prior-period filing.
 *
 * <p>Per OSS Guidelines §3.6: refunds, rate revisions, and evidence revisions are all reported as
 * corrections in the next current return, not as amendments to the historical return.
 */
public enum CorrectionReason {
  REFUND,
  RATE_CHANGE,
  EVIDENCE_REVISED,
  OTHER
}
