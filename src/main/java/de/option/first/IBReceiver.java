package de.option.first;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ib.client.Bar;
import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDescription;
import com.ib.client.ContractDetails;
import com.ib.client.Decimal;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.DepthMktDataDescription;
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

public class IBReceiver implements EWrapper {
  
	protected int currentValidId = 0;
	
	public ContractDetails contractDetails = null;
	
	public int getValidId() {
		return currentValidId;
	}
	
	public void clear() {
		contractDetails = null;
	}
	
	@Override
	public void accountDownloadEnd(String arg0) {
		System.out.println("accountDownloadEnd: " + arg0);
	}

	@Override
	public void accountSummary(int arg0, String arg1, String arg2, String arg3, String arg4) {
		System.out.println("accountSummary: " + arg0 + " " + arg1 + " " + arg2 + " " + arg3 + " " + arg4);
	}

	@Override
	public void accountSummaryEnd(int arg0) {
		System.out.println("accountSummaryEnd: " + arg0);
	}

	@Override
	public void accountUpdateMulti(int arg0, String arg1, String arg2, String arg3, String arg4, String arg5) {
		System.out.println("accountUpdateMulti: " + arg0);
	}

	@Override
	public void accountUpdateMultiEnd(int arg0) {
		System.out.println("accountUpdateMultiEnd: " + arg0);
	}

	@Override
	public void bondContractDetails(int arg0, ContractDetails arg1) {
		System.out.println("bondContractDetails: " + arg0);
	}

	@Override
	public void commissionReport(CommissionReport arg0) {
		System.out.println("commissionReport: " + arg0);
		
	}

	@Override
	public void completedOrder(Contract arg0, Order arg1, OrderState arg2) {
		System.out.println("completedOrder: " + arg0);
	}

	@Override
	public void completedOrdersEnd() {
		System.out.println("accountUpdateMulti ");
	}

	@Override
	public void connectAck() {
		System.out.println("connectAck");
	}

	@Override
	public void connectionClosed() {
		System.out.println("connectionClosed");
	}

	@Override
	public void contractDetails(int arg0, ContractDetails arg1) {
		System.out.println("contractDetails: " + arg0  + " " + arg1.toString());
		this.contractDetails = arg1;
	}

	@Override
	public void contractDetailsEnd(int arg0) {
		System.out.println("contractDetailsEnd: " + arg0);
	}

	@Override
	public void currentTime(long arg0) {
		System.out.println("currentTime: " + arg0);
	}

	@Override
	public void deltaNeutralValidation(int arg0, DeltaNeutralContract arg1) {
		System.out.println("deltaNeutralValidation: " + arg0);
	}

	@Override
	public void displayGroupList(int arg0, String arg1) {
		System.out.println("displayGroupList: " + arg0);
	}

	@Override
	public void displayGroupUpdated(int arg0, String arg1) {
		System.out.println("displayGroupUpdated: " + arg0);
	}

	@Override
	public void error(Exception arg0) {
		System.out.println("error: " + arg0);
	}

	@Override
	public void error(String arg0) {
		System.out.println("error: " + arg0);
	}

	@Override
	public void error(int arg0, int arg1, String arg2, String arg3) {
		System.out.println("error: " + arg0);
	}

	@Override
	public void execDetails(int arg0, Contract arg1, Execution arg2) {
		System.out.println("execDetails: " + arg0);
	}

	@Override
	public void execDetailsEnd(int arg0) {
		System.out.println("execDetailsEnd: " + arg0);
	}

	@Override
	public void familyCodes(FamilyCode[] arg0) {
		System.out.println("familyCodes: " + arg0);
	}

	@Override
	public void fundamentalData(int arg0, String arg1) {
		System.out.println("fundamentalData: " + arg0);
	}

	@Override
	public void headTimestamp(int arg0, String arg1) {
		System.out.println("headTimestamp: " + arg0);
	}

	@Override
	public void histogramData(int arg0, List<HistogramEntry> arg1) {
		System.out.println("histogramData: " + arg0);
	}

	@Override
	public void historicalData(int arg0, Bar arg1) {
		System.out.println("historicalData: " + arg0);
	}

	@Override
	public void historicalDataEnd(int arg0, String arg1, String arg2) {
		System.out.println("historicalDataEnd: " + arg0);
	}

	@Override
	public void historicalDataUpdate(int arg0, Bar arg1) {
		System.out.println("historicalDataUpdate: " + arg0);
	}

	@Override
	public void historicalNews(int arg0, String arg1, String arg2, String arg3, String arg4) {
		System.out.println("historicalNews: " + arg0);
	}

	@Override
	public void historicalNewsEnd(int arg0, boolean arg1) {
		System.out.println("historicalNewsEnd: " + arg0);
	}

