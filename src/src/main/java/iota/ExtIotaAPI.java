package iota;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetAttachToTangleResponse;
import jota.dto.response.GetTransactionsToApproveResponse;
import jota.error.BroadcastAndStoreException;
import jota.error.InvalidTrytesException;
import jota.model.Transaction;
import jota.pow.ICurl;

public class ExtIotaAPI extends IotaAPI  {

	protected ExtIotaAPI(Builder builder) {
		super((Builder)builder);
	}
	
	@Override
    public List<Transaction> sendTrytes(final String[] trytes, final int depth, final int minWeightMagnitude) throws InvalidTrytesException {
        
		String trunkTxHash = "", branchTxHash = "";
		
		//long start = System.currentTimeMillis();
		final GetTransactionsToApproveResponse txs = getTransactionsToApprove(depth);
		trunkTxHash = txs.getTrunkTransaction();
		branchTxHash = txs.getBranchTransaction();
		//UIManager.logDbg("waited " + (System.currentTimeMillis()-start) + "ms for "+UIManager.ANSI_CYAN+"getTransactionsToApprove"+UIManager.ANSI_RESET+" reponse");
		
		//long start = System.currentTimeMillis();
        final GetAttachToTangleResponse res = attachToTangle(trunkTxHash, branchTxHash, minWeightMagnitude, trytes);
		//UIManager.logDbg("waited " + (System.currentTimeMillis()-start) + "ms for "+UIManager.ANSI_PURPLE+"GetAttachToTangleResponse"+UIManager.ANSI_RESET+" reponse");

        try {
            broadcastAndStore(res.getTrytes());
        } catch (BroadcastAndStoreException e) {
            return new ArrayList<Transaction>();
        }

        final List<Transaction> trx = new ArrayList<Transaction>();

        for (final String tryte : Arrays.asList(res.getTrytes()))
			trx.add(new Transaction(tryte, null));
        
        return trx;
	}
	
    public static class Builder extends IotaAPI.Builder {

        public Builder withCustomCurl(ICurl curl) {
            return this;
        }

        public ExtIotaAPI build() {
            super.build();
            return new ExtIotaAPI(this);
        }
    }
}
