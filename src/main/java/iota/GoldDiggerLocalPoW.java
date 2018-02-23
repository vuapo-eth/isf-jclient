package iota;

import cfb.pearldiver.PearlDiver;
import isf.Configs;
import isf.P;
import jota.IotaLocalPoW;
import jota.utils.Converter;

/**
 * Perform local PoW using Come-from-Beyond's PearlDiver implementation.
 * EDIT by microhash: threading
 */
public class GoldDiggerLocalPoW implements IotaLocalPoW {

	private static int amountPoW = 0;
	private static long totalTimePoW = 0;
	
    PearlDiver pearlDiver = new PearlDiver();

    public String performPoW(String trytes, int minWeightMagnitude) {
        int[] trits = Converter.trits(trytes);
        long timeStarted = System.currentTimeMillis();
        if (!pearlDiver.search(trits, minWeightMagnitude, Configs.getInt(P.THREADS_AMOUNT_POW)))
            throw new IllegalStateException("GoldDigger search failed");
        totalTimePoW += System.currentTimeMillis() - timeStarted;
        amountPoW++;
        return Converter.trytes(trits);
    }
    
    public static double getAvgPoWTime() {
		return amountPoW == 0 ? 0 : 0.001 * totalTimePoW / amountPoW;
	}
}