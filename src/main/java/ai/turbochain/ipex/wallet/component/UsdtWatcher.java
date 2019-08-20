package ai.turbochain.ipex.wallet.component;

import ai.turbochain.ipex.wallet.config.Constant;
import ai.turbochain.ipex.wallet.config.JsonrpcClient;
import ai.turbochain.ipex.wallet.entity.Account;
import ai.turbochain.ipex.wallet.entity.Coin;
import ai.turbochain.ipex.wallet.entity.Deposit;
import ai.turbochain.ipex.wallet.service.AccountService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.spark.blockchain.rpcclient.BitcoinUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

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
				List<String> list = jsonrpcClient.omniListBlockTransactions(blockHeight);
				for (String txid : list) {
					Map<String, Object> map = jsonrpcClient.omniGetTransactions(txid);
					if (map.get("propertyid") == null)
						continue;
					String propertyid = map.get("propertyid").toString();
					String txId = map.get("txid").toString();
					String address = String.valueOf(map.get("referenceaddress"));
					Boolean valid = Boolean.parseBoolean(map.get("valid").toString());
					if (propertyid.equals(Constant.PROPERTYID_USDT) && valid) {
						if (accountService.isAddressExist(address)) {
							Deposit deposit = new Deposit();
							deposit.setTxid(txId);
							deposit.setBlockHash(String.valueOf(map.get("blockhash")));
							deposit.setAmount(new BigDecimal(map.get("amount").toString()));
							deposit.setAddress(address);
							logger.info("receive usdt {}", String.valueOf(map.get("referenceaddress")));
							deposit.setBlockHeight(Long.valueOf(String.valueOf(map.get("block"))));
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
			rpcClient.omniSend(deposit.getAddress(), coin.getWithdrawAddress(), deposit.getAmount());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Long getNetworkBlockHeight() {
		try {
			return Long.valueOf(jsonrpcClient.getBlockCount());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0L;
	}

}
