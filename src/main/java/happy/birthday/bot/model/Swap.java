package happy.birthday.bot.model;

public record Swap(
        String instType, String instId, double last,
        double lastSz, double askPx, double askSz,
        double bidPx, double bidSz, double open24h,
        double high24h,double low24h,double volCcy24h,
        double vol24h,double ts,double sodUtc0,
        double sodUtc8

) implements JsonObject{
}
/*
* {
  "instType" : "SWAP",
  "instId" : "CETUS-USDT-SWAP",
  "last" : "0.11818",
  "lastSz" : "17",
  "askPx" : "0.11819",
  "askSz" : "1",
  "bidPx" : "0.11817",
  "bidSz" : "185",
  "open24h" : "0.13189",
  "high24h" : "0.13366",
  "low24h" : "0.1106",
  "volCcy24h" : "386531990",
  "vol24h" : "38653199",
  "ts" : "1707316053506",
  "sodUtc0" : "0.12752",
  "sodUtc8" : "0.13272"
}
*
*
*
*
*
* */