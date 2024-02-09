package happy.birthday.bot.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

@Component
@Slf4j
@Getter
public class SharedData {
    private final Lock userSignalsLock;
    private final Lock extractedObjectsLock;


    //  private List<JsonObject> extractedObjects = Collections.synchronizedList(new ArrayList<>());
    private List<JsonObject> extractedObjects = new ArrayList<>();
    private final List<Signal> userSignals = new ArrayList<>();

    public SharedData(Lock userSignalsLock, Lock extractedObjectsLock) {
        this.userSignalsLock = userSignalsLock;
        this.extractedObjectsLock = extractedObjectsLock;
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

    public List<JsonObject> getExtractedObjectsCopyWithLock() {
        try {
            extractedObjectsLock.lock();
            return new ArrayList<>(extractedObjects);
        } finally {
            userSignalsLock.unlock();
        }
    }

    public void setExtractedObjectsWithLock(List<JsonObject> jsonObjectListFromApi) {
        try {
            extractedObjectsLock.lock();
            extractedObjects = jsonObjectListFromApi;
        } finally {
            userSignalsLock.unlock();
        }
    }
}
