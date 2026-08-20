package tz.go.tirdo.teltp.billing.entity;

/** Four payment patterns underpinning the eight revenue streams. */
public enum ChargeModel {
    ONE_TIME,        // course fee, exam fee, workshop, bootcamp
    SUBSCRIPTION,    // content subscription
    PERIODIC_RENEWAL,// certificate renewal
    CONTRACT         // corporate B2B
}
