package iota;

import java.util.ArrayList;
import java.util.List;

import isf.AddressManager;
import isf.TipPool;
import isf.TxBroadcaster;
import isf.NodeManager;
import isf.SpamThread;
import isf.UploadDataManager;
import jota.dto.response.GetAttachToTangleResponse;
import jota.dto.response.GetTransactionsToApproveResponse;
import jota.error.ArgumentException;
import jota.model.Input;
import jota.model.Transfer;
import jota.pow.ICurl;

public class IotaAPI extends jota.IotaAPI {
	
	private static final int SECURITY = 2, MIN_WEIGHT_MAGNITUDE = 14;

	protected IotaAPI(Builder builder) {
		super(builder);
	}
	
	public void sendSpam() throws ArgumentException {
		
		ArrayList<Transfer> transfers = new ArrayList<Transfer>();
		String message = UploadDataManager.getNextData();
		transfers.add(new Transfer(AddressManager.getSpamAddress(), 0, message, SpamThread.getTag()));
		List<Input> inputs = new ArrayList<Input>();
        
        List<String> trytes = prepareTransfers("", SECURITY,  transfers, null, inputs, false);

        GetTransactionsToApproveResponse txs = TipPool.getTransactionsToApprove();
		while(txs == null) txs = NodeManager.getTransactionsToApprove();
        final GetAttachToTangleResponse res = attachToTangle(txs.getTrunkTransaction(), txs.getBranchTransaction(), MIN_WEIGHT_MAGNITUDE, trytes.toArray(new String[trytes.size()]));
        
        TxBroadcaster.queueTrytes(res);
	}
	
    public static class Builder extends jota.IotaAPI.Builder {
    	
        public jota.IotaAPI.Builder withCustomCurl(ICurl curl) {
            return super.withCustomCurl(curl);
        }

        public IotaAPI build() {
            super.build();
            return new IotaAPI(this);
        }
    }
}
