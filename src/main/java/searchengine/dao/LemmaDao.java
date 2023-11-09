package searchengine.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Lemma;
import searchengine.model.Page;
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

    public Optional<List<Lemma>> getAllBySite(Site site) {
        Session session = sessionFactory.openSession();
        Query<Lemma> query = session.createQuery("from Lemma where site = :site", Lemma.class);
        query.setParameter("site", site);
        List<Lemma> lemmas = query.getResultList();
        return lemmas.isEmpty() ? Optional.empty() : Optional.of(lemmas);
    }

    public Optional<List<Lemma>> getListByLemma(Object[] lemmas) {
        Session session = sessionFactory.openSession();
        Query<Lemma> query = session.createQuery("from Lemma where lemma in :lemmas", Lemma.class);
        query.setParameterList("lemmas", lemmas);
        List<Lemma> lemmaList = query.getResultList();
        return lemmaList.isEmpty() ? Optional.empty() : Optional.of(lemmaList);
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

    public synchronized void saveOrUpdateBatch(List<Lemma> lemmas) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        int i = 0;

        for (Lemma lemma : lemmas) {
            if (i > 0 && i % BATCH_SIZE == 0){
                session.flush();
                session.clear();
            }

            if (isExists(lemma, session)) {
                session.merge(lemma);
            } else {
                session.persist(lemma);
            }

            i++;
        }

        transaction.commit();
        session.close();
    }

    @Override
    public void delete(Lemma lemma) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        session.remove(lemma);
        transaction.commit();
        session.close();
    }

    public int getCount(Site site) {
        Session session = sessionFactory.openSession();
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

        CriteriaQuery<Long> criteria = criteriaBuilder.createQuery(Long.class);
        Root<Lemma> root = criteria.from(Lemma.class);
        criteria.select(criteriaBuilder.count(root))
                .where(criteriaBuilder.equal(root.get("site"), site));

        Query<Long> query = session.createQuery(criteria);
        Long count = query.getSingleResult();
        return Math.toIntExact(count);
    }

    private boolean isExists(Lemma lemma, Session session) {
        return session.get(Lemma.class, lemma.getId()) != null;
    }
}
