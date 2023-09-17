package searchengine.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.util.HibernateUtil;

import java.util.*;

public class LemmaDao implements Dao<Lemma>{
    private final static int BATCH_SIZE = 20;

    @Autowired
    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    @Override
    public Optional<Lemma> get(int id) {
        Session session = sessionFactory.openSession();
        Lemma lemma = session.get(Lemma.class, id);
        session.close();

        return lemma == null ? Optional.empty() : Optional.of(lemma);
    }

    @Override
    public Optional<Lemma> get(Lemma lemma) {
        Session session = sessionFactory.openSession();
        String query = "from " + Lemma.class.getSimpleName() + " where lemma = '" + lemma.getLemma() + "'";
        Lemma l;

        try {
            l = session.createQuery(query, Lemma.class).getSingleResult();
        } catch (Exception e) {
            l = null;
        } finally {
            session.close();
        }

        return l == null ? Optional.empty() : Optional.of(l);
    }

    @Override
    public Optional<List<Lemma>> getAll() {
        Session session = sessionFactory.openSession();
        List<Lemma> lemmas;

        try {
            lemmas = session.createQuery("from " + Lemma.class.getSimpleName(), Lemma.class).getResultList();
        } catch (Exception e) {
            lemmas = new ArrayList<>();
        } finally {
            session.close();
        }

        return Optional.of(lemmas);
    }

    @Override
    public void save(Lemma lemma) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.persist(lemma);
        transaction.commit();
        session.close();
    }

    public void saveBatch(Collection<Lemma> lemmas) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int i = 0;

        for (Lemma lemma : lemmas) {
            if (i > 0 && i % BATCH_SIZE == 0) {
                session.flush();
                session.clear();
            }
            session.persist(lemma);
            i++;
        }

        session.close();
    }

    @Override
    public void update(Lemma lemma) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.merge(lemma);
        transaction.commit();
        session.close();
    }

    public void updateBatch(Collection<Lemma> lemmas) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int i = 0;

        for (Lemma lemma : lemmas) {
            if (i > 0 && i % BATCH_SIZE == 0) {
                session.flush();
                session.clear();
            }
            session.merge(lemma);
            i++;
        }

        session.close();
    }

    @Override
    public void delete(Lemma lemma) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.delete(lemma);
        transaction.commit();
        session.close();
    }
}
