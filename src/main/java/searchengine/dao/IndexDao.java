package searchengine.dao;

import org.hibernate.*;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.util.HibernateUtil;

import java.util.*;

public class IndexDao implements Dao<Index> {
    private final static int BATCH_SIZE = 20;

    @Autowired
    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    @Override
    public Optional<Index> get(int id) {
        Session session;
        session = sessionFactory.openSession();
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

    public Optional<List<Index>> getListByLemma(Lemma lemma) {
        Session session = sessionFactory.openSession();
        Query<Index> query = session.createQuery("from Index where lemma = :lemma", Index.class);
        query.setParameter("lemma", lemma);
        List<Index> indexes = query.getResultList();
        session.close();
        return indexes.isEmpty() ? Optional.empty() : Optional.of(indexes);
    }

    public Optional<List<Index>> getListByPage(Page page) {
        Session session = sessionFactory.openSession();
        Query<Index> query = session.createQuery("from Index where page = :page", Index.class);
        query.setParameter("page", page);
        List<Index> indexes = query.getResultList();
        session.close();
        return indexes.isEmpty() ? Optional.empty() : Optional.of(indexes);
    }

    public Optional<Index> getListByPageAndLemma(Page page, Lemma lemma) {
        Session session = sessionFactory.openSession();
        Query<Index> query = session.createQuery("from Index where page = :page and lemma = :lemma", Index.class);
        query.setParameter("page", page)
                .setParameter("lemma", lemma);
        Index index = query.getSingleResult();

        return index == null ? Optional.empty() : Optional.of(index);
    }

    @Override
    public Optional<List<Index>> getAll() {
        Session session = sessionFactory.openSession();
        List<Index> indexes;

        try {
            indexes = session.createQuery("from " + Index.class.getSimpleName(), Index.class).getResultList();
        } catch (Exception e) {
            indexes = new ArrayList<>();
        } finally {
            session.close();
        }

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

    public void saveOrUpdateBatch(Collection<Index> indexes) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int i = 0;

        for (Index index : indexes) {
            if (i > 0 && i % BATCH_SIZE == 0) {
                session.flush();
                session.clear();
            }

            try {
                session.persist(index);
            } catch (PersistentObjectException e) {
                session.merge(index);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }

            i++;
        }

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
        session.remove(index);
        transaction.commit();
        session.close();
    }

    public void delete(Object[] ids) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        Query query = session.createQuery("delete from Index where id in :ids");
        query.setParameterList("ids", ids)
                .executeUpdate();
        transaction.commit();
        session.close();
    }
}
