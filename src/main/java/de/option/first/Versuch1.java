package de.option.first;

import java.io.IOException;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReader;
import com.ib.client.TagValue;

public class Versuch1 {

//	https://ibkrcampus.com/campus/ibkr-api-page/twsapi-doc/
//  Die Sourcen sind in C:\TWS API\source\JavaClient
//	oder hier in eclipse im projekt javaclient

	public static String ip = "localhost";
	public static int port = 4001;

	public static String url1 = "http://" + ip + ":" + port + "/v1/api";
//	public static String url1 = "https://api.ibkr.com/v1/api";
	public static String url_orders = "/iserver/orders";
	public static String url_status = "/iserver/auth/status";
	public static String account = "U14799105";

	private EClientSocket clientSocket = null;
	private EJavaSignal signal = new EJavaSignal();
	private EReader reader;
	private IBReceiver ibReceiver;

	public void start() {
		System.out.println("VERSUCH1 start");
		ibReceiver = new IBReceiver();
		clientSocket = new EClientSocket(ibReceiver, signal);
		clientSocket.eConnect(ip, port, 1);
		System.out.println("Try to connect");
		if (clientSocket.isConnected()) {
			try {

				Thread.sleep(500);
				reader = new EReader(clientSocket, signal);
				reader.start();

				System.out.println("-----------------------");
				System.out.println("INTRO START");
				signal.waitForSignal();
				Thread.sleep(500);
				reader.processMsgs(); // Hier kommt Error -1
				System.out.println("INTRO ENDE");
				System.out.println("-----------------------");

				// Account list
				// getAccountList();

				//
				//getAccountSummary();
				//System.out.println("" + ibReceiver.getNetLiquidationValue());

				//
				//getAllOpenOrders();
				
				// 
				//getAllPositions();
				
				//
				// getContractDetails(getContract1());

				Contract vow3 = this.getContractVw();
				getMktData(vow3);
				//
//				subscribe();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		clientSocket.eDisconnect();
	}

	private void forceFreieOrderId() {
		this.clientSocket.reqIds(-1);
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void getAccountList() throws InterruptedException, IOException {
		try {
			System.out.println("-----------------------");
			clientSocket.reqManagedAccts();
			signal.waitForSignal();
			Thread.sleep(500);
			reader.processMsgs();
		} catch (InterruptedException e) {
			throw e;
//		} catch (IOException e) {
//			throw e;
		}
	}

	private void getAllOpenOrders() throws InterruptedException, IOException {
		try {
			System.out.println("-----------------------");
			clientSocket.reqAllOpenOrders();
			signal.waitForSignal();
			Thread.sleep(500);
			reader.processMsgs();

		} catch (InterruptedException e) {
			throw e;
//		} catch (IOException e) {
//			throw e;
		}
	}
	
	private void getAllPositions() throws InterruptedException, IOException {
		try {
			System.out.println("-----------------------");
			clientSocket.reqPositions();
			signal.waitForSignal();
			Thread.sleep(500);
			reader.processMsgs();

		} catch (InterruptedException e) {
			throw e;
//		} catch (IOException e) {
//			throw e;
		}
	}
	

	private void getAccountSummary() throws InterruptedException, IOException {
		try {
			clientSocket.reqAccountSummary(ibReceiver.getValidId(), "All",
					"AccountType,NetLiquidation,TotalCashValue,SettledCash,AccruedCash,BuyingPower,EquityWithLoanValue,PreviousEquityWithLoanValue,GrossPositionValue,ReqTEquity,ReqTMargin,SMA,InitMarginReq,MaintMarginReq,AvailableFunds,ExcessLiquidity,Cushion,FullInitMarginReq,FullMaintMarginReq,FullAvailableFunds,FullExcessLiquidity,LookAheadNextChange,LookAheadInitMarginReq ,LookAheadMaintMarginReq,LookAheadAvailableFunds,LookAheadExcessLiquidity,HighestSeverity,DayTradesRemaining,Leverage");
			signal.waitForSignal();
			Thread.sleep(500);
			reader.processMsgs();
		} catch (InterruptedException e) {
			throw e;
//		} catch (IOException e) {
//			throw e;
		}
	}

	

	private void getContractDetails(Contract contract) throws InterruptedException, IOException {
		try {
			System.out.println("-----------------------");
			int id = ibReceiver.getValidId();
			clientSocket.reqContractDetails(id, contract);
			signal.waitForSignal();
			Thread.sleep(500);
			reader.processMsgs();

			getMktData(contract);


		} catch (InterruptedException e) {
			throw e;
//		} catch (IOException e) {
//			throw e;
		}
	}

	private void getMktData(Contract contract) throws InterruptedException, IOException {
		try {
			System.out.println("-----------------------");
			System.out.println("getMktData");
//			int tickerid = ibReceiver.getValidId();
			java.util.List<TagValue> l = new java.util.ArrayList<TagValue>();

			clientSocket.reqMktData(2, contract, "", true, true, l);
			signal.waitForSignal();
			Thread.sleep(500);
			reader.processMsgs();

		} catch (InterruptedException e) {
			throw e;
//		} catch (IOException e) {
//			throw e;
		}
	}

	private void subscribe() throws InterruptedException, IOException {
		try {
			System.out.println("-----------------------");
			System.out.println("Current ID: " + ibReceiver.getValidId());
			clientSocket.subscribeToGroupEvents(ibReceiver.getValidId(), 1);
			signal.waitForSignal();
			clientSocket.cancelAccountSummary(ibReceiver.getValidId());
			reader.processMsgs();
//		} catch (InterruptedException e) {
//			throw e;
		} catch (IOException e) {
			throw e;
		}
	}

	private Contract getContractEur() {
		Contract contract = new Contract();
		contract.symbol("EURUSD");
		return contract;
	}

	private Contract getContractIBM() {
		Contract contract1 = new Contract();
		contract1.symbol("IBM");
		contract1.secType("STK");
		contract1.exchange("NYSE");
		return contract1;
	}
	private Contract getContractVw() {
		Contract contract1 = new Contract();
		contract1.symbol("VOW3");
		contract1.secType("STK");
		contract1.exchange("IBIS");
		return contract1;
	}
}
