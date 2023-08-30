package searchengine.dao;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Index;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.util.HibernateUtil;

import java.util.List;
import java.util.Optional;

public class IndexDao implements Dao<Index> {
    @Autowired
    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    @Override
    public Optional<Index> get(int id) {
        Session session = sessionFactory.openSession();
        Index index = session.get(Index.class, id);
        session.close();

        return index == null ? Optional.empty() : Optional.of(index);
    }

    @Override
    public Optional<Index> get(Index index) {
        Session session = sessionFactory.openSession();
        Index i = session.createQuery("from", Index.class).getSingleResult();
        session.close();

        return i == null ? Optional.empty() : Optional.of(i);
    }

    public Optional<List<Index>> getList(Page page) {
        Session session = sessionFactory.openSession();
        String query = "from " + Index.class.getSimpleName() + " where page_id = " + page.getId();
        List<Index> indexes = session.createQuery(query).getResultList();
        session.close();

        return indexes.isEmpty() ? Optional.empty() : Optional.of(indexes);
    }

    @Override
    public Optional<List<Index>> getAll() {
        Session session = sessionFactory.openSession();
        List<Index> indexes = session.createQuery("from", Index.class).list();
        session.close();

        return Optional.of(indexes);
    }

    @Override
    public void save(Index index) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.persist(index);
        transaction.commit();
        session.close();
    }

    @Override
    public void update(Index index) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.merge(index);
        transaction.commit();
        session.close();
    }

    @Override
    public void delete(Index index) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.delete(index);
        transaction.commit();
        session.close();
    }
}