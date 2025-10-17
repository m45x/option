package de.option.chatgpt.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import de.option.chatgpt.entity.AktienEntity;
import de.option.chatgpt.entity.ContractEntity;
import de.option.chatgpt.entity.MarketDataEntity;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class HibernateUtil {

	private static final SessionFactory sessionFactory;

	static {
		try {
			sessionFactory = new Configuration().configure().buildSessionFactory();
		} catch (Throwable ex) {
			System.err.println("Initial SessionFactory creation failed." + ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public static void save(Object data) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			session.persist(data);
			session.getTransaction().commit();
		} finally {
			session.close();
		}
	}

	public static AktienEntity setAktieVerkauft(Long idAktie, BigDecimal verkaufspreis) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			AktienEntity aktie = session.find(AktienEntity.class, idAktie);
			if (aktie == null) {
				System.out.println("speichern fehlgeschlagen");
				return null;
			}
			aktie.setVerkkaufpreis(verkaufspreis);
			aktie.setVerkaufdatum(LocalDateTime.now());
			return session.merge(aktie);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static ArrayList<ContractEntity> getContracts(Boolean nurAktive, Integer tickerId) {
		Session session = sessionFactory.openSession();
		try {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<ContractEntity> cq = cb.createQuery(ContractEntity.class);
			Root<ContractEntity> root = cq.from(ContractEntity.class);
			List<Predicate> predicates = new ArrayList<Predicate>();

			// if (emailExact != null && !emailExact.equals(""))
			// predicates.add(cb.like(root.get(ProfileEntity_.email), emailExact));
			if (nurAktive)
				predicates.add(cb.equal(root.get("aktiv"), Boolean.TRUE));

			if (tickerId != null)
				predicates.add(cb.equal(root.get("tickerId"), tickerId));

			cq.where(predicates.toArray(new Predicate[] {}));
			TypedQuery<ContractEntity> tq = session.createQuery(cq);
			List<ContractEntity> allitems = tq.getResultList();
			return new ArrayList<ContractEntity>(allitems);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			session.close();
		}
	}

	public static ArrayList<AktienEntity> getAktien(String symbol, Boolean nurNochNichtVerkaufte) {
		Session session = sessionFactory.openSession();
		try {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<AktienEntity> cq = cb.createQuery(AktienEntity.class);
			Root<AktienEntity> root = cq.from(AktienEntity.class);
			List<Predicate> predicates = new ArrayList<Predicate>();

			// if (emailExact != null && !emailExact.equals(""))
			// predicates.add(cb.like(root.get(ProfileEntity_.email), emailExact));
			// predicates.add(cb.equal(root.get("aktiv"), Boolean.TRUE));

			if (symbol != null)
				predicates.add(cb.equal(root.get("symbol"), symbol));
			if (nurNochNichtVerkaufte)
				predicates.add(cb.isNull(root.get("verkaufdatum")));

			cq.where(predicates.toArray(new Predicate[] {}));
			TypedQuery<AktienEntity> tq = session.createQuery(cq);
			List<AktienEntity> allitems = tq.getResultList();
			return new ArrayList<AktienEntity>(allitems);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			session.close();
		}
	}

	public static ArrayList<MarketDataEntity> getMarketDataDesc(String symbol, 
			String tickerType,
			boolean vonHeute
			) {
		Session session = sessionFactory.openSession();
		try {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<MarketDataEntity> cq = cb.createQuery(MarketDataEntity.class);
			Root<MarketDataEntity> root = cq.from(MarketDataEntity.class);
			List<Predicate> predicates = new ArrayList<Predicate>();

			// if (emailExact != null && !emailExact.equals(""))
			// predicates.add(cb.like(root.get(ProfileEntity_.email), emailExact));
//			predicates.add(cb.equal(root.get("aktiv"), Boolean.TRUE));

			LocalDateTime heute = LocalDateTime.of(LocalDate.now().getYear(), LocalDate.now().getMonthValue(),
					LocalDate.now().getDayOfMonth(), 0, 0, 0);

			if (tickerType != null)
				predicates.add(cb.equal(root.get("tickType"), tickerType));
			if (symbol != null)
				predicates.add(cb.equal(root.get("symbol"), symbol));
			if (vonHeute)
				predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), heute));

			cq.orderBy(cb.desc(root.get("timestamp")));
			cq.where(predicates.toArray(new Predicate[] {}));
			TypedQuery<MarketDataEntity> tq = session.createQuery(cq);
			List<MarketDataEntity> allitems = tq.getResultList();
			return new ArrayList<MarketDataEntity>(allitems);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			session.close();
		}
	}
}