package de.option.first.chat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import com.ib.client.TickAttrib;
import com.ib.client.TickAttribBidAsk;
import com.ib.client.TickAttribLast;
import com.ib.client.TickType;

public class IBReceiver implements EWrapper {
	private final EClientSocket client;
	private final EJavaSignal signal;
	private final Map<Integer, Watch> watches = new ConcurrentHashMap<>();
	private final CountDownLatch idLatch = new CountDownLatch(1);
	private volatile int nextOrderId;
	private int currentReqId = 1000;
	private double availableFunds = 0.0;
	private boolean accountUpdatesRequested = false;
	
	public IBReceiver(EJavaSignal signal) {
		this.signal = signal;
		this.client = new EClientSocket(this, signal);
	}

	public void connect(String host, int port, int clientId) throws InterruptedException {
		client.eConnect(host, port, clientId);
		new Thread(() -> {
			EReader reader = new EReader(client, signal);
			reader.start();
			while (client.isConnected()) {
				signal.waitForSignal();
				try {
					reader.processMsgs();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		
		// reqIds, um nextValidId zu erhalten
		client.reqIds(-1);
		Thread.sleep(500);
		/*if (!idLatch.await(5, TimeUnit.SECONDS)) {
			throw new RuntimeException("Timeout beim Empfang von nextValidId");
		}*/
	}

	public void waitForNextValidId() throws InterruptedException {
		idLatch.await();
	}

	public void addWatch(String symbol, String secType, String currency, String exchange, double threshold, BigDecimal qty) {
		Contract c = new Contract();
		c.symbol(symbol);
		c.secType(secType);
		c.currency(currency);
		c.exchange(exchange);
		int reqId = currentReqId++;
		watches.put(reqId, new Watch(reqId, c, threshold, qty));
	}

	public void startMarketData() {
		for (Watch w : watches.values()) {
			client.reqMktData(currentReqId , w.contract, "", false, false, null);
		}
		
		// Die Ergebnisse kommen bei tickprice an
	}
	
	public void requestAccountUpdates(String accountName) {
	    client.reqAccountUpdates(true, accountName);
	    accountUpdatesRequested = true;
	}

	// In updateAccountValue speichern
	@Override
	public void updateAccountValue(String key, String value, String currency, String accountName) {
	    if (key.equals("AvailableFunds") && currency.equals("USD")) {
	        try {
	            availableFunds = Double.parseDouble(value);
	            System.out.println("AvailableFunds: " + availableFunds);
	        } catch (NumberFormatException e) {
	            e.printStackTrace();
	        }
	    }
	}

	// Vor Kauf prüfen
	private boolean canBuy(double price, BigDecimal qty) {
	    return availableFunds >= qty.multiply(new BigDecimal(price)).doubleValue();
	}

	@Override
	public void nextValidId(int orderId) {
		nextOrderId = orderId;
		idLatch.countDown();
		System.out.println("Next orderId = " + orderId);
	}

	@Override
	public void tickPrice(int tickerId, int field, double price, TickAttrib attribs) {
		Watch w = watches.get(tickerId);
		
		if (price < w.threshold && !w.bought) {
		    if (canBuy(price, w.qty)) {
		        System.out.printf("Kauf auslösen: %s %d Stück%n", w.contract.symbol(), w.qty);
		        // Order platzieren...
		    } else {
		        System.out.printf("Nicht genug Geld zum Kauf von %s: Verfügbar=%.2f, Benötigt=%.2f%n",
		            w.contract.symbol(), availableFunds, w.qty.multiply(new BigDecimal(price)).doubleValue());
		    }
		}
		
		if (w != null && field == TickType.LAST.index() && !w.bought) {
			System.out.printf("%s price = %.4f, threshold = %.2f%n", w.contract.symbol(), price, w.threshold);
			if (price < w.threshold) {
				System.out.printf("Kauf auslösen: %s %d Stück%n", w.contract.symbol(), w.qty);
				Order o = new Order();
				o.action("BUY");
				o.orderType("MKT");
				o.totalQuantity(Decimal.get(w.qty));
				//client.placeOrder(nextOrderId++, w.contract, o);
				w.bought = true;
			}
		}
	}

	// --- viele EWrapper-Methoden müssen implementiert, aber können leer bleiben
	// ---

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
	public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining, double avgFillPrice,
			int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
		// TODO Auto-generated method stub

	}

	@Override
	public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
		// TODO Auto-generated method stub

	}

	@Override
	public void openOrderEnd() {
		// TODO Auto-generated method stub

	}


	@Override
	public void updatePortfolio(Contract contract, Decimal position, double marketPrice, double marketValue,
			double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
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
	public void execDetails(int reqId, Contract contract, Execution execution) {
		// TODO Auto-generated method stub

	}

	@Override
	public void execDetailsEnd(int reqId) {
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
	public void realtimeBar(int reqId, long time, double open, double high, double low, double close, Decimal volume,
			Decimal wap, int count) {
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
		// TODO Auto-generated method stub

	}

	@Override
	public void position(String account, Contract contract, Decimal pos, double avgCost) {
		// TODO Auto-generated method stub

	}

	@Override
	public void positionEnd() {
		// TODO Auto-generated method stub

	}

	@Override
	public void accountSummary(int reqId, String account, String tag, String value, String currency) {
		// TODO Auto-generated method stub

	}

	@Override
	public void accountSummaryEnd(int reqId) {
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
		// TODO Auto-generated method stub

	}

	@Override
	public void error(String str) {
		// TODO Auto-generated method stub

	}

	@Override
	public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectionClosed() {
		// TODO Auto-generated method stub

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
	public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {
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
	public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {
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
	public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {
		// TODO Auto-generated method stub

	}

	@Override
	public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL,
			double value) {
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
	public void tickByTickMidPoint(int reqId, long time, double midPoint) {
		// TODO Auto-generated method stub

	}

	@Override
	public void orderBound(long orderId, int apiClientId, int apiOrderId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void completedOrder(Contract contract, Order order, OrderState orderState) {
		// TODO Auto-generated method stub

	}

	@Override
	public void completedOrdersEnd() {
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

	// … restliche Methoden leer …
}
