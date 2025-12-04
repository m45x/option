package de.bergamotti.aktien.ibapi.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import de.bergamotti.aktien.ibapi.entity.AktieEntity;
import de.bergamotti.aktien.ibapi.entity.ContractEntity;
import de.bergamotti.aktien.ibapi.entity.ExecEntity;
import de.bergamotti.aktien.ibapi.entity.MarketDataEntity;
import de.bergamotti.aktien.ibapi.entity.ParameterEntity;
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

	public static void save(MarketDataEntity data) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			session.persist(data);
			session.getTransaction().commit();
		} finally {
			session.close();
		}
	}

	public static void save(ExecEntity data) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			session.persist(data);
			session.getTransaction().commit();
		} finally {
			session.close();
		}
	}

	public static void tickerGestartet(ContractEntity contract) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			contract = session.find(ContractEntity.class, contract.getId());
			contract.setTickerLastStartet(LocalDateTime.now());
			session.merge(contract);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static AktieEntity neueAktie(ContractEntity contract, BigDecimal anzahl, Integer orderId,
			BigDecimal kauflimit) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			AktieEntity aktie = new AktieEntity();
			aktie.setSymbol(contract.getSymbol());
			aktie.setCurrency(contract.getCurrency());
			aktie.setExchange(contract.getExchange());
			aktie.setAnzahlAktien(anzahl);
			aktie.setKaufdatum(LocalDateTime.now());
			aktie.setKaufOrderId(orderId);
			aktie.setKauflimit(kauflimit);
			aktie.setKaufError(Boolean.FALSE);
			return session.merge(aktie);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static void addGebuehrZuAktie(AktieEntity aktie, BigDecimal gebuehr) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			aktie = session.find(AktieEntity.class, aktie.getId());
			BigDecimal tmpgebuehr = aktie.getGebuehren();
			if (tmpgebuehr == null)
				tmpgebuehr = BigDecimal.ZERO;
			tmpgebuehr = tmpgebuehr.add(gebuehr);
			aktie.setGebuehren(tmpgebuehr);
			session.merge(aktie);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static ContractEntity updateLetzterBriefKurs(ContractEntity contract, BigDecimal briefKurs) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			ContractEntity e = session.find(ContractEntity.class, contract.getId());
			e.setLetzterBriefKurs(briefKurs);
			return session.merge(e);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static ContractEntity updateLetzterGeldKurs(ContractEntity contract, BigDecimal geldKurs) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			ContractEntity e = session.find(ContractEntity.class, contract.getId());
			e.setLetzterGeldKurs(geldKurs);
			return session.merge(e);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static void setAktieKaufError(AktieEntity aktie) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			aktie = session.find(AktieEntity.class, aktie.getId());
			aktie.setKaufError(Boolean.TRUE);
			session.merge(aktie);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static void setzeGewinnOderVerlustBeiAktie(AktieEntity aktie, BigDecimal gewinnOderVerlust) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			aktie = session.find(AktieEntity.class, aktie.getId());
			// Round to 2 decimal places and ensure value fits into DECIMAL(38,2)
			if (gewinnOderVerlust != null) {
				BigDecimal limit = new BigDecimal("1E36"); // absolute value must be less than 10^36 for PRECISION 38,
															// SCALE 2
				if (gewinnOderVerlust.abs().compareTo(limit) >= 0) {
					throw new IllegalArgumentException(
							"gewinnOderVerlust zu groß für DECIMAL(38,2): " + gewinnOderVerlust);
				}
				aktie.setGewinnOderVerlust(gewinnOderVerlust);
			} else {
				aktie.setGewinnOderVerlust(null);
			}
			session.merge(aktie);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static AktieEntity kaufAusgefuehrt(Long aktieId, BigDecimal kaufspreis, BigDecimal stoploss) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			AktieEntity aktie = session.find(AktieEntity.class, aktieId);
			if (aktie == null) {
				System.out.println("speichern fehlgeschlagen");
				return null;
			}
			aktie.setKaufpreis(kaufspreis);
			aktie.setStoplosspreis(stoploss);
			return session.merge(aktie);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static AktieEntity verkaufAusgefuehrt(Long aktieId, BigDecimal verkaufspreis) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			AktieEntity aktie = session.find(AktieEntity.class, aktieId);
			if (aktie == null) {
				System.out.println("speichern fehlgeschlagen");
				return null;
			}
			if (aktie.getVerkaufdatum() == null)
				aktie.setVerkaufdatum(LocalDateTime.now());
			aktie.setVerkkaufpreis(verkaufspreis);
			return session.merge(aktie);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static AktieEntity setAktieZumVerkaufAnmelden(Long idAktie, Integer orderId) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			AktieEntity aktie = session.find(AktieEntity.class, idAktie);
			if (aktie == null) {
				System.out.println("speichern fehlgeschlagen");
				return null;
			}
			aktie.setVerkaufOrderId(orderId);
			return session.merge(aktie);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static AktieEntity setAktieVerkauft(Long idAktie, BigDecimal verkaufspreis) {
		Session session = sessionFactory.openSession();
		try {
			session.beginTransaction();
			AktieEntity aktie = session.find(AktieEntity.class, idAktie);
			if (aktie == null) {
				System.out.println("speichern fehlgeschlagen");
				return null;
			}
			aktie.setVerkkaufpreis(verkaufspreis);
			aktie.setVerkaufdatum(LocalDateTime.now());
			// Calculate GewinnOderVerlust, round to 2 decimals and ensure it fits
			// DECIMAL(38,2)
			if (verkaufspreis != null && aktie.getKaufpreis() != null) {
				BigDecimal diff = verkaufspreis.subtract(aktie.getKaufpreis());
				BigDecimal rounded = diff.setScale(2, RoundingMode.HALF_UP);
				BigDecimal limit = new BigDecimal("1E36");
				if (rounded.abs().compareTo(limit) >= 0) {
					throw new IllegalArgumentException(
							"berechneter Gewinn/Verlust zu groß für DECIMAL(38,2): " + rounded);
				}
				aktie.setGewinnOderVerlust(rounded);
			} else {
				aktie.setGewinnOderVerlust(null);
			}
			return session.merge(aktie);
		} finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	public static ArrayList<ParameterEntity> getParameter(String parameterName, Boolean nurAktive) {
		Session session = sessionFactory.openSession();
		try {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<ParameterEntity> cq = cb.createQuery(ParameterEntity.class);
			Root<ParameterEntity> root = cq.from(ParameterEntity.class);
			List<Predicate> predicates = new ArrayList<Predicate>();

			// if (emailExact != null && !emailExact.equals(""))
			// predicates.add(cb.like(root.get(ProfileEntity_.email), emailExact));
			if (nurAktive)
				predicates.add(cb.equal(root.get("aktiv"), Boolean.TRUE));

			if (parameterName != null)
				predicates.add(cb.equal(root.get("parameterName"), parameterName));

			cq.where(predicates.toArray(new Predicate[] {}));
			TypedQuery<ParameterEntity> tq = session.createQuery(cq);
			List<ParameterEntity> allitems = tq.getResultList();
			return new ArrayList<ParameterEntity>(allitems);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
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

	public static ArrayList<AktieEntity> getAktien(String symbol, Boolean nurNochNichtVerkaufte) {
		Session session = sessionFactory.openSession();
		try {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<AktieEntity> cq = cb.createQuery(AktieEntity.class);
			Root<AktieEntity> root = cq.from(AktieEntity.class);
			List<Predicate> predicates = new ArrayList<Predicate>();

			// if (emailExact != null && !emailExact.equals(""))
			// predicates.add(cb.like(root.get(ProfileEntity_.email), emailExact));
			// predicates.add(cb.equal(root.get("aktiv"), Boolean.TRUE));

			predicates.add(cb.isFalse(root.get("kaufError")));
			if (symbol != null)
				predicates.add(cb.equal(root.get("symbol"), symbol));
			if (nurNochNichtVerkaufte)
				predicates.add(cb.isNull(root.get("verkaufdatum")));

			cq.where(predicates.toArray(new Predicate[] {}));
			TypedQuery<AktieEntity> tq = session.createQuery(cq);
			List<AktieEntity> allitems = tq.getResultList();
			return new ArrayList<AktieEntity>(allitems);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			session.close();
		}
	}

	public static ArrayList<MarketDataEntity> getMarketDataDesc(String symbol, String tickerType, boolean vonHeute,
			LocalDateTime abWann) {
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
			if (abWann != null)
				predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), abWann));

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

	public static ArrayList<ExecEntity> getExecs(String execId) {
		Session session = sessionFactory.openSession();
		try {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<ExecEntity> cq = cb.createQuery(ExecEntity.class);
			Root<ExecEntity> root = cq.from(ExecEntity.class);
			List<Predicate> predicates = new ArrayList<Predicate>();

			if (execId != null)
				predicates.add(cb.equal(root.get("execId"), execId));

//			cq.orderBy(cb.desc(root.get("timestamp")));
			cq.where(predicates.toArray(new Predicate[] {}));
			TypedQuery<ExecEntity> tq = session.createQuery(cq);
			List<ExecEntity> allitems = tq.getResultList();
			return new ArrayList<ExecEntity>(allitems);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			session.close();
		}
	}

	public static ArrayList<AktieEntity> getKaufOrderId(Integer kauforderId) {
		Session session = sessionFactory.openSession();
		try {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<AktieEntity> cq = cb.createQuery(AktieEntity.class);
			Root<AktieEntity> root = cq.from(AktieEntity.class);
			List<Predicate> predicates = new ArrayList<Predicate>();

			if (kauforderId != null)
				predicates.add(cb.equal(root.get("kaufOrderId"), kauforderId));

//			cq.orderBy(cb.desc(root.get("timestamp")));
			cq.where(predicates.toArray(new Predicate[] {}));
			TypedQuery<AktieEntity> tq = session.createQuery(cq);
			List<AktieEntity> allitems = tq.getResultList();
			return new ArrayList<AktieEntity>(allitems);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			session.close();
		}
	}

	public static Integer getAnzahlAktienImPortfolio(String symbol) {
		Session session = sessionFactory.openSession();
		try {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<AktieEntity> cq = cb.createQuery(AktieEntity.class);
			Root<AktieEntity> root = cq.from(AktieEntity.class);
			List<Predicate> predicates = new ArrayList<Predicate>();

			if (symbol != null)
				predicates.add(cb.equal(root.get("symbol"), symbol));
			predicates.add(cb.equal(root.get("kaufError"), Boolean.FALSE));
			predicates.add(cb.isNull(root.get("verkaufdatum")));

			cq.where(predicates.toArray(new Predicate[] {}));
			TypedQuery<AktieEntity> tq = session.createQuery(cq);
			List<AktieEntity> allitems = tq.getResultList();
			return allitems.size();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			session.close();
		}
	}
}