	@Override
	public void historicalSchedule(int arg0, String arg1, String arg2, String arg3, List<HistoricalSession> arg4) {
		System.out.println("historicalSchedule: " + arg0);
	}

	@Override
	public void historicalTicks(int arg0, List<HistoricalTick> arg1, boolean arg2) {
		System.out.println("historicalTicks: " + arg0);
	}

	@Override
	public void historicalTicksBidAsk(int arg0, List<HistoricalTickBidAsk> arg1, boolean arg2) {
		System.out.println("historicalTicksBidAsk: " + arg0);
	}

	@Override
	public void historicalTicksLast(int arg0, List<HistoricalTickLast> arg1, boolean arg2) {
		System.out.println("historicalTicksLast: " + arg0);
	}

	@Override
	public void managedAccounts(String arg0) {
		System.out.println("managedAccounts: " + arg0);
	}

	@Override
	public void marketDataType(int arg0, int arg1) {
		System.out.println("marketDataType: " + arg0);
	}

	@Override
	public void marketRule(int arg0, PriceIncrement[] arg1) {
		System.out.println("marketRule: " + arg0);
	}

	@Override
	public void mktDepthExchanges(DepthMktDataDescription[] arg0) {
		System.out.println("mktDepthExchanges: " + arg0);
	}

	@Override
	public void newsArticle(int arg0, int arg1, String arg2) {
		System.out.println("newsArticle: " + arg0);
	}

	@Override
	public void newsProviders(NewsProvider[] arg0) {
		System.out.println("newsProviders: " + arg0);
	}

	@Override
	public void nextValidId(int arg0) {
		System.out.println("nextValidId: " + arg0);
		this.currentValidId = arg0;
	}

	@Override
	public void openOrder(int arg0, Contract arg1, Order arg2, OrderState arg3) {
		System.out.println("openOrder: " + arg0);
	}

	@Override
	public void openOrderEnd() {
		System.out.println("openOrderEnd");
	}

	@Override
	public void orderBound(long arg0, int arg1, int arg2) {
		System.out.println("orderBound: " + arg0);
	}

	@Override
	public void orderStatus(int arg0, String arg1, Decimal arg2, Decimal arg3, double arg4, int arg5, int arg6,
			double arg7, int arg8, String arg9, double arg10) {
		System.out.println("orderStatus: " + arg0);
	}

	@Override
	public void pnl(int arg0, double arg1, double arg2, double arg3) {
		System.out.println("pnl: " + arg0);
	}

	@Override
	public void pnlSingle(int arg0, Decimal arg1, double arg2, double arg3, double arg4, double arg5) {
		System.out.println("REPLACpnlSingleETHISTEXT: " + arg0);
	}

	@Override
	public void position(String arg0, Contract arg1, Decimal arg2, double arg3) {
		System.out.println("position: " + arg0);
	}

	@Override
	public void positionEnd() {
		System.out.println("positionEnd");
	}

	@Override
	public void positionMulti(int arg0, String arg1, String arg2, Contract arg3, Decimal arg4, double arg5) {
		System.out.println("positionMulti: " + arg0);
	}

	@Override
	public void positionMultiEnd(int arg0) {
		System.out.println("positionMultiEnd: " + arg0);
	}

	@Override
	public void realtimeBar(int arg0, long arg1, double arg2, double arg3, double arg4, double arg5, Decimal arg6,
			Decimal arg7, int arg8) {
		System.out.println("realtimeBar: " + arg0);
	}

	@Override
	public void receiveFA(int arg0, String arg1) {
		System.out.println("receiveFA: " + arg0);
	}

	@Override
	public void replaceFAEnd(int arg0, String arg1) {
		System.out.println("replaceFAEnd: " + arg0);
	}

	@Override
	public void rerouteMktDataReq(int arg0, int arg1, String arg2) {
		System.out.println("rerouteMktDataReq: " + arg0);
	}

	@Override
	public void rerouteMktDepthReq(int arg0, int arg1, String arg2) {
		System.out.println("rerouteMktDepthReq: " + arg0);
	}

	@Override
	public void scannerData(int arg0, int arg1, ContractDetails arg2, String arg3, String arg4, String arg5,
			String arg6) {
		System.out.println("scannerData: " + arg0);
	}

	@Override
	public void scannerDataEnd(int arg0) {
		System.out.println("scannerDataEnd: " + arg0);
	}

	@Override
	public void scannerParameters(String arg0) {
		System.out.println("scannerParameters: " + arg0);
	}

	@Override
	public void securityDefinitionOptionalParameter(int arg0, String arg1, int arg2, String arg3, String arg4,
			Set<String> arg5, Set<Double> arg6) {
		System.out.println("securityDefinitionOptionalParameter: " + arg0);
	}

