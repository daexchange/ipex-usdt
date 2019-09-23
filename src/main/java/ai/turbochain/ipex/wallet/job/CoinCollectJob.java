package ai.turbochain.ipex.wallet.job;

import java.math.BigDecimal;

import ai.turbochain.ipex.wallet.config.Constant;
import ai.turbochain.ipex.wallet.utils.HttpRequest;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.spark.blockchain.rpcclient.BitcoinUtil;

import ai.turbochain.ipex.wallet.config.JsonrpcClient;
import ai.turbochain.ipex.wallet.entity.Coin;
import ai.turbochain.ipex.wallet.service.AccountService;
import ai.turbochain.ipex.wallet.util.AccountReplay;

//@Component
public class CoinCollectJob {
    private Logger logger = LoggerFactory.getLogger(CoinCollectJob.class);
    @Autowired
    private AccountService accountService;
    @Autowired
    private JsonrpcClient rpcClient;
    @Autowired
    private Coin coin;

    //@Scheduled(cron = "0 0 15 * * *")
    public void rechargeMinerFee(){
        try {
            AccountReplay accountReplay = new AccountReplay(accountService, 100);
            accountReplay.run(account -> {
                BigDecimal btcBalance = rpcClient.getAddressBalance(account.getAddress());
                if(btcBalance.compareTo(coin.getRechargeMinerFee()) < 0) {
                    // 获取usdtd的余额
                    String availAmtStr = HttpRequest.sendGetData(Constant.ACL_ADDRESS_BALANCE + account.getAddress(), "");
                    JSONObject availAmtInfo = JSONObject.parseObject(availAmtStr);
                    BigDecimal usdtBalance = availAmtInfo.getBigDecimal("data");
//                    BigDecimal usdtBalance = rpcClient.omniGetBalance(account.getAddress());
                    if(usdtBalance.compareTo(coin.getMinCollectAmount()) >= 0) {
                        try {
                            String txid = BitcoinUtil.sendTransaction(rpcClient, coin.getWithdrawAddress(), account.getAddress(), coin.getRechargeMinerFee(), coin.getDefaultMinerFee());
                            logger.info("BitcoinUtil.sendTransaction:address={},txid={}", account.getAddress(), txid);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
