package com.jd.blockchain.ledger.core.impl.handles;

import org.springframework.stereotype.Service;

import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.ledger.BytesValue;
import com.jd.blockchain.ledger.DataAccountKVSetOperation;
import com.jd.blockchain.ledger.DataAccountKVSetOperation.KVWriteEntry;
import com.jd.blockchain.ledger.Operation;
import com.jd.blockchain.ledger.core.DataAccount;
import com.jd.blockchain.ledger.core.LedgerDataSet;
import com.jd.blockchain.ledger.core.LedgerService;
import com.jd.blockchain.ledger.core.OperationHandle;
import com.jd.blockchain.ledger.core.TransactionRequestContext;
import com.jd.blockchain.ledger.core.impl.OperationHandleContext;
import com.jd.blockchain.utils.Bytes;

@Service
public class DataAccountKVSetOperationHandle implements OperationHandle {
	static {
		DataContractRegistry.register(BytesValue.class);
	}

	@Override
	public byte[] process(Operation op, LedgerDataSet dataset, TransactionRequestContext requestContext,
			LedgerDataSet previousBlockDataset, OperationHandleContext handleContext, LedgerService ledgerService) {
		DataAccountKVSetOperation kvWriteOp = (DataAccountKVSetOperation) op;
		DataAccount account = dataset.getDataAccountSet().getDataAccount(kvWriteOp.getAccountAddress());
		KVWriteEntry[] writeset = kvWriteOp.getWriteSet();
		for (KVWriteEntry kvw : writeset) {
			account.setBytes(Bytes.fromString(kvw.getKey()), kvw.getValue(), kvw.getExpectedVersion());
		}

		// No return value;
		return null;
	}

	@Override
	public boolean support(Class<?> operationType) {
		return DataAccountKVSetOperation.class.isAssignableFrom(operationType);
	}

}
