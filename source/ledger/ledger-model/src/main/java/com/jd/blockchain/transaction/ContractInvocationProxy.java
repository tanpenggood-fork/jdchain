package com.jd.blockchain.transaction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import com.jd.blockchain.contract.ContractSerializeUtils;
import com.jd.blockchain.ledger.ContractEventSendOperation;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.IllegalDataException;

public class ContractInvocationProxy implements InvocationHandler {

	// private String contractMessage;

//	private Bytes contractAddress;

	private ContractType contractType;

//	private ContractEventSendOperationBuilder sendOpBuilder;

	private ContractEventSendOperation sendOperation;

	public ContractInvocationProxy(Bytes contractAddress, ContractType contractType,
			ContractEventSendOperationBuilder sendOpBuilder) {
//		this.contractAddress = contractAddress;
		if(contractType == null){
			throw new IllegalDataException("contractType == null, no invoke really.");
		}
		this.contractType = contractType;
//		this.sendOpBuilder = sendOpBuilder;
		// Send一个地址，但不涉及Event
		this.sendOperation = sendOpBuilder.send(contractAddress);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		// 判断是否是常规方法调用
		if (method.getName().equals("hashCode")) {
			// 该处需要使用当前代理类的HashCode
			return this.hashCode();
		}
		if (method.getName().equals("toString")) {
			// 该处使用当前代理类的toString
			return this.toString();
		}

		if (sendOperation.getEvent() != null) {
			throw new IllegalStateException("Contract Object can only execute method one time !!!");
		}

		String event = contractType.getEvent(method);
		if (event == null) {
			// 该方法不是合约可执行的方法
			throw new IllegalAccessException(String.format("This Method [%s] is not Contract Event Method !!!",
					method.getName()));
		}
		// 合约方法；
		byte[] argBytes = serializeArgs(args);
		if (sendOperation instanceof ContractEventSendOpTemplate) {
			((ContractEventSendOpTemplate) sendOperation).setEventAndArgs(event, argBytes);
		}
		// 代理操作，返回值类型无法创建
		return null;
	}

	private byte[] serializeArgs(Object[] args) {
		return ContractSerializeUtils.serializeArray(args);
	}

	public int opIndex() {
		if (sendOperation instanceof ContractEventSendOpTemplate) {
			return ((ContractEventSendOpTemplate) sendOperation).getOpIndex();
		}
		return -1;
	}
}
