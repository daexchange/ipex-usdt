package ai.turbochain.ipex.wallet.job;

import java.math.BigDecimal;

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
                    BigDecimal usdtBalance = rpcClient.omniGetBalance(account.getAddress());
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
