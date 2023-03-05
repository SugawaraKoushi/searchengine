package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Index;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(indexes = {
        @Index(name = "path_index", columnList = "path")
})
public class Page implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "\"path\"", columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @Override
    public int hashCode() {
        int result = id;
        result = result * 31 + (path != null ? path.hashCode() : 0);
        result += code;
        result += result * 31 + (content != null ? content.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (obj instanceof Page p) {
            if (p.path != null)
                return this.path.equals(p.path) || this.id == p.id;
        }

        return false;
    }
}
