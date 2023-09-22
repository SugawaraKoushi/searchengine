package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
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

    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER, mappedBy = "lemma")
    private List<Index> indexes;

    @Override
    public int hashCode() {
        int result = id;
        result += result * 31 + (lemma.isBlank() ? 0 : lemma.hashCode());
        result += result * 31 + frequency;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Lemma l) {
            if (!l.lemma.isBlank()) {
                return l.lemma.equals(this.lemma);
            }
        }

        return false;
    }

    @Override
    public int compareTo(Lemma l) {
        return Integer.compare(this.frequency, l.frequency);
    }
}
