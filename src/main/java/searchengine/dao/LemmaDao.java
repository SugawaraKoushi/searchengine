package searchengine.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.*;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.util.HibernateUtil;

import java.util.*;

public class LemmaDao implements Dao<Lemma> {
    private static LemmaDao instance;
    private final static int BATCH_SIZE = 20;

    @Autowired
    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

    public static LemmaDao getInstance() {
        LemmaDao localInstance = instance;

        if (localInstance == null) {
            synchronized (LemmaDao.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new LemmaDao();
                }
            }
        }

        return localInstance;
    }

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

        Query<Lemma> query = session.createQuery("from Lemma where lemma = :lemma and site = :site", Lemma.class);
        query.setParameter("lemma", lemma.getLemma());
        query.setParameter("site", lemma.getSite());

        Lemma l;

        try {
            l = query.getSingleResult();
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

    public Optional<List<Lemma>> getLemmasByListAndSite(Object[] lemmas, Site site) {
        Session session = sessionFactory.openSession();
        Query<Lemma> query = session.createQuery("from Lemma where site = :site and lemma in :lemmas", Lemma.class);
        query.setParameterList("lemmas", lemmas);
        query.setParameter("site", site);
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

    public synchronized void saveOrUpdateBatch(Collection<Lemma> lemmas) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        int i = 0;

        for (Lemma lemma : lemmas) {
            if (i > 0 && i % BATCH_SIZE == 0) {
                session.flush();
                session.clear();
            }

            try {
                if (lemma.getId() == 0) {
                    session.persist(lemma);
                } else {
                    Lemma l = get(lemma).orElse(null);
                    lemma.setFrequency(lemma.getFrequency() + l.getFrequency());
                    lemma.setId(l.getId());
                    session.merge(lemma);
                }
            } catch (HibernateException e) {
                e.printStackTrace();
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

    public void delete(Object[] ids) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        try {
            Query query = session.createQuery("delete from Lemma where id in :ids");
            query.setParameterList("ids", ids)
                    .executeUpdate();
        } catch (Exception e) {
            System.out.println("Lemma id");
        }

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
}
