package tz.go.tirdo.teltp.corporate.entity;

/** Formal B2B training-contract lifecycle (mirrors CIAP consultancy workflow shape). */
public enum ContractStatus {
    DRAFT, QUOTED, APPROVED, ACTIVE, COMPLETED, CANCELLED;

    public boolean canTransitionTo(ContractStatus target) {
        return switch (this) {
            case DRAFT -> target == QUOTED || target == CANCELLED;
            case QUOTED -> target == APPROVED || target == CANCELLED;
            case APPROVED -> target == ACTIVE || target == CANCELLED;
            case ACTIVE -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
