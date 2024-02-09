package happy.birthday.bot.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
@Getter
public class SharedData {
    private final Lock userSignalsLock;

    //  private List<JsonObject> extractedObjects = Collections.synchronizedList(new ArrayList<>());
    private List<JsonObject> extractedObjects = Collections.synchronizedList(new ArrayList<>());
    private final List<Signal> userSignals = new ArrayList<>();

    public SharedData(Lock userSignalsLock) {
        this.userSignalsLock = userSignalsLock;
    }

    public void addUserSignalsWithLock(Signal signal) {
        try {
            userSignalsLock.lock();
            userSignals.add(signal);
        } finally {
            userSignalsLock.unlock();
        }

    }
    public int getUserSignalsSizeWithLock() {
        try {
            userSignalsLock.lock();
            return userSignals.size();
        } finally {
            userSignalsLock.unlock();
        }
    }
    public List<Signal> getUserSignalsCopyWithLock() {
        try {
            userSignalsLock.lock();
            return new ArrayList<>(userSignals);
        } finally {
            userSignalsLock.unlock();
        }
    }
    public Signal getUserSignalsIndexWithLock(int index) {
        try {
            userSignalsLock.lock();
            return userSignals.get(index);
        } finally {
            userSignalsLock.unlock();
        }
    }

    public boolean removeFromUserSignalsWithLock(Signal matchedSignal) {
        boolean success;
        try {
            userSignalsLock.lock();
            success = userSignals.remove(matchedSignal);
        } finally {
            userSignalsLock.unlock();
        }
        return success;
    }
    public Signal removeFromUserSignalsWithLock(int index) {
        Signal signal;
        try {
            userSignalsLock.lock();
            signal = userSignals.remove(index);
        } finally {
            userSignalsLock.unlock();
        }
        return signal;
    }

    public void cancelAllOrders() {
        try {
            userSignalsLock.lock();
            userSignals.clear();
        } finally {
            userSignalsLock.unlock();
        }
    }

}
