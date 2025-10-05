package io.github.mateokadiu.moss.enrich.evidence;

/**
 * The five accepted evidence-of-location piece types under Council Implementing Reg. 282/2011 Art.
 * 24f.
 */
public enum EvidenceType {
  /** Billing address country reported by the customer's payment method record. */
  BILLING_ADDRESS,
  /** IP-based country lookup (MaxMind / equivalent provider). */
  IP_GEOLOCATION,
  /** Issuing bank country of the payment card. */
  BANK_LOCATION,
  /** Mobile country code from the SIM (rarely available outside telco scenarios). */
  MCC_PHONE,
  /** Country declared by the customer during checkout — last-resort. */
  CUSTOMER_DECLARED
}
