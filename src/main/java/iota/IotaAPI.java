package iota;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import isf.GttaThread;
import jota.dto.response.GetAttachToTangleResponse;
import jota.dto.response.GetTransactionsToApproveResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;
import jota.pow.ICurl;
import jota.pow.SpongeFactory;

public class IotaAPI extends jota.IotaAPI {
	
    private ICurl customCurl;

	protected IotaAPI(Builder builder) {
		super(builder);
        customCurl = builder.customCurl;
	}
	
	@Override
    public List<Transaction> sendTrytes(final String[] trytes, final int depth, final int minWeightMagnitude) throws ArgumentException {
    	
        GetTransactionsToApproveResponse txs = GttaThread.getTransactionsToApprove();
		if(txs == null) txs = getTransactionsToApprove(depth);
        
        final GetAttachToTangleResponse res = attachToTangle(txs.getTrunkTransaction(), txs.getBranchTransaction(), minWeightMagnitude, trytes);

        try {
            broadcastAndStore(res.getTrytes());
        } catch (ArgumentException e) {
            return new ArrayList<>();
        }

        final List<Transaction> trx = new ArrayList<>();

        for (final String tryte : Arrays.asList(res.getTrytes())) {
            trx.add(new Transaction(tryte, customCurl.clone()));
        }
        return trx;
    }

    public static class Builder extends jota.IotaAPI.Builder {
        private ICurl customCurl = SpongeFactory.create(SpongeFactory.Mode.KERL);

        public jota.IotaAPI.Builder withCustomCurl(ICurl curl) {
            customCurl = curl;
            return super.withCustomCurl(curl);
        }

        public IotaAPI build() {
            super.build();
            return new IotaAPI(this);
        }
	}
}
