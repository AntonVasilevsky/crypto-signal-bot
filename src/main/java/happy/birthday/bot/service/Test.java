package happy.birthday.bot.service;

public class Test {
    public static void main(String[] args) {
        String messageText = "set_s BTC/USDT 45000 msg";
        String[] params = messageText.split("\\s+");
        for(String p : params) {
            System.out.println(p);
        }
    }
}
