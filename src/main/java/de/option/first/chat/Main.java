package de.option.first.chat;

import java.math.BigDecimal;

import com.ib.client.EJavaSignal;

public class Main {


	public static void main(String[] args) throws Exception {
		EJavaSignal signal = new EJavaSignal();
		IBReceiver ib = new IBReceiver(signal);
		ib.connect("127.0.0.1", 4001, 1);
		ib.requestAccountUpdates("U14799105");
		
		// Warte auf Verbindung und nächste available orderId
		ib.waitForNextValidId();

		// Setze Schwellenwerte
		ib.addWatch("VOW3", "STK", "EUR", "FRA", 89.0, new BigDecimal(100.0));
		ib.addWatch("DBK", "STK", "EUR", "FRA", 25.0, new BigDecimal(100));
		ib.addWatch("NCLH", "STK", "USD", "SMART", 20.0, new BigDecimal(100));

		ib.startMarketData();

		// Laufend prüfen bis beendet (Strg+C)
		Thread.sleep(Long.MAX_VALUE);
	}
}
