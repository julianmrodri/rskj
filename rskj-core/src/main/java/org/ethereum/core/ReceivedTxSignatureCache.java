package org.ethereum.core;

import co.rsk.core.RskAddress;
import co.rsk.metrics.profilers.Metric;
import co.rsk.metrics.profilers.Profiler;
import co.rsk.metrics.profilers.ProfilerFactory;
import co.rsk.remasc.RemascTransaction;
import co.rsk.util.MaxSizeHashMap;
import org.ethereum.crypto.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SignatureException;
import java.util.Map;

public class ReceivedTxSignatureCache {
    private static final Logger logger = LoggerFactory.getLogger(ReceivedTxSignatureCache.class);
    private static final Profiler profiler = ProfilerFactory.getInstance();

    private static final int MAX_CACHE_SIZE = 6000;

    private final Map<Transaction, RskAddress> addressesCache;

    public ReceivedTxSignatureCache() {
        addressesCache = new MaxSizeHashMap<>(MAX_CACHE_SIZE,true);
    }

    public RskAddress getSender(Transaction transaction) {

        if (transaction instanceof RemascTransaction) {
            return RemascTransaction.REMASC_ADDRESS;
        }

        RskAddress sender = addressesCache.computeIfAbsent(transaction, this::extractSenderFromSignature);
        return sender;
    }

    private RskAddress extractSenderFromSignature(Transaction tx) {
        Metric metric = profiler.start(Profiler.PROFILING_TYPE.KEY_RECOV_FROM_SIG);
        try {
            ECKey key = ECKey.signatureToKey(tx.getRawHash().getBytes(), tx.getSignature());
            return new RskAddress(key.getAddress());
        } catch (SignatureException e) {
            logger.error("Unable to extract signature for tx {}", tx.getHash(), e);
            return RskAddress.nullAddress();
        } finally {
            profiler.stop(metric);
        }
    }
}
