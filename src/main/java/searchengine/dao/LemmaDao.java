package searchengine.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.util.HibernateUtil;

import java.util.List;
import java.util.Optional;

public class LemmaDao implements Dao<Lemma>{
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
        String query = "from " + Site.class.getSimpleName() + " where lemma like '" + lemma.getLemma() + "'";
        Lemma l = session.createQuery(query, Lemma.class).getSingleResult();
        session.close();

        return l == null ? Optional.empty() : Optional.of(l);
    }

    @Override
    public Optional<List<Lemma>> getAll() {
        Session session = sessionFactory.openSession();
        List<Lemma> lemmas = session.createQuery("from", Lemma.class).list();
        session.close();

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

    @Override
    public void update(Lemma lemma) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.merge(lemma);
        transaction.commit();
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