	@Override
	public void securityDefinitionOptionalParameterEnd(int arg0) {
		System.out.println("securityDefinitionOptionalParameterEnd: " + arg0);
	}

	@Override
	public void smartComponents(int arg0, Map<Integer, Entry<String, Character>> arg1) {
		System.out.println("smartComponents: " + arg0);
	}

	@Override
	public void softDollarTiers(int arg0, SoftDollarTier[] arg1) {
		System.out.println("softDollarTiers: " + arg0);
	}

	@Override
	public void symbolSamples(int arg0, ContractDescription[] arg1) {
		System.out.println("symbolSamples: " + arg0);
	}

	@Override
	public void tickByTickAllLast(int arg0, int arg1, long arg2, double arg3, Decimal arg4, TickAttribLast arg5,
			String arg6, String arg7) {
		System.out.println("tickByTickAllLast: " + arg0);
	}

	@Override
	public void tickByTickBidAsk(int arg0, long arg1, double arg2, double arg3, Decimal arg4, Decimal arg5,
			TickAttribBidAsk arg6) {
		System.out.println("tickByTickBidAsk: " + arg0);
	}

	@Override
	public void tickByTickMidPoint(int arg0, long arg1, double arg2) {
		System.out.println("tickByTickMidPoint: " + arg0);
	}

	@Override
	public void tickEFP(int arg0, int arg1, double arg2, String arg3, double arg4, int arg5, String arg6, double arg7,
			double arg8) {
		System.out.println("tickEFP: " + arg0);
	}

	@Override
	public void tickGeneric(int arg0, int arg1, double arg2) {
		System.out.println("tickGeneric: " + arg0);
	}

	@Override
	public void tickNews(int arg0, long arg1, String arg2, String arg3, String arg4, String arg5) {
		System.out.println("tickNews: " + arg0);
	}

	@Override
	public void tickOptionComputation(int arg0, int arg1, int arg2, double arg3, double arg4, double arg5, double arg6,
			double arg7, double arg8, double arg9, double arg10) {
		System.out.println("tickOptionComputation: " + arg0);
	}

	@Override
	public void tickPrice(int arg0, int arg1, double arg2, TickAttrib arg3) {
		System.out.println("tickPrice: " + arg0);
	}

	@Override
	public void tickReqParams(int arg0, double arg1, String arg2, int arg3) {
		System.out.println("tickReqParams: " + arg0);
	}

	@Override
	public void tickSize(int arg0, int arg1, Decimal arg2) {
		System.out.println("tickSize: " + arg0);
	}

	@Override
	public void tickSnapshotEnd(int arg0) {
		System.out.println("tickSnapshotEnd: " + arg0);
	}

	@Override
	public void tickString(int arg0, int arg1, String arg2) {
		System.out.println("tickString: " + arg0);
	}

	@Override
	public void updateAccountTime(String arg0) {
		System.out.println("updateAccountTime: " + arg0);
	}

	@Override
	public void updateAccountValue(String arg0, String arg1, String arg2, String arg3) {
		System.out.println("updateAccountValue: " + arg0);
	}

	@Override
	public void updateMktDepth(int arg0, int arg1, int arg2, int arg3, double arg4, Decimal arg5) {
		System.out.println("updateMktDepth: " + arg0);
	}

	@Override
	public void updateMktDepthL2(int arg0, int arg1, String arg2, int arg3, int arg4, double arg5, Decimal arg6,
			boolean arg7) {
		System.out.println("updateMktDepthL2: " + arg0);
	}

	@Override
	public void updateNewsBulletin(int arg0, int arg1, String arg2, String arg3) {
		System.out.println("updateNewsBulletin: " + arg0);
	}

	@Override
	public void updatePortfolio(Contract arg0, Decimal arg1, double arg2, double arg3, double arg4, double arg5,
			double arg6, String arg7) {
		System.out.println("updatePortfolio: " + arg0);
	}

	@Override
	public void userInfo(int arg0, String arg1) {
		System.out.println("userInfo: " + arg0);
	}

	@Override
	public void verifyAndAuthCompleted(boolean arg0, String arg1) {
		System.out.println("verifyAndAuthCompleted: " + arg0);
	}

	@Override
	public void verifyAndAuthMessageAPI(String arg0, String arg1) {
		System.out.println("verifyAndAuthMessageAPI: " + arg0);
	}

	@Override
	public void verifyCompleted(boolean arg0, String arg1) {
		System.out.println("verifyCompleted: " + arg0);
	}

	@Override
	public void verifyMessageAPI(String arg0) {
		System.out.println("verifyMessageAPI: " + arg0);
	}

	@Override
	public void wshEventData(int arg0, String arg1) {
		System.out.println("wshEventData: " + arg0);
	}

	@Override
	public void wshMetaData(int arg0, String arg1) {
		System.out.println("wshMetaData " + arg0);
	}

}