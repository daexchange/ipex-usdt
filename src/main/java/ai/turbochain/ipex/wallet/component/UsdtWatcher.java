package ai.turbochain.ipex.wallet.component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import ai.turbochain.ipex.wallet.entity.Coin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.spark.blockchain.rpcclient.BitcoinUtil;

import ai.turbochain.ipex.wallet.config.Constant;
import ai.turbochain.ipex.wallet.config.JsonrpcClient;
import ai.turbochain.ipex.wallet.entity.Deposit;
import ai.turbochain.ipex.wallet.service.AccountService;

import ai.turbochain.ipex.wallet.utils.HttpRequest;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

@Component
public class UsdtWatcher extends Watcher {
	private Logger logger = LoggerFactory.getLogger(UsdtWatcher.class);
	@Autowired
	private JsonrpcClient jsonrpcClient;
	@Autowired
	private AccountService accountService;
	@Autowired
	private ExecutorService executorService;
	@Autowired
	private JsonrpcClient rpcClient;
	@Autowired
	private Coin coin;

	public List<Deposit> replayBlock(Long startBlockNumber, Long endBlockNumber) {
		List<Deposit> deposits = new ArrayList<>();
		try {
			for (Long blockHeight = startBlockNumber; blockHeight <= endBlockNumber; blockHeight++) {
				// 获取特定区块的数据
				String blockStr = HttpRequest.sendGetData(Constant.ACT_BLOCKNO_HEIGHT + blockHeight,"");
				JSONObject jsonObject = JSONObject.parseObject(blockStr);
				JSONArray dataArr = jsonObject.getJSONArray("data");
				for (int i = 0; i < dataArr.size(); i++) {
					// 根据交易id查询交易的详细信息
					String txnInfoStr = HttpRequest.sendGetData(Constant.ACT_TRANSACTIONS_BY_TXID + dataArr.get(i), "");
					JSONObject txnInfo = JSONObject.parseObject(txnInfoStr);
					JSONObject data = txnInfo.getJSONObject("data");
					if (StringUtils.isBlank(data.getString("propertyid"))) {
						continue;
					}
					String propertyid = data.getString("propertyid");
					String txId = data.getString("txid");
					String address = data.getString("referenceaddress");
					Boolean valid = data.getBoolean("valid");
					if (propertyid.equals(Constant.PROPERTYID_USDT) && valid) {
						if (accountService.isAddressExist(address)) {
							Deposit deposit = new Deposit();
							deposit.setTxid(txId);
							deposit.setBlockHash(data.getString("blockhash"));
							deposit.setAmount(data.getBigDecimal("amount"));
							logger.info("receive usdt {}", address);
							deposit.setAddress(address);
							deposit.setBlockHeight(data.getLong("block"));
							deposits.add(deposit);
							afterDeposit(deposit);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return deposits;
	}

	/**
	 * 注册成功后的操作
	 */
	public void afterDeposit(Deposit deposit) {
		executorService.execute(new Runnable() {
			public void run() {
				depositCoin(deposit);
			}
		});
	}

	/**
	 * 充值USDT转账到withdraw账户
	 * 
	 * @param deposit
	 */
	public void depositCoin(Deposit deposit) {
		try {
			BigDecimal btcFee = rpcClient.getAddressBalance(deposit.getAddress());
			if (btcFee.compareTo(coin.getDefaultMinerFee()) < 0) {
				logger.info("地址{}矿工费不足，最小为{},当前为{}", deposit.getAddress(), coin.getDefaultMinerFee(), btcFee);
				String txid = BitcoinUtil.sendTransaction(rpcClient, coin.getWithdrawAddress(), deposit.getAddress(),
						coin.getRechargeMinerFee(), coin.getDefaultMinerFee());
				if (txid == null) {
					return;
				}
				Thread.sleep(1000 * 60 * 120);// 给充值地址转PWR作为手续费，2小时交易确认
				logger.info("{}手续费不足，转账BTC到充值账户作为手续费:from={},to={},amount={}", deposit.getAddress(),
						coin.getWithdrawAddress(), deposit.getAddress(), btcFee);
			}
			logger.info("充值USDT转账到withdraw账户:from={},to={},amount={}", deposit.getAddress(), coin.getWithdrawAddress(),
					deposit.getAmount());
//			rpcClient.omniSend(deposit.getAddress(), coin.getWithdrawAddress(), deposit.getAmount());
			// 调用转账
			String url = Constant.ACT_TRANSFER_FROM_ADDRESS + Constant.FORM_ADDRESS_PARAM + deposit.getAddress() + Constant.ADDRESS_PARAM + coin.getWithdrawAddress()
					+ Constant.AMOUNT_PARAM + deposit.getAmount();
			HttpRequest.sendGetData(url, "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Long getNetworkBlockHeight() {
		try {
			String blockCountStr = HttpRequest.sendGetData(Constant.ACT_BLOCKNO_LATEST, "");
			JSONObject jsonObject = JSONObject.parseObject(blockCountStr);
			Long blockCount = jsonObject.getLong("data");
			return blockCount;
//			return Long.valueOf(jsonrpcClient.getBlockCount());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0L;
	}

}
