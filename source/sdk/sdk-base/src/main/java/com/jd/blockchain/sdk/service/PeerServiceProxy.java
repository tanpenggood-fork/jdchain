package com.jd.blockchain.sdk.service;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.TransactionRequest;
import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.sdk.BlockchainException;
import com.jd.blockchain.sdk.LedgerAccessContext;
import com.jd.blockchain.sdk.proxy.BlockchainServiceProxy;
import com.jd.blockchain.transaction.BlockchainQueryService;
import com.jd.blockchain.transaction.TransactionService;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 对共识节点的区块链服务代理；
 *
 * @author huanghaiquan
 *
 */
public class PeerServiceProxy extends BlockchainServiceProxy implements TransactionService {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PeerServiceProxy.class);

	private final Lock accessLock = new ReentrantLock();

	/**
	 * 许可的账本以及交易列表；
	 */
	private Map<HashDigest, LedgerAccessContext> ledgerAccessContexts;

	public PeerServiceProxy(LedgerAccessContext[] accessAbleLedgers) {
		this.ledgerAccessContexts = new HashMap<>();
		for (LedgerAccessContext lac : accessAbleLedgers) {
			if (ledgerAccessContexts.containsKey(lac.getLedgerHash())) {
				throw new IllegalArgumentException(
						String.format("Ledger repeatly! --[LedgerHash=%s]", lac.getLedgerHash().toBase58()));
			}
			ledgerAccessContexts.put(lac.getLedgerHash(), lac);
		}
	}

	public void addLedgerAccessContexts(LedgerAccessContext[] accessAbleLedgers) {
		accessLock.lock();
		try {
			if (this.ledgerAccessContexts == null) {
				throw new IllegalArgumentException("LedgerAccessContexts is null, you need init first !!!");
			}
			for (LedgerAccessContext lac : accessAbleLedgers) {
				if (!ledgerAccessContexts.containsKey(lac.getLedgerHash())) {
					ledgerAccessContexts.put(lac.getLedgerHash(), lac);
				}
			}
		} finally {
			accessLock.unlock();
		}
	}

	@Override
	protected TransactionService getTransactionService(HashDigest ledgerHash) {
		return getLedgerAccessContext(ledgerHash).getTransactionService();
	}

	@Override
	protected BlockchainQueryService getQueryService(HashDigest ledgerHash) {
		return getLedgerAccessContext(ledgerHash).getQueryService();
	}

	private LedgerAccessContext getLedgerAccessContext(HashDigest ledgerHash) {
		LedgerAccessContext lac = ledgerAccessContexts.get(ledgerHash);
		if (lac == null) {
			throw new BlockchainException("Unsupported access ledger[" + ledgerHash.toBase58() + "] !");
		}
		return lac;
	}

	@Override
	public HashDigest[] getLedgerHashs() {
		return ledgerAccessContexts.keySet().toArray(new HashDigest[ledgerAccessContexts.size()]);
	}

	/**
	 * 直接获取账本信息
	 *         不通过内部缓存
	 *
	 * @return
	 */
	public HashDigest[] getLedgerHashsDirect() {
		Set<HashDigest> ledgerHashs = new HashSet<>();
		if (ledgerAccessContexts != null && !ledgerAccessContexts.isEmpty()) {
			Collection<LedgerAccessContext> ctxs = ledgerAccessContexts.values();
			for (LedgerAccessContext ctx : ctxs) {
				HashDigest[] hashs = ctx.getQueryService().getLedgerHashs();
				for (HashDigest h : hashs) {
					LOGGER.info("Get ledger[{}] by [{}]'s AccessContext !!!", h.toBase58(), ctx.getLedgerHash().toBase58());
				}
				ledgerHashs.addAll(Arrays.asList(hashs));
			}
		}
		if (ledgerHashs.isEmpty()) {
			return new HashDigest[0];
		}
		return ledgerHashs.toArray(new HashDigest[ledgerHashs.size()]);
	}

	/**
	 * 处理网关的交易转发；
	 */
	@Override
	public TransactionResponse process(TransactionRequest txRequest) {
		TransactionService targetTxService = getTransactionService(txRequest.getTransactionContent().getLedgerHash());
		return targetTxService.process(txRequest);
	}
}
