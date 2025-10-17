package de.option.first.chat;
import java.math.BigDecimal;

import com.ib.client.Contract;

public class Watch {
    public final int reqId;
    public final Contract contract;
    public final double threshold;
    public final BigDecimal qty;
    public boolean bought = false;

    public Watch(int reqId, Contract contract, double threshold, BigDecimal qty) {
        this.reqId = reqId;
        this.contract = contract;
        this.threshold = threshold;
        this.qty = qty;
    }
}

