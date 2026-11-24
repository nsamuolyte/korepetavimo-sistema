package lt.ispk.korepetavimosistema.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mokėjimas už pamokų paketą (US-M09, US-T08, FR-24–FR-26).
 * Kortelės duomenys nesaugomi (NFR-A5) – saugomas tik išorinės sistemos ID.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "package_purchase_id", nullable = false)
    private PackagePurchase packagePurchase;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Išorinės mokėjimų sistemos transakcijos identifikatorius. */
    private String externalId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Payment() {
    }

    public Long getId() {
        return id;
    }

    public User getPayer() {
        return payer;
    }

    public void setPayer(User payer) {
        this.payer = payer;
    }

    public PackagePurchase getPackagePurchase() {
        return packagePurchase;
    }

    public void setPackagePurchase(PackagePurchase packagePurchase) {
        this.packagePurchase = packagePurchase;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
