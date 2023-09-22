package searchengine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table
public class Site implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "status_time", nullable = false)
    private Date statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(name = "\"name\"", columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "site")
    private Set<Page> pages;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "site")
    private List<Lemma> lemmas;

    @Override
    public int hashCode() {
        int result = id;
        result += result * 31 + (lastError.isEmpty() ? 0 : lastError.hashCode());
        result += result * 31 + (url.isEmpty() ? 0 : url.hashCode());
        result += result * 31 + (name.isEmpty() ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Site s) {
            if (!s.url.isBlank()) {
                return this.url.equals(s.url);
            }
        }

        return false;
    }
}
