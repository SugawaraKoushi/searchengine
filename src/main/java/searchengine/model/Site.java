package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table
public class Site {
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
}
