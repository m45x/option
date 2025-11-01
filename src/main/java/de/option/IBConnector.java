package de.option;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ib.client.Bar;
import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDescription;
import com.ib.client.ContractDetails;
import com.ib.client.Decimal;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.DepthMktDataDescription;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.FamilyCode;
import com.ib.client.HistogramEntry;
import com.ib.client.HistoricalSession;
import com.ib.client.HistoricalTick;
import com.ib.client.HistoricalTickBidAsk;
import com.ib.client.HistoricalTickLast;
import com.ib.client.NewsProvider;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.PriceIncrement;
import com.ib.client.SoftDollarTier;
import com.ib.client.TagValue;
import com.ib.client.TickAttrib;
import com.ib.client.TickAttribBidAsk;
import com.ib.client.TickAttribLast;
import com.ib.client.TickType;

import de.option.dto.AccountDto;
import de.option.entity.AktieEntity;
import de.option.entity.ContractEntity;
import de.option.entity.ExecEntity;
import de.option.entity.MarketDataEntity;
import de.option.util.HibernateUtil;

/**
 * 
 * Unter connect() wird die Verbindung aufgebaut.
 * 
 * Bei nextValidId() wird der Ticker für einige Werte eimalig gestartet.
 * 
 * Bei startScheduler wird dann der Thread gestartet und alle 20 Sekunden wird
 * geschaut, ob weitere Marktdaten beobachtet werden sollen.
 * 
 * Wenn Beobachtungen getriggert wird, beginnt das System jedesmal auf kaufen
 * oder verkaufen zu prüfen.
 * 
 * finviz.com
 * 
 */
public class IBConnector implements EWrapper {
	private final EClientSocket clientSocket;
	private final EJavaSignal signal;
	private EReader reader;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	// replaced Integer with AtomicInteger to ensure thread-safe unique order IDs
	private final AtomicInteger globalOrderId = new AtomicInteger(0);
	private AccountDto accountDto = null;

	private Boolean isSimulation = false;

