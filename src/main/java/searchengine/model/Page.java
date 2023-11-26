package searchengine.model;

import jakarta.persistence.Index;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(indexes = {
        @Index(name = "path_index", columnList = "path")
})
public class Page implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    @ToString.Exclude
    private Site site;

    @Column(name = "\"path\"", columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "page")
    @ToString.Exclude
    private List<searchengine.model.Index> indexes;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Page page = (Page) o;
        return getPath() != null && Objects.equals(getPath() , page.getPath());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
