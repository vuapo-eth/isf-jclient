package iota.pow;

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

    PearlDiver pearlDiver = new PearlDiver();

    public String performPoW(String trytes, int minWeightMagnitude) {
        int[] trits = Converter.trits(trytes);
        if (!pearlDiver.search(trits, minWeightMagnitude, Configs.getInt(P.THREADS_AMOUNT)))
            throw new IllegalStateException("GoldDigger search failed");
        return Converter.trytes(trits);
    }
}