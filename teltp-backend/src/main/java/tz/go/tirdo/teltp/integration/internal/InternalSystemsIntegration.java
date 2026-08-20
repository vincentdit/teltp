package tz.go.tirdo.teltp.integration.internal;

/**
 * Seam marker for internal TIRDO sibling systems integration. These are integrated later via API
 * once published contracts exist; nothing is built against them in v1.
 *
 * Planned integrations:
 *   - CIAP  (Consultancy & Industrial Advisory Portal) — shares conventions with this system,
 *            enabling cross-referral of corporate training contracts and consultancy engagements.
 *   - LIMS  (Laboratory Information Management System)  — training tied to lab/testing services.
 *   - ERP                                               — finance/GL reconciliation of revenue.
 *   - DMS   (Document Management System)                — authoritative document store for materials.
 *   - RMS   (Research Management System)                — research-to-training pipeline.
 *
 * When a contract is published, add a typed client interface here (one per system) following the
 * same seam pattern used for payments, notifications and meetings.
 */
public interface InternalSystemsIntegration {
}
