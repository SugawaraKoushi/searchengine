package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "\"indexes\"")
public class Index implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "\"rank\"", nullable = false)
    private float rank;

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
        Index index = (Index) o;
        return getLemma().getId() == index.getLemma().getId() && getPage().getId() == index.getPage().getId();
    }

    @Override
    public final int hashCode() {
        int result = id;
        result += page.hashCode();
        result += lemma.hashCode();
        result += (int) rank;
        return result;
    }
}
