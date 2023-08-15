package searchengine.dao;

import java.util.List;
import java.util.Optional;

public interface Dao<T> {
    Optional<T> get(int id);

    Optional<T> get(T t);

    Optional<List<T>> getList(T t);

    Optional<List<T>> getAll();

    void save(T t);

    void update(T t);

    void delete(T t);
}
