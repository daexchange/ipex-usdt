package ai.turbochain.ipex.wallet.controller;

import ai.turbochain.ipex.wallet.config.Constant;
import ai.turbochain.ipex.wallet.config.JsonrpcClient;
import ai.turbochain.ipex.wallet.entity.Account;
import ai.turbochain.ipex.wallet.entity.Coin;
import ai.turbochain.ipex.wallet.service.AccountService;
import ai.turbochain.ipex.wallet.service.DepositService;
import ai.turbochain.ipex.wallet.util.MessageResult;

import ai.turbochain.ipex.wallet.utils.HttpRequest;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/rpc")
public class WalletController {
	@Autowired
	private JsonrpcClient rpcClient;
	@Autowired
	private Coin coin;
	private Logger logger = LoggerFactory.getLogger(WalletController.class);
	@Autowired
	private AccountService accountService;

	@GetMapping("address/{account}")
	public MessageResult getNewAddress(@PathVariable String account) {
		logger.info("create new address :" + account);
//		String address = rpcClient.getNewAddress(account);
		String addressStr = HttpRequest.sendGetData(Constant.ACT_NEW_ADDRESS + account, "");
		JSONObject addressInfo = JSONObject.parseObject(addressStr);
		String address = addressInfo.getString("data");
		accountService.saveOne(account, address);
		MessageResult result = new MessageResult(0, "success");
		result.setData(address);
		return result;
	}

	@GetMapping("withdraw")
	public MessageResult withdraw(String address, BigDecimal amount, BigDecimal fee) {
		logger.info("withdraw:address={},amount={},fee={}", address, amount, fee);
		try {
			String txid = rpcClient.omniSend(coin.getWithdrawAddress(), address, amount);
			MessageResult result = new MessageResult(0, "success");
			result.setData(txid);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "error:" + e.getMessage());
		}
	}

	/**
	 * 多个账户转账->address
	 * 
	 * @param address
	 * @param amount
	 * @return
	 */
	@GetMapping("transfer")
	public MessageResult transfer(String address, BigDecimal amount) {
		logger.info("transfer:address={},amount={},fee={}", address, amount);
		try {
			List<Account> accounts = accountService.findAll();
			BigDecimal transferedAmt = BigDecimal.ZERO;
			for (Account account : accounts) {
				if (account.getAddress().equalsIgnoreCase(address))
					continue;
//				// 获取btc的余额
//				String btcFeeStr = HttpRequest.sendGetData(Constant.ACL_ADDRESS_BALANCE + address, "");
//				JSONObject btcFeeInfo = JSONObject.parseObject(btcFeeStr);
//				BigDecimal btcFee = btcFeeInfo.getBigDecimal("data");
				BigDecimal btcFee = rpcClient.getAddressBalance(account.getAddress());
				if (btcFee.compareTo(coin.getDefaultMinerFee()) < 0) {
					logger.info("地址{}矿工费不足，最小为{},当前为{}", account.getAddress(), coin.getDefaultMinerFee(), btcFee);
					continue;
				}
				BigDecimal availAmt = rpcClient.omniGetBalance(account.getAddress());

				if (availAmt.compareTo(amount.subtract(transferedAmt)) > 0) {
					availAmt = amount.subtract(transferedAmt);
				}
				if (availAmt.compareTo(BigDecimal.ZERO) <= 0) {
					continue;
				}

				String txid = rpcClient.omniSend(account.getAddress(), address, availAmt);
				if (txid != null) {
					System.out.println("fromAddress" + account.getAddress() + ",txid:" + txid);
					transferedAmt = transferedAmt.add(availAmt);
				}
				if (transferedAmt.compareTo(amount) >= 0) {
					break;
				}
			}
			MessageResult result = new MessageResult(0, "success");
			result.setData(transferedAmt);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "error:" + e.getMessage());
		}
	}

	@GetMapping("transfer-from-address")
	public MessageResult transferFromAddress(String fromAddress, String address, BigDecimal amount, BigDecimal fee) {
		logger.info("transfer:fromeAddress={},address={},amount={},fee={}", fromAddress, address, amount, fee);
		try {
			// 拼接转账的url
			String url = Constant.ACT_TRANSFER_FROM_ADDRESS + Constant.FORM_ADDRESS_PARAM + fromAddress + Constant.ADDRESS_PARAM + address
					+ Constant.AMOUNT_PARAM + amount;
			// 获取结果
			String txidStr = HttpRequest.sendGetData(url, "");
			JSONObject txidInfo = JSONObject.parseObject(txidStr);
			String txid = txidInfo.getString("data");
			String message = "";
			if (StringUtils.isBlank(txid)) {
				message = "Transfer failed";
			} else {
				message = "Transfer success";
			}
			MessageResult result = new MessageResult(0, message);
			result.setData(txid);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "error:" + e.getMessage());
		}
	}

	/**
	 * 所有账户余额
	 * 
	 * @return
	 */
	@GetMapping("balance")
	public MessageResult balance() {
		BigDecimal amount = BigDecimal.ZERO;
		try {
			List<Account> accounts = accountService.findAll();
			for (int i = 0; i < accounts.size(); i++) {
//				amount = amount.add(rpcClient.omniGetBalance(accounts.get(i).getAddress()));
				// 获取usdtd的余额
				String availAmtStr = HttpRequest.sendGetData(Constant.ACL_ADDRESS_BALANCE + accounts.get(i).getAddress(), "");
				JSONObject availAmtInfo = JSONObject.parseObject(availAmtStr);
				BigDecimal availAmt = availAmtInfo.getBigDecimal("data");
				amount = amount.add(availAmt);
			}
			MessageResult result = new MessageResult(0, "success");
			result.setData(amount);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "error:" + e.getMessage());
		}
	}

	@GetMapping("balance/{address}")
	public MessageResult balance(@PathVariable String address) {
		try {
//			BigDecimal balance = rpcClient.omniGetBalance(address);
			// 获取usdtd的余额
			String availAmtStr = HttpRequest.sendGetData(Constant.ACL_ADDRESS_BALANCE + address, "");
			JSONObject availAmtInfo = JSONObject.parseObject(availAmtStr);
			BigDecimal balance = availAmtInfo.getBigDecimal("data");
			MessageResult result = new MessageResult(0, "success");
			result.setData(balance);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageResult.error(500, "error:" + e.getMessage());
		}
	}

}
