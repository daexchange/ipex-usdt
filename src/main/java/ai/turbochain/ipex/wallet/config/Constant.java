package ai.turbochain.ipex.wallet.config;

/**
 * 
 * <p> TODO</p>
 * @author:         shangxl
 * @Date :          2018年3月7日 下午5:37:00
 */
public class Constant {
	/**
	 * zcash币编码
	 */
	public static final String ZCASH = "zec";
	/**
	 * tv编码
	 */
	public static final String TV = "tv";
	/**
	 * usdt编码 
	 */
	public static final String USDT = "USDT";
	/**
	 * usdt的token Id 正式网络usdt=31，测试网络可以用2
	 */
	public static final String PROPERTYID_USDT = "31";

	/**
	 * 精度
	 */
	public static final String DECIMALS = "_DECIMALS";
	/**
	 * neo
	 */
	public static final String NEO = "NEO";
	/**
	 * bds 
	 */
	public static final String BDS = "BDS";
	/**
	 * bts 
	 */
	public static final String BTS = "BTS";
	/**
	 * gxs
	 */
	public static final String GXS = "GXS";
	/**
	 * eth编码
	 */
	public static final String ETHER = "ETH";
	/**
	 * etc编码
	 */
	public static final String ETC = "ETC";
	/**
	 * etz编码
	 */
	public static final String ETZ = "ETZ";
	/**
	 * 域名
	 */
	public static final String ACT_PREFIX = "http://usdt.ipex.openserver.cn";

	/**
	 * 指定区块的API
	 */
	public static final String ACT_BLOCKNO_HEIGHT = ACT_PREFIX + "/rpc/block-txns/";
	/**
	 * 获取usdt链的高度
	 */
	public static final String ACT_BLOCKNO_LATEST = ACT_PREFIX + "/rpc/height";
	/**
	 * 根据交易txid查询交易的详细信息
	 */
	public static final String ACT_TRANSACTIONS_BY_TXID = ACT_PREFIX + "/rpc/txninfo/";
	/**
	 * 获取账户地址
	 */
	public static final String ACT_NEW_ADDRESS = ACT_PREFIX + "/rpc/address/";
	/**
	 * 获得余额
	 */
	public static final String ACL_ADDRESS_BALANCE = ACT_PREFIX + "/rpc/balance/";
	/**
	 * 转账
	 */
	public static final String ACT_TRANSFER_FROM_ADDRESS = ACT_PREFIX + "/rpc/transfer-from-address";
	/**
	 * 转出地址参数
	 */
	public static final String FORM_ADDRESS_PARAM = "?fromAddress=";
	/**
	 * 转入地址参数
	 */
	public static final String ADDRESS_PARAM = "&address=";
	/**
	 * 转账数量
	 */
	public static final String AMOUNT_PARAM = "&amount=";
	/**
	 * 交易费
	 */
	public static final String FEE_PARAM = "$fee=";
}
