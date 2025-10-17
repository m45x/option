package de.option.first;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;

public class Versuch2 {

//	https://ibkrcampus.com/campus/ibkr-api-page/twsapi-doc/

	public static String url1 = "http://localhost:4001/v1/api";
//	public static String url1 = "https://api.ibkr.com/v1/api";
	public static String url_orders = "/iserver/orders";
	public static String url_status = "/iserver/auth/status";
	public static String konto = "U14799105";

	public int currentOrderId;

	public void start() {

		IBReceiver ibReceiver = new IBReceiver();

		com.ib.client.EReaderSignal readerSignal = new EJavaSignal();
		EClientSocket clientSocket = new EClientSocket(ibReceiver, readerSignal);
		clientSocket.eConnect("127.0.0.1",4001, 1);
		try {

			// Wait for nextValidId
			for (int i = 0; i < 10; i++) {

				System.out.println("Round " + i);

				Thread.sleep(1000);
			}


			// From here you can add the logic of your application

			System.out.println("Starte Request");

			clientSocket.reqAccountSummary(1, konto,
					"AccountType,NetLiquidation,TotalCashValue,SettledCash,AccruedCash,BuyingPower,EquityWithLoanValue,PreviousEquityWithLoanValue,GrossPositionValue,ReqTEquity,ReqTMargin,SMA,InitMarginReq,MaintMarginReq,AvailableFunds,ExcessLiquidity,Cushion,FullInitMarginReq,FullMaintMarginReq,FullAvailableFunds,FullExcessLiquidity,LookAheadNextChange,LookAheadInitMarginReq ,LookAheadMaintMarginReq,LookAheadAvailableFunds,LookAheadExcessLiquidity,HighestSeverity,DayTradesRemaining,Leverage");
			System.out.println("Warte for Signal...");
			readerSignal.waitForSignal();
			System.out.println("Fertig für Contract");

			EReader reader = new EReader(clientSocket, readerSignal);

			Contract contract = new Contract();
			contract.symbol("EURUSD");
			reader.processMsgs();

			System.out.println("ok Status: " + reader.getState());

			System.out.println("ok Name: " + reader.getName());

			clientSocket.reqContractDetails(1, contract);
			System.out.println("conID: " + contract.conid());

			clientSocket.reqAccountSummary(contract.conid(), "", "");
			reader.processMsgs();
			System.out.println("ok Status: " + reader.getState());
		} catch (Exception ex) {
			ex.printStackTrace();
			System.err.println(ex);
			System.out.println("Fehler");
		} finally {
			clientSocket.eDisconnect();
			System.out.println("Disconnect");
			System.exit(0);
		}
	}
}
