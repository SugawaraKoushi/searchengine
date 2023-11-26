package searchengine.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table
public class Lemma implements Serializable, Comparable<Lemma>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "lemma")
    private List<Index> indexes;

    @Override
    public int compareTo(Lemma l) {
        return Integer.compare(this.frequency, l.frequency);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Lemma lemma = (Lemma) o;
        return getLemma() != null &&
                getSite().getId() == lemma.getSite().getId() &&
                Objects.equals(getLemma(), lemma.getLemma());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("{%s}", lemma);
    }

}