	private HashMap<Integer, String> hmMarketData = new HashMap<>();
	private Boolean accountWirdAktualisiert = false;
	private DecimalFormat formatter = new DecimalFormat("#,##0.00");

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IBConnector.class);

	public IBConnector() {
		signal = new EJavaSignal();
		clientSocket = new EClientSocket(this, signal);
	}

	public static void main(String[] args) {
		new IBConnector().connect();
	}

	public void connect() {
		String ip = "127.0.0.1";
		int port = 4001; // IB Gateway
		int clientId = 1;

		this.clientSocket.eConnect(ip, port, clientId);
//		this.clientSocket.reqMarketDataType(1);
		log.info("Connecting to IB Gateway...");

//		this.clientSocket.reqOpenOrders();

		// Löscht alle offenen Orders (global)
		// this.clientSocket.reqGlobalCancel();

		if (clientSocket.isConnected()) {
			log.info("{} Connected!", LocalDateTime.now());

			reader = new EReader(clientSocket, signal);
			reader.start();

			new Thread(() -> {
				while (clientSocket.isConnected()) {
					signal.waitForSignal();
					try {
						reader.processMsgs();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		} else {
			log.info("Connection failed!");
		}
	}

	@Override
	public void nextValidId(int orderId) {
		// set the starting order id provided by the TWS/Gateway
		this.globalOrderId.set(orderId);

		startScheduler();

		// Account initial aktualisieren
		accountAktualisieren();
	}

	// thread-safe method to get a unique order id for each placed order
	private int getNextOrderId() {
		int current = this.globalOrderId.getAndIncrement();
		if (current == 0) {
			// If nextValidId hasn't been called yet, warn the user — still return a value
			log.warn(
					"getNextOrderId() used before nextValidId was received; using id=0 and incrementing. This may cause errors.");
		}
		return current;
	}

	private void kaufen(boolean isSimulation, BigDecimal anzahl, ContractEntity contract, BigDecimal limitPrice) {

		//
		Order order = new Order();
		order.action("BUY");
		order.totalQuantity(Decimal.get(anzahl));
		order.orderType("LMT");
//		order.orderType("MKT");
		order.lmtPrice(limitPrice.doubleValue());
		order.tif("DAY");
		
		// Damit IBIS bevorzugt wird, sonst landet er wieder im Darkpool
		order.smartComboRoutingParams(List.of(new TagValue("PreferredExchanges", contract.getPrimaryExch())));
		
		// get a unique order id
		int orderIdToUse = getNextOrderId();

		//
		if (!isSimulation) {
			this.clientSocket.placeOrder(orderIdToUse, createContract(contract), order);
			// Danach sollte die Methode "openOrder" automatisch aufgerufen werden.
		}

		// Ohne preis in Aktien reinschreiben
		AktieEntity aktie = HibernateUtil.neueAktie(contract, anzahl, orderIdToUse, limitPrice);

		// Bei der Simulation wird sofort der Preis gesetzt
		// beim echten kauf erst wenn es wirklich gekauft wurde
		if (this.isSimulation)
			HibernateUtil.kaufAusgefuehrt(aktie.getId(), limitPrice, this.calcStopLoss(limitPrice));
	}

	private void verkaufen(AktieEntity aktie, ContractEntity contractEntity, BigDecimal limitPrice) {

		Contract contract = createContract(contractEntity);
		//
		Order order = new Order();
		order.action("SELL");
		order.totalQuantity(Decimal.get(aktie.getAnzahlAktien()));
		order.orderType("LMT");
//		order.orderType("MKT");
		order.lmtPrice(limitPrice.doubleValue());
		order.tif("DAY");

		if (!isSimulation) {

			//
			int orderIdToUse = getNextOrderId();
			this.clientSocket.placeOrder(orderIdToUse, contract, order);

			//
			aktie = HibernateUtil.setAktieZumVerkaufAnmelden(aktie.getId(), orderIdToUse);
			// Danach sollte die Methode "openOrder" automatisch aufgerufen werden.
		}

		// Bei der Simulation wird hier bereits der Preis aktualisiert, sonst
		// wird das erst bei execOrder gemacht
		if (isSimulation) {
			/*
			 * ArrayList<AktienEntity> arr = HibernateUtil.getAktien(contract.symbol(),
			 * true); AktienEntity aktie = null; for (AktienEntity e : arr) if
			 * (e.getVerkkaufpreis() == null) aktie = e;
			 * 
			 * if (aktie != null) {
			 */
			aktie = HibernateUtil.setAktieVerkauft(aktie.getId(), limitPrice);
			
			// Eigentlich müsste man hier auch die commisstion dinger simulieren
			// }
		}

	}

	public void pruefeKaufEntscheidung(ContractEntity contractEntity) {

		if (contractEntity == null) {
			log.info("Kein ContractEntity!");
			return;
		}

		ArrayList<AktieEntity> arrAktien = HibernateUtil.getAktien(contractEntity.getSymbol(), true);
		
		if (contractEntity.getKaufbar() == false) {
//			log.info("{} (pruefeKaufEntscheidung) Nicht kaufbar.", contractEntity.getSymbol());
			return;
		}
		
		// Ist von der Uhrzeit her ein Kauf ok?
		if (!darfWegenUhrzeitGekauftWerden(contractEntity)) {
//			log.info("{} (pruefeKaufEntscheidung) Kein Kauf wegen Uhrzeit.", contractEntity.getSymbol());
			return;
		}

		// Kein Kauf, solange ich noch Aktien habe.
		if (arrAktien == null || arrAktien.size() != 0) {
//			log.info("{} (pruefeKaufEntscheidung) Kein Kauf solange Aktien noch im Portfolio.",
//					contractEntity.getSymbol());
			return;
		}

		// Keine Entscheidung bevor Accountdaten da sind
		if (this.accountDto == null) {
			log.info("{} (pruefeKaufEntscheidung) Noch keine Accountdaten da.", contractEntity.getSymbol());
			return;
		}

		// Kaufentscheidung ja/nein
		if (!this.isAktieAmSteigen(contractEntity.getSymbol())) {
//			log.info("{} (pruefeKaufEntscheidung) Aktie ist nicht am steigen.", contractEntity.getSymbol());
			return;
		}
//		log.info("{} (pruefeKaufEntscheidung) Aktie steigt!", contractEntity.getSymbol());

		// Limitprice errechnen
		BigDecimal limitPrice = this.getLetztenKaufPreis(contractEntity.getSymbol());
		if (limitPrice == null) {
			log.info("{} (pruefeKaufEntscheidung) LimitPreis war nicht zu ermitteln!", contractEntity.getSymbol());
			return;
		}

		// Wenn Bärenmarkt dann nicht
		if (this.isBaerenmarkt(contractEntity, limitPrice)) {
//			log.info("{} (pruefeKaufEntscheidung) Bärenmarkt. Kein Kauf", contractEntity.getSymbol());
			return;
		}

		var anzahlAktien = new BigDecimal(100);
		BigDecimal gesamtwert = anzahlAktien.multiply(limitPrice);

		// Wenn die Kaufkraft kleiner ist als der Kaufpreis, dann nicht
		if (gesamtwert.compareTo(this.accountDto.getBuyingPower()) > 0) {
//			log.info("{} (pruefeKaufEntscheidung) Kaufkraft reicht zum Kaufen der Aktie nicht aus.",
//					contractEntity.getSymbol());
			return;
		}

		log.info("{} (pruefeKaufEntscheidung) KAUFEN!", contractEntity.getSymbol());
		this.kaufen(this.isSimulation, anzahlAktien, contractEntity, limitPrice);
	}

	/*private boolean mussWegenUhrzeitVerkauftWerden(ContractEntity contractEntity) {

		if (contractEntity.getPrimaryExch().equals("IBIS")) {
			if (LocalDateTime.now().getHour() >= 15 && LocalDateTime.now().getMinute() >= 30)
				return true;
		} else
			return false;

		return false;
	}*/

	private boolean darfWegenUhrzeitGekauftWerden(ContractEntity contractEntity) {

		if (contractEntity == null || contractEntity.getPrimaryExch() == null)
			return false;

		if (contractEntity.getPrimaryExch().equals("IBIS")) {
			if (LocalDateTime.now().getHour() >= 9 && LocalDateTime.now().getHour() < 17)
				return true;
		} else
			return true;

		return false;
	}

	private boolean isBaerenmarkt(ContractEntity contractEntity, BigDecimal aktuellerPreis) {

		BigDecimal schluss = this.getSchlusskurs(contractEntity);
		if (schluss == null)
			return false;

//		log.info("%s Schlusskurs %.2f", LocalDateTime.now(), contractEntity.getSymbol(), schluss);

//		log.info("{} Vergleiche aktuell {} mit schluss {}", contractEntity.getSymbol(),
//				formatter.format(aktuellerPreis), formatter.format(schluss.multiply(new BigDecimal(0.99))));
		if (aktuellerPreis.compareTo(schluss.multiply(new BigDecimal(0.99))) < 0)
			return true;
		else
			return false;
	}

	private BigDecimal getSchlusskurs(ContractEntity contractEntity) {

		ArrayList<MarketDataEntity> arr = HibernateUtil.getMarketDataDesc(contractEntity.getSymbol(), "close", true);
		if (arr == null || arr.size() == 0)
			return null;

		BigDecimal schlusskurs = arr.get(0).getLast();

		return schlusskurs;

	}

	/*
	 * private BigDecimal getEroeffnungskurs(ContractEntity contractEntity) {
	 * 
	 * ArrayList<MarketDataEntity> arr =
	 * HibernateUtil.getMarketDataDesc(contractEntity.getSymbol(), "close", true);
	 * if (arr == null || arr.size() == 0) return null;
	 * 
	 * BigDecimal openkurs = arr.get(0).getLast();
	 * 
	 * log.info("{} Eröffnungskurs {}", contractEntity.getSymbol(),
	 * formatter.format(openkurs)); return openkurs;
	 * 
	 * }
	 */

	public boolean isAktieAmSteigen(String symbol) {
//		return this.isAktieAmSteigenLetzten3(symbol);
		return this.isAktieAmSteigenUeber10KurseHinweg(symbol);
	}

	public boolean isAktieAmFallen(String symbol, BigDecimal aktuellerPreis) {
		return this.isAktieAmFallenUeber10KurseHinweg(symbol, aktuellerPreis);
	}

	public boolean isAktieAmSteigenLetzten3(String symbol) {

		// Nimm die letzten 3 Briefkurse. Wenn dreimal hintereinander die Preise am
		// steigen
		// sind, ist die Aktie am Steigen

		ArrayList<MarketDataEntity> arr = HibernateUtil.getMarketDataDesc(symbol, "lastPrice", true);

		if (arr == null || arr.size() <= 3)
			return false;

		BigDecimal preis3 = arr.get(0).getLast();
		BigDecimal preis2 = arr.get(1).getLast();
		BigDecimal preis1 = arr.get(2).getLast();
		// log.info("{} Prüfe {} zu {} zu {}", symbol, formatter.format(preis1),
		// formatter.format(preis2),
		// formatter.format(preis3));

		if (preis1.compareTo(preis2) == -1 && preis2.compareTo(preis3) == -1)
			return true;

		return false;
	}

	public boolean isAktieAmSteigenUeber10KurseHinweg(String symbol) {

		// Nimm die letzten n+2 Briefkurse. Wenn der letzte Preis höher als der
		// vorletzte
		// und höher als der durchschnitt der n letzten davor, dann steigt die aktie

		ArrayList<MarketDataEntity> arr = HibernateUtil.getMarketDataDesc(symbol, "lastPrice", true);

		int anzahlWerte = 10;
		if (arr == null || arr.size() <= anzahlWerte + 2)
			return false;

		BigDecimal preissummiert = BigDecimal.ZERO;
		for (int i = 2; i < anzahlWerte + 2; i++) {
			preissummiert = preissummiert.add(arr.get(i).getLast());
		}

		BigDecimal durchschnitt = preissummiert.divide(new BigDecimal(anzahlWerte));
		BigDecimal vorletzterPreis = arr.get(1).getLast();
		BigDecimal letzterPreis = arr.get(0).getLast();

//		log.info("{} Prüfe Durchschnittspreis {} und letzten Preis {} zu jetzigen Preis von {}", symbol,
//				formatter.format(durchschnitt), formatter.format(vorletzterPreis), formatter.format(letzterPreis));

		if (vorletzterPreis.compareTo(letzterPreis) < 0 && durchschnitt.compareTo(letzterPreis) < 0)
			return true;

		return false;
	}

	public boolean isAktieAmFallenUeber10KurseHinweg(String symbol, BigDecimal aktuellerPreis) {

		// Nimm die letzten 12 Briefkurse. Wenn der letzte Preis höher als der vorletzte
		// und höher als der durchschnitt der 10 letzten davor, dann steigt die aktie

		ArrayList<MarketDataEntity> arr = HibernateUtil.getMarketDataDesc(symbol, TickType.BID.field(), true);

		if (arr == null || arr.size() <= 11)
			return false;

		BigDecimal preissummiert = BigDecimal.ZERO;
		Integer anzahlWerte = 0;
		for (int i = 1; i < 11; i++) {
			preissummiert = preissummiert.add(arr.get(i).getGeldkurs());
			anzahlWerte++;
		}

		BigDecimal durchschnitt = preissummiert.divide(new BigDecimal(anzahlWerte),
				new MathContext(2, RoundingMode.HALF_EVEN));
//		BigDecimal vorletzterPreis = arr.get(1).getLast();
		BigDecimal letzterPreis = arr.get(0).getGeldkurs();

		log.info("{} Prüfe Durchschnittspreis {} und letzten Preis {} zu jetzigen Preis von {}", symbol,
				formatter.format(durchschnitt), formatter.format(letzterPreis), formatter.format(aktuellerPreis));

		// Durchschnitt höher als letzter Preis
		// und letzter Preis kleiner oder gleich als vorletzter Preis
		// dann sinkt die Aktie
		if (durchschnitt.compareTo(letzterPreis) > 0 && letzterPreis.compareTo(aktuellerPreis) >= 0)
			return true;

		return false;
	}

	private BigDecimal getLetztenKaufPreis(String symbol) {
		ArrayList<MarketDataEntity> arr = HibernateUtil.getMarketDataDesc(symbol, "askPrice", true);

		if (arr == null || arr.size() == 0)
			return null;
		else
			return arr.get(0).getBriefkurs();

	}

	public void pruefeVerkaufEntscheidung(BigDecimal preis, ContractEntity contractEntity) {
//		System.out.printf("(verkaufentscheidung) {} %.2f\n" , contractEntity.getSymbol() , preis);

		if (preis.equals(BigDecimal.ZERO)) {
			log.info("{} Preis ist 0 - Abbruch", contractEntity.getSymbol());
			return;
		}

		ArrayList<AktieEntity> arrAktien = HibernateUtil.getAktien(contractEntity.getSymbol(), true);
		for (AktieEntity aktie : arrAktien) {
			
			// wenn geldkurs grösser kaufpreis dann verkaufen
			if (aktie.getKaufpreis() == null) {
//				log.info("{} Kaufpreis ist null - Abbruch", aktie.getSymbol());
				continue;
			}

			if (aktie.getVerkaufOrderId() != null) {
//				log.info("{} Verkaufsorder bereits platziert", aktie.getSymbol());
				continue;
			}

			BigDecimal verkaufspreis = aktie.getKaufpreis();

			// Hier kommen die Transaktionskosten noch dazu
			if (aktie.getGebuehren()!=null) verkaufspreis = verkaufspreis.add(aktie.getGebuehren().divide(aktie.getAnzahlAktien(), 4, RoundingMode.HALF_EVEN));
			
			// Preis grösser als Verkaufspreis
			if (preis.compareTo(verkaufspreis) == 1) {

				log.info("{} GEWINNZONE ERREICHT", aktie.getSymbol());

				// Ist von der Uhrzeit her ein Verkauf ein MUSS?
				/*if (this.mussWegenUhrzeitVerkauftWerden(contractEntity)) {
					log.info("{} Verkauf zum jetzigen Preis von {} wegen der Uhrzeit.", contractEntity.getSymbol(),
							formatter.format(preis));

					return;
				}*/

				if (this.isAktieAmFallen(contractEntity.getSymbol(), preis)) {
					log.info("{} VERKAUFSENTSCHEIDUNG GEWINN!! Kurs {}", aktie.getSymbol(), formatter.format(preis));
					this.verkaufen(aktie, contractEntity, preis);
					return;
				} else {
					log.info("{} Kurs steigt noch. Kaufpreis {}. Aktuell bei {}", aktie.getSymbol(),
							formatter.format(aktie.getKaufpreis()), formatter.format(preis));
					return;
				}
			}

			// oder StopLoss bei über n % Verlust
//			if (aktie.getStoplosspreis() == null) {
//				log.info("{} (pruefeVerkaufentscheidung) STOPLOSS Preis war null, daher neu berechnet: {}", aktie.getSymbol(),
//						formatter.format(this.calcStopLoss(aktie.getKaufpreis())));
//			}

			if (aktie.getStoplosspreis() != null && preis.compareTo(aktie.getStoplosspreis()) == -1) {
				log.info("{} (pruefeVerkaufentscheidung) VERKAUF STOPLOSS!! Kurs {}", aktie.getSymbol(),
						formatter.format(preis));
				this.verkaufen(aktie, contractEntity, preis);
				return;
			}

			//
			if (aktie.getStoplosspreis() != null) {
				log.info("{} (pruefeVerkaufentscheidung)  {} <- {} - {} - {} -> {}", contractEntity.getSymbol(),
						formatter.format(aktie.getStoplosspreis()),
						formatter.format(preis.subtract(aktie.getStoplosspreis())), formatter.format(preis),
						formatter.format(verkaufspreis.subtract(preis)), formatter.format(verkaufspreis));
			} else {
				log.info("{} (pruefeVerkaufentscheidung) {} - {} -> {}", contractEntity.getSymbol(),
						formatter.format(preis), formatter.format(verkaufspreis.subtract(preis)),
						formatter.format(verkaufspreis));
			}
		}
	}

	private BigDecimal calcStopLoss(BigDecimal kaufPreis) {
		// oder StopLoss bei über n % Verlust
		/*BigDecimal percentLimit = new BigDecimal(0.995);
		if (kaufPreis.compareTo(new BigDecimal(30)) == 1)
			percentLimit = new BigDecimal(0.99);
		if (kaufPreis.compareTo(new BigDecimal(80)) == 1)
			percentLimit = new BigDecimal(0.998);
		if (kaufPreis.compareTo(new BigDecimal(200)) == 1)
			percentLimit = new BigDecimal(0.999);

		BigDecimal stoplossPercent = kaufPreis.multiply(percentLimit);

		// Oder absoluter StopLoss von 20 Cent
		BigDecimal stoplossAbsolut = kaufPreis.subtract(new BigDecimal(0.20));
		BigDecimal stoploss = (stoplossPercent.compareTo(stoplossAbsolut) == -1) ? stoplossAbsolut : stoplossPercent;
		return stoploss;
		*/
//		return new BigDecimal(0.01); // stoploss;
		return null; // stoploss;
	}

	private void startScheduler() {
		this.scheduler.scheduleAtFixedRate(() -> {

//			System.out.printf("entries: ");
//			for ( Entry<Integer , String> e : this.hmMarketData.entrySet()) {
//				System.out.printf(e.getKey() + " " );
//			}
//			System.out.printf("\n");

			ArrayList<ContractEntity> arrContracts = HibernateUtil.getContracts(false, null);
			for (ContractEntity contract : arrContracts) {

				if (!contract.getAktiv()) {

					if (this.hmMarketData.containsKey(contract.getTickerId())) {

						log.info("{} Marktüberwachung wird gestoppt.", contract.getSymbol());
						// Rausnehmen
						this.hmMarketData.remove(contract.getTickerId());

						// Abfrage stoppen
						this.clientSocket.cancelMktData(contract.getTickerId());
					}

				} else {

					// Schauen, ob bereits abgefragt wird.
					if (this.hmMarketData.containsKey(contract.getTickerId()) == true)
						continue;

					// Wenn nicht, dann hinzufügen
					this.hmMarketData.put(contract.getTickerId(), null);
					log.info("{} Marktüberwachung wird gestartet.", contract.getSymbol());

					// Und Abfrage starten
//					if (contract.getSecType().equals(SecType.STK.name()))
					this.clientSocket.reqMktData(contract.getTickerId(), this.createContract(contract), "", false,
							false, null);
//					else if (contract.getSecType().equals(SecType.CASH.name()))
//						this.clientSocket.reqMktData(contract.getTickerId(), this.createCashContract(contract), "",
//								false, false, null);

				}
			}

		}, 0, 10, TimeUnit.SECONDS);
	}

	private Contract createContract(ContractEntity contract) {
		Contract c = new Contract();
		c.symbol(contract.getSymbol());
		c.secType(contract.getSecType());
		c.exchange(contract.getExchange());
		c.currency(contract.getCurrency());
		if (contract.getSecType().equals("STK") && contract.getPrimaryExch() != null)
			c.primaryExch(contract.getPrimaryExch());
		return c;
	}

	// Hier werden Preisdaten empfangen
	@Override
	public void tickPrice(int tickerId, int field, double price, TickAttrib attribs) {

		//
		if (price < 0)
			return;

		// Suche Contract nach tickerId
		ArrayList<ContractEntity> arrContract = HibernateUtil.getContracts(true, tickerId);
		if (arrContract == null || arrContract.size() != 1)
			return;
		ContractEntity contractEntity = arrContract.get(0);

		if (field == TickType.BID.index()) { // com.ib.client.TickType.LAST) {
			BigDecimal geldkurs = new BigDecimal(price);
//			System.out.printf("⏱ Letzter Verkaufpreis (BID) für " + contractEntity.getSymbol() + ": %.2f", geldkurs);
			MarketDataEntity data = new MarketDataEntity(contractEntity.getTickerId(), contractEntity.getSymbol(),
					TickType.BID.field(), null, geldkurs, null, LocalDateTime.now());
			HibernateUtil.save(data);

//			log.info("{} %s %s %.2f\n" , contractEntity.getSymbol() ,tickerId , field , price);

			pruefeVerkaufEntscheidung(geldkurs, contractEntity);
		} else if (field == TickType.ASK.index()) { // com.ib.client.TickType.LAST) {
			BigDecimal briefkurs = new BigDecimal(price);
//			System.out.printf("⏱ Letzter Kaufpreis (ASK) für " + contractEntity.getSymbol() + ": %.2f", briefkurs);
			MarketDataEntity data = new MarketDataEntity(contractEntity.getTickerId(), contractEntity.getSymbol(),
					TickType.ASK.field(), null, null, briefkurs, LocalDateTime.now());
			HibernateUtil.save(data);
		} else if (field == TickType.LAST.index()) { // com.ib.client.TickType.LAST) {
			BigDecimal preis = new BigDecimal(price);
//			System.out.printf("⏱ Letzter Preis (LAST) für " + contractEntity.getSymbol() + ": %.2f", preis);
			MarketDataEntity data = new MarketDataEntity(contractEntity.getTickerId(), contractEntity.getSymbol(),
					TickType.LAST.field(), preis, null, null, LocalDateTime.now());
			HibernateUtil.save(data);

			this.pruefeKaufEntscheidung(contractEntity);
		} else if (field == TickType.OPEN.index()) { // com.ib.client.TickType.LAST) {
			BigDecimal preis = new BigDecimal(price);
//			System.out.printf("⏱ Letzter Preis (OPEN) für " + contractEntity.getSymbol() + ": %.2f", preis);
			MarketDataEntity data = new MarketDataEntity(contractEntity.getTickerId(), contractEntity.getSymbol(),
					TickType.OPEN.field(), preis, null, null, LocalDateTime.now());
			HibernateUtil.save(data);

		} else if (field == TickType.CLOSE.index()) { // com.ib.client.TickType.LAST) {
			BigDecimal preis = new BigDecimal(price);
//			System.out.printf("⏱ Letzter Preis (CLOSE) für " + contractEntity.getSymbol() + ": %.2f", preis);
			MarketDataEntity data = new MarketDataEntity(contractEntity.getTickerId(), contractEntity.getSymbol(),
					TickType.CLOSE.field(), preis, null, null, LocalDateTime.now());
			HibernateUtil.save(data);

		} else {
//			System.out.printf("Feld " + field + " wird nicht abgefragt!");
		}

	}

	@Override
	public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
		log.info("openOrder aufgerufen!! OrderId: {} / {} / Status {} ", orderId, contract.symbol(),
				orderState.completedStatus());
	}

	private void accountAktualisieren() {

		// Wenn schon, dann brauch nicht.
		if (this.accountWirdAktualisiert)
			return;

		// Schonmal Account summe aufrufen - siehe "accountSummary"
		this.clientSocket.reqAccountSummary(9001, "All", "NetLiquidation,TotalCashValue,BuyingPower,Currency");

		this.accountWirdAktualisiert = true;
	}

	@Override
	public void execDetails(int reqId, Contract contract, Execution execution) {
		log.info("execDetails! ORDER ausgeführt! {} / {} / OrderID: ", reqId, contract.symbol(), execution.orderId());

		// Schonmal account aktualisierung anstoßen
		accountAktualisieren();

		AktieEntity aktie = null;
		String side = execution.side();
		if ("BOT".equals(side)) {
			log.info("📈 Kauf ausgeführt! Kaufpreis " + execution.price());

			// suche Aktie ohne Preis
			ArrayList<AktieEntity> arr = HibernateUtil.getAktien(contract.symbol(), true);
			for (AktieEntity e : arr)
				if (e.getKaufpreis() == null)
					aktie = e;

			if (aktie != null) {
				BigDecimal stoploss = this.calcStopLoss(new BigDecimal(execution.price()));
				HibernateUtil.kaufAusgefuehrt(aktie.getId(), new BigDecimal(execution.price()), stoploss);
			} else {
				log.error("Kaufaktie nicht gefunden für Symbol {}", contract.symbol());
			}

		} else if ("SLD".equals(side)) {
			log.info("📉 Verkauf ausgeführt!");

			// Suche danach und dann updaten
			ArrayList<AktieEntity> arr = HibernateUtil.getAktien(contract.symbol(), true);
			for (AktieEntity e : arr)
				if (e.getVerkkaufpreis() == null)
					aktie = e;

			if (aktie != null) {
				HibernateUtil.verkaufAusgefuehrt(aktie.getId(), new BigDecimal(execution.price()));
			} else {
				log.error("Verkaufaktie nicht gefunden für Symbol {}", contract.symbol());
			}
		}

		if (aktie!=null) {
			ExecEntity execEntity = new ExecEntity();
			execEntity.setAktien(aktie);
			execEntity.setOrderId(execution.orderId());
			execEntity.setExecId(execution.execId());
			HibernateUtil.save(execEntity);
			
			log.info("=== Execution Details ===");
			log.info("Kauf/Verkauf: " + side);
			log.info("ReqId:        " + reqId);
			log.info("Symbol:       " + contract.symbol());
		    log.info("SecType:      " + contract.secType());
		    log.info("Exchange:     " + execution.exchange());  // <-- Hier siehst du z. B. IBIS, CHIXDE, TRQX, etc.
		    log.info("OrderId:      " + execution.orderId());
		    log.info("ExecId:       " + execution.execId());
		    log.info("Time:         " + execution.time());
		    log.info("Shares:       " + execution.shares());
		    log.info("Price:        " + execution.price());
		    log.info("Side:         " + execution.side());
		    log.info("CumQty:       " + execution.cumQty());
		    log.info("AvgPrice:     " + execution.avgPrice());
		    log.info("=========================");
		}
		
	}

	@Override
	public void accountSummary(int reqId, String account, String tag, String value, String currency) {
		log.info("AccountSummary {}: {} = {} {}", account, tag, value, currency);

		if (this.accountDto == null)
			this.accountDto = new AccountDto();

		this.accountDto.setAccount(account);
		if (tag.equalsIgnoreCase("buyingpower"))
			this.accountDto.setBuyingPower(new BigDecimal(value));
		if (tag.equalsIgnoreCase("netliquidation"))
			this.accountDto.setNetLiquidation(new BigDecimal(value));
		if (tag.equalsIgnoreCase("totalcashvalue"))
			this.accountDto.setTotalCashValue(new BigDecimal(value));
	}

	@Override
	public void accountSummaryEnd(int reqId) {
		this.accountWirdAktualisiert = false;
	}

	@Override
	public void tickByTickMidPoint(int reqId, long time, double midPoint) {
	}

	@Override
	public void orderBound(long orderId, int apiClientId, int apiOrderId) {
	}

	@Override
	public void completedOrder(Contract contract, Order order, OrderState orderState) {
		log.info("completedOrder aufgerufen!! OrderId: {} / {} / Status {} ", order.orderId(), contract.symbol(),
				orderState.completedStatus());
	}

	@Override
	public void completedOrdersEnd() {
	}

	@Override
	public void tickSize(int tickerId, int field, Decimal size) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickOptionComputation(int tickerId, int field, int tickAttrib, double impliedVol, double delta,
			double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
		// TODO Auto-generated method stub

	}

	@Override
	public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining, double avgFillPrice,
			int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updatePortfolio(Contract contract, Decimal position, double marketPrice, double marketValue,
			double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateMktDepth(int tickerId, int position, int operation, int side, double price, Decimal size) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price,
			Decimal size, boolean isSmartDepth) {
		// TODO Auto-generated method stub

	}

	@Override
	public void realtimeBar(int reqId, long time, double open, double high, double low, double close, Decimal volume,
			Decimal wap, int count) {
		// TODO Auto-generated method stub

	}

	@Override
	public void position(String account, Contract contract, Decimal pos, double avgCost) {
		// TODO Auto-generated method stub

	}

	@Override
	public void verifyMessageAPI(String apiData) {
		// TODO Auto-generated method stub

	}

	@Override
	public void verifyCompleted(boolean isSuccessful, String errorText) {
		// TODO Auto-generated method stub

	}

	@Override
	public void verifyAndAuthMessageAPI(String apiData, String xyzChallenge) {
		// TODO Auto-generated method stub

	}

	@Override
	public void verifyAndAuthCompleted(boolean isSuccessful, String errorText) {
		// TODO Auto-generated method stub

	}

	@Override
	public void displayGroupList(int reqId, String groups) {
		// TODO Auto-generated method stub

	}

	@Override
	public void displayGroupUpdated(int reqId, String contractInfo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void error(Exception e) {
		e.printStackTrace();
	}

	@Override
	public void error(String str) {
		// TODO Auto-generated method stub
		log.info("ERROR {}", str);
	}

	@Override
	public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		// TODO Auto-generated method stub

		log.info("ERROR {}", errorMsg);
	}

	@Override
	public void connectAck() {
		// TODO Auto-generated method stub

	}

	@Override
	public void positionMulti(int reqId, String account, String modelCode, Contract contract, Decimal pos,
			double avgCost) {
		// TODO Auto-generated method stub

	}

	@Override
	public void positionMultiEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value,
			String currency) {
		// TODO Auto-generated method stub

	}

	@Override
	public void accountUpdateMultiEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId,
			String tradingClass, String multiplier, Set<String> expirations, Set<Double> strikes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void securityDefinitionOptionalParameterEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {
		// TODO Auto-generated method stub

	}

	@Override
	public void familyCodes(FamilyCode[] familyCodes) {
		// TODO Auto-generated method stub

	}

	@Override
	public void symbolSamples(int reqId, ContractDescription[] contractDescriptions) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickNews(int tickerId, long timeStamp, String providerCode, String articleId, String headline,
			String extraData) {
		// TODO Auto-generated method stub

	}

	@Override
	public void smartComponents(int reqId, Map<Integer, Entry<String, Character>> theMap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newsProviders(NewsProvider[] newsProviders) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newsArticle(int requestId, int articleType, String articleText) {
		// TODO Auto-generated method stub

	}

	@Override
	public void historicalNews(int requestId, String time, String providerCode, String articleId, String headline) {
		// TODO Auto-generated method stub

	}

	@Override
	public void historicalNewsEnd(int requestId, boolean hasMore) {
		// TODO Auto-generated method stub

	}

	@Override
	public void headTimestamp(int reqId, String headTimestamp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void histogramData(int reqId, List<HistogramEntry> items) {
		// TODO Auto-generated method stub

	}

	@Override
	public void historicalDataUpdate(int reqId, Bar bar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rerouteMktDataReq(int reqId, int conId, String exchange) {
		// TODO Auto-generated method stub

	}

	@Override
	public void rerouteMktDepthReq(int reqId, int conId, String exchange) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL,
			double value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size,
			TickAttribLast tickAttribLast, String exchange, String specialConditions) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize,
			Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {
		// TODO Auto-generated method stub

	}

	@Override
	public void replaceFAEnd(int reqId, String text) {
		// TODO Auto-generated method stub

	}

	@Override
	public void wshMetaData(int reqId, String dataJson) {
		// TODO Auto-generated method stub

	}

	@Override
	public void wshEventData(int reqId, String dataJson) {
		// TODO Auto-generated method stub

	}

	@Override
	public void historicalSchedule(int reqId, String startDateTime, String endDateTime, String timeZone,
			List<HistoricalSession> sessions) {
		// TODO Auto-generated method stub

	}

	@Override
	public void userInfo(int reqId, String whiteBrandingId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickGeneric(int tickerId, int tickType, double value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickString(int tickerId, int tickType, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints,
			double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact,
			double dividendsToLastTradeDate) {
		// TODO Auto-generated method stub

	}

	@Override
	public void openOrderEnd() {
		// TODO Auto-generated method stub
		log.info("openOrderEnd aufgerufen!!");

	}

	@Override
	public void updateAccountValue(String key, String value, String currency, String accountName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateAccountTime(String timeStamp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void accountDownloadEnd(String accountName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void contractDetails(int reqId, ContractDetails contractDetails) {
		// TODO Auto-generated method stub

	}

	@Override
	public void bondContractDetails(int reqId, ContractDetails contractDetails) {
		// TODO Auto-generated method stub

	}

	@Override
	public void contractDetailsEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void execDetailsEnd(int reqId) {
		// TODO Auto-generated method stub
		log.info("execDetailsEnd aufgerufen!! reqId: {}", reqId);

	}

	@Override
	public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {
		// TODO Auto-generated method stub

	}

	@Override
	public void managedAccounts(String accountsList) {
		// TODO Auto-generated method stub

	}

	@Override
	public void receiveFA(int faDataType, String xml) {
		// TODO Auto-generated method stub

	}

	@Override
	public void historicalData(int reqId, Bar bar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scannerParameters(String xml) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark,
			String projection, String legsStr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void scannerDataEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void currentTime(long time) {
		// TODO Auto-generated method stub

	}

	@Override
	public void fundamentalData(int reqId, String data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickSnapshotEnd(int reqId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void marketDataType(int reqId, int marketDataType) {
		// TODO Auto-generated method stub

	}

	@Override
	public void commissionReport(CommissionReport commissionReport) {
		log.info("commissionReport");
		
		log.info("  ExecId: " + commissionReport.execId());
		log.info("  Commission: " + commissionReport.commission() + " " + commissionReport.currency());
		
		ArrayList<ExecEntity> arr = HibernateUtil.getExecs(commissionReport.execId());
		
		if (arr!=null && arr.size()==1) {
			BigDecimal gebuehr = new BigDecimal(commissionReport.commission());

			HibernateUtil.addGebuehrZuAktie(arr.get(0).getAktien() , gebuehr);
			
			// Gewinn oder Verlust ermitteln
//			HibernateUtil.ermittelGewinnOderVerlustBeiAktie(arr.get(0).getAktien());
			HibernateUtil.setzeGewinnOderVerlustBeiAktie(arr.get(0).getAktien() , new BigDecimal(commissionReport.realizedPNL()));	
		}

	}

	@Override
	public void positionEnd() {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectionClosed() {
		// TODO Auto-generated method stub

	}

	@Override
	public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {
		// TODO Auto-generated method stub

	}

	@Override
	public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {
		// TODO Auto-generated method stub

	}

	@Override
	public void historicalTicks(int reqId, List<HistoricalTick> ticks, boolean done) {
		// TODO Auto-generated method stub

	}

	@Override
	public void historicalTicksBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean done) {
		// TODO Auto-generated method stub

	}

	@Override
	public void historicalTicksLast(int reqId, List<HistoricalTickLast> ticks, boolean done) {
		// TODO Auto-generated method stub

	}

}