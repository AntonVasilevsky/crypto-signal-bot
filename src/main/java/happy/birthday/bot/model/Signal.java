package happy.birthday.bot.model;

import java.util.Objects;

public record Signal(
        String ticker,
        Double price,
        Double current,
        String message,
        long user,
        long id,
        boolean isLong

) {
    public Signal(String ticker, Double price, Double current, String message, long user, long id) {
        this(ticker, price, current, message, user, id, price > current);
    }




    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Signal signal)) return false;
        return user == signal.user && Objects.equals(ticker, signal.ticker) && Objects.equals(price, signal.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ticker, price, user);
    }
}